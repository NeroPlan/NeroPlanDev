import hashlib
import logging
import uuid
from pathlib import Path
from typing import Any, Dict, List, Optional

import numpy as np
from qdrant_client import QdrantClient
from qdrant_client.models import (
    Distance,
    FieldCondition,
    Filter,
    MatchValue,
    PointStruct,
    Range,
    ScoredPoint,
    SearchParams,
    VectorParams,
)
from sentence_transformers import SentenceTransformer

from shared.settings import config

logger = logging.getLogger(__name__)


class NeuroPaperVectorStore:
    def __init__(
        self,
        collection_name: str = None,
        embedding_model: SentenceTransformer = None,
        path: str = None,
        load_embedding_model: bool = True,
    ):
        self.collection_name = collection_name or config.vector_db.collection_name

        if path is not None:
            self.path = path
        elif config.neuro_paper_rag_storage.persist_vector_store:
            self.path = config.neuro_paper_rag_storage.vector_store_path
        else:
            self.path = config.vector_db.path

        self.vector_size = config.vector_db.vector_size

        # 서버 모드: 도커로 띄운 Qdrant 서버(host:port)에 접속
        # 팀원 전체가 같은 서버를 보게 되고, http://host:6333/dashboard 로 GUI 확인
        if config.vector_db.use_server:
            self._client = QdrantClient(
                host=config.vector_db.host,
                port=config.vector_db.port,
            )
            logger.info(
                "Qdrant connected to server: %s:%s",
                config.vector_db.host,
                config.vector_db.port,
            )
        # 파일 모드: 로컬 sqlite 파일 하나에 저장 - 기본 방식
        elif self.path == ":memory:":
            self._client = QdrantClient(":memory:")
            logger.info("Qdrant initialized in memory mode")
        else:
            Path(self.path).mkdir(parents=True, exist_ok=True)
            self._client = QdrantClient(path=self.path)
            logger.info("Qdrant initialized in persistent mode: %s", self.path)

        if embedding_model is not None:
            self._embed_model = embedding_model
        elif load_embedding_model:
            self._embed_model = self._load_embedding_model()
        else:
            self._embed_model = None

        self._ensure_collection()

    def get_collection_info(self) -> Dict[str, Any]:
        info = self._client.get_collection(self.collection_name)
        return {
            "storage_path": self.path,
            "vectors_count": getattr(
                info,
                "vectors_count",
                getattr(info, "indexed_vectors_count", 0),
            ),
            "points_count": getattr(info, "points_count", 0),
            "status": str(info.status),
        }


    def upsert_papers(self, papers: List[Dict[str, Any]]) -> int:
        if not papers:
            return 0

        texts = [f"{paper['title']} {paper.get('summary', '')}" for paper in papers]
        vectors = self.embed_texts(texts)

        points = []
        for paper, vector in zip(papers, vectors):
            point_id = paper.get("id") or self._stable_point_id(paper)
            payload = {
                "source_id": paper.get("source_id", ""),
                "title": paper.get("title", ""),
                "summary": paper.get("summary", ""),
                "published_year": paper.get("published_year", 0),
                "authors": paper.get("authors", ""),
                "domain": paper.get("domain", "general"),
                "pdf_url": paper.get("pdf_url", ""),
                "primary_category": paper.get("primary_category", ""),
                "categories": paper.get("categories", []),
            }
            points.append(PointStruct(id=point_id, vector=vector.tolist(), payload=payload))

        batch_size = 1000
        total_uploaded = 0
        for start in range(0, len(points), batch_size):
            batch = points[start : start + batch_size]
            self._client.upsert(collection_name=self.collection_name, points=batch)
            total_uploaded += len(batch)

        logger.info("Indexed %s papers", total_uploaded)
        return total_uploaded

    def search(
        self,
        query: str,
        top_k: int = None,
        domain_filter: Optional[str] = None,
        min_year: Optional[int] = None,
    ) -> List[Dict[str, Any]]:
        top_k = top_k or config.scoring.top_k_retrieve
        query_vector = self.embed_texts([query])[0]

        must_conditions = self._build_filter_conditions(domain_filter, min_year)
        query_filter = Filter(must=must_conditions) if must_conditions else None
        if hasattr(self._client, "query_points"):
            response = self._client.query_points(
                collection_name=self.collection_name,
                query=query_vector.tolist(),
                limit=top_k,
                query_filter=query_filter,
                with_payload=True,
                search_params=SearchParams(hnsw_ef=128),
            )
            results: List[ScoredPoint] = list(response.points)
        else:
            results = self._client.search(
                collection_name=self.collection_name,
                query_vector=query_vector.tolist(),
                limit=top_k,
                query_filter=query_filter,
                with_payload=True,
                search_params=SearchParams(hnsw_ef=128),
            )

        return [
            {
                "id": str(result.id),
                "score": float(result.score),
                **result.payload,
            }
            for result in results
        ]

    def list_papers(
        self,
        limit: int = 20,
        domain_filter: Optional[str] = None,
        min_year: Optional[int] = None,
    ) -> List[Dict[str, Any]]:
        must_conditions = self._build_filter_conditions(domain_filter, min_year)
        scroll_filter = Filter(must=must_conditions) if must_conditions else None
        records, _ = self._client.scroll(
            collection_name=self.collection_name,
            scroll_filter=scroll_filter,
            limit=limit,
            with_payload=True,
            with_vectors=False,
        )
        return [
            {
                "id": str(record.id),
                **(record.payload or {}),
            }
            for record in records
        ]

    def embed_texts(self, texts: List[str]) -> np.ndarray:
        if self._embed_model is None:
            raise RuntimeError("Embedding model is not loaded.")

        return self._embed_model.encode(
            texts,
            batch_size=config.embedding.batch_size,
            show_progress_bar=len(texts) > 100,
            convert_to_numpy=True,
            normalize_embeddings=True,
        )

    @staticmethod
    def _stable_point_id(paper: Dict[str, Any]) -> str:
        stable_key = (
            paper.get("source_id")
            or paper.get("doi")
            or f"{paper.get('title', '')}|{paper.get('published_year', 0)}"
        )
        digest = hashlib.md5(stable_key.encode("utf-8")).hexdigest()
        return str(uuid.UUID(digest))

    @staticmethod
    def _build_filter_conditions(
        domain_filter: Optional[str],
        min_year: Optional[int],
    ) -> List[FieldCondition]:
        must_conditions: List[FieldCondition] = []
        if domain_filter:
            must_conditions.append(
                FieldCondition(key="domain", match=MatchValue(value=domain_filter))
            )
        if min_year:
            must_conditions.append(
                FieldCondition(key="published_year", range=Range(gte=min_year))
            )
        return must_conditions

    def _load_embedding_model(self) -> SentenceTransformer:
        """설정된 sentence-transformers 모델을 로드합니다."""
        device = "cuda" if self._is_cuda_available() else "cpu"
        model = SentenceTransformer(config.embedding.model_name, device=device)
        logger.info("Embedding model loaded: %s on %s", config.embedding.model_name, device)
        return model

    @staticmethod
    def _is_cuda_available() -> bool:
        try:
            import torch

            return torch.cuda.is_available()
        except ImportError:
            return False

    def _ensure_collection(self):
        existing = [collection.name for collection in self._client.get_collections().collections]
        if self.collection_name not in existing:
            self._client.create_collection(
                collection_name=self.collection_name,
                vectors_config=VectorParams(
                    size=self.vector_size,
                    distance=Distance.COSINE,
                ),
            )
            logger.info("Collection created: %s", self.collection_name)
        else:
            logger.info("Collection reused: %s", self.collection_name)