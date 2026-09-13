"""논문 수집과 저장을 담당하는 ingest 서비스"""

import logging
from typing import Dict, List

from neuro_paper_rag.adapters.artifact_store_adapter import save_json_artifact
from neuro_paper_rag.adapters.arxiv_client_adapter import ArxivPaperClient
from neuro_paper_rag.adapters.vector_store_adapter import NeuroPaperVectorStore
from neuro_paper_rag.neuro_paper_workflow_state import PaperRAGState
from shared.settings import config

logger = logging.getLogger(__name__)


"""ArXiv에서 뇌과학 논문을 수집하여 벡터 저장소에 적재하는 에이전트"""
class IngestAgent:

    # 시작 - 초기화
    def __init__(
        self,
        # vector_store는 필수, arxiv_client는 테스트 시 mock 주입 가능하도록 선택적.
        vector_store: NeuroPaperVectorStore,
        arxiv_client: ArxivPaperClient = None,
    ):
        self.vector_store = vector_store
        self.arxiv_client = arxiv_client if arxiv_client is not None else ArxivPaperClient()


    """벡터 저장소 확인 후 비어 있을 때에만 ArXiv 논문 적재 수행"""
    def __call__(self, state: PaperRAGState) -> PaperRAGState:

        # 벡터 저장소 조회 및 몇 개의 논문이 존재하는지 확인
        info = self.vector_store.get_collection_info()
        existing_count = info.get("points_count", 0)

        # 논문이 존재할 경우
        if existing_count > 0:
            logger.info("Vector store already has %s papers, skipping ingestion", existing_count)
            return {
                # 기존 state 유지
                **state,
                "ingested_paper_count": existing_count,
                "ingest_status": "skip",
            }

        # 논문이 존재하지 않을 경우
        try:
            count = self.ingest_from_arxiv_api()

            # 이후 성공 상태를 모아놓은 baseEntity로 수정 예정
            return {
                **state,
                "ingested_paper_count": count,
                "ingest_status": "success",
            }
        except Exception as exc:
            logger.error("Ingestion failed: %s", exc)
            return {
                **state,
                "ingested_paper_count": 0,
                "ingest_status": "error",
                "errors": [f"IngestAgent error: {str(exc)}"],
            }


    """설정된 ArXiv 카테고리에서 최신 논문을 가져와 중복 없이 저장"""
    def ingest_from_arxiv_api(self) -> int:
        logger.info("ArXiv ingestion started: categories=%s", config.arxiv.categories)
        fetched_papers = self.arxiv_client.fetch_recent_papers(
            categories=config.arxiv.categories,
            max_results_per_category=config.arxiv.max_results_per_category,
        )

        # 연도 필터, 도메인 필터, 중복 제거 수행
        papers = self._filter_and_dedupe_papers(fetched_papers)

        # 필터링 된 논문을 벡터 저장소에 업서트
        count = self.vector_store.upsert_papers(papers)

        # 필터링 된 논문 산출물 파일명 = ingested_papers.json {논문 메타데이터}
        save_json_artifact(
            "ingested_papers.json",
            {
                "categories": config.arxiv.categories,
                "min_year": config.arxiv.min_year,
                "count": len(papers),
                "papers": papers,
            },
        )
        logger.info("ArXiv ingestion completed: %s papers", count)

        
        return count



    """연도 필터, 도메인 필터, 중복 제거 수행"""
    def _filter_and_dedupe_papers(self, papers: List[Dict[str, object]]) -> List[Dict[str, object]]:
        deduped_papers: Dict[str, Dict[str, object]] = {}
        for paper in papers:
            if int(paper.get("published_year", 0)) < config.arxiv.min_year:
                continue
            if paper.get("domain") != "neuroscience":
                continue

            dedupe_key = (
                str(paper.get("source_id", ""))
                or f"{paper.get('title', '')}|{paper.get('published_year', 0)}"
            )
            deduped_papers[dedupe_key] = paper

        return list(deduped_papers.values())
