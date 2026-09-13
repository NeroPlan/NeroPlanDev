"""프로젝트 전역 설정 + 뇌과학 파이프라인 기본값을 정의"""

import os
from dataclasses import dataclass, field
from pathlib import Path
from typing import List

PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DATA_PATH = PROJECT_ROOT / "data"
DEFAULT_NEURO_PAPER_RAG_PATH = DEFAULT_DATA_PATH / "neuro_paper_rag"
DEFAULT_NEURO_PAPER_RAG_QDRANT_PATH = DEFAULT_NEURO_PAPER_RAG_PATH / "qdrant_store"
DEFAULT_NEURO_PAPER_RAG_ARTIFACT_PATH = DEFAULT_NEURO_PAPER_RAG_PATH / "artifacts"
DEFAULT_MODEL_SAVE_PATH = DEFAULT_DATA_PATH / "models"


@dataclass
class VectorDBConfig:
    """Qdrant 벡터 저장소 관련 설정입니다."""

    # use_server=True 이면 host:port로 Qdrant 서버(도커)에 접속함
    # 지금은 서버 모드를 기본으로 사용 (환경변수 QDRANT_USE_SERVER=0 이면 파일모드로 되돌릴 수 있음)
    use_server: bool = field(
        default_factory=lambda: os.getenv("QDRANT_USE_SERVER", "1") == "1"
    )
    host: str = field(default_factory=lambda: os.getenv("QDRANT_HOST", "localhost"))
    port: int = field(default_factory=lambda: int(os.getenv("QDRANT_PORT", "6333")))
    path: str = ":memory:"
    collection_name: str = "neuroscience_papers"
    vector_size: int = 768


@dataclass
class NeuroPaperRAGStorageConfig:
    # 저장 위치 설정
    persist_vector_store: bool = True
    vector_store_path: str = field(default_factory=lambda: str(DEFAULT_NEURO_PAPER_RAG_QDRANT_PATH))
    artifact_dir: str = field(default_factory=lambda: str(DEFAULT_NEURO_PAPER_RAG_ARTIFACT_PATH))


@dataclass
class EmbeddingConfig:
    # 임베딩 모델 설정
    model_name: str = "sentence-transformers/all-mpnet-base-v2"
    batch_size: int = 32
    max_length: int = 512
    device: str = "auto"


@dataclass
class LLMConfig:
    # 질의 확장 + 추천 이유 생성에 사용하는 LLM 설정
    provider: str = field(
        default_factory=lambda: os.getenv("LLM_PROVIDER", "gemini")
    )  # "gemini" | "openai" | "local_hf"
    
    model_name: str = field(
        default_factory=lambda: os.getenv("GEMINI_MODEL_NAME", "gemini-3.5-flash")
    )
    temperature: float = 0.2
    max_tokens: int = 2400
    api_key_env: str = "GEMINI_API_KEY"


@dataclass
class ArxivFetchConfig:
    # ArXiv 조회 설정

    categories: List[str] = field(default_factory=lambda: ["q-bio.NC"])
    max_results_per_category: int = 300
    min_year: int = 2020
    request_delay_sec: float = 3.0


@dataclass
class ScoringConfig:
    """논문 후보를 재정렬할 때 사용하는 점수화 설정입니다.

    최종 점수 = semantic_similarity_weight × semantic_sim
              + recency_weight × recency
              + relevance_to_plan_weight × plan_relevance
    (세 가중치의 합은 1.0)
    """

    # 질문과 논문의 "의미"가 벡터 공간에서 얼마나 가까운지(코사인 유사도).
    # SearchAgent가 Qdrant에서 검색할 때 나온 score 값을 그대로 사용.
    # (질문 원문 + LLM이 확장한 영어 학술 쿼리로 검색한 결과)
    semantic_similarity_weight: float = 0.55

    # 논문이 얼마나 최근에 출판됐는지 (published_year 기준 지수 감쇠).
    # 나이(age)가 0에 가까울수록 1.0에 가까움.
    # 참고: 지금 코퍼스는 검색된 후보 대부분이 2026년 논문이라
    # 이 값이 거의 항상 1.0으로 동일하게 나와 실질적인 변별력이 낮음.
    recency_weight: float = 0.15

    # 사용자의 플랜/질문에서 뽑은 키워드가 논문 제목+초록에 실제로
    # 몇 개나 등장하는지(단어 단위 문자열 매칭 비율).
    # semantic_similarity와 달리 "의미"가 아니라 "정확한 단어 일치"를 봄.
    relevance_to_plan_weight: float = 0.30

    # MMR(Maximal Marginal Relevance)에서 관련성과 다양성 사이의 균형 계수.
    # 1.0에 가까울수록 관련성(점수)만 보고, 0.0에 가까울수록 다양성(중복 억제)을 우선함.
    mmr_lambda: float = 0.60

    # SearchAgent가 Qdrant에서 1차로 가져오는 후보 논문 개수.
    # 이 중에서 RankAgent + MMR이 최종 top_n_recommend개를 추려냄.
    top_k_retrieve: int = 20

    # MergeAgent에게 최종적으로 넘길, 사용자에게 보여줄 추천 논문 개수.
    top_n_recommend: int = 3


@dataclass
class RecommendationConfig:
    # 추천 설명 길이와 출력 형식 조절
    summary_chars_per_paper: int = 220
    actions_per_paper: int = 3


# 수정 필요
@dataclass
class PredictionModelConfig:
    user_model_type: str = "gradient_boosting"
    user_model_threshold: float = 0.5
    agent_model_type: str = "random_forest"
    agent_model_threshold: float = 0.5
    min_history_days: int = 7
    retrain_interval_days: int = 14
    feature_window_days: int = 30


# 수정 필요
@dataclass
class HRNConfig:
    bias_threshold: float = 0.30
    urgency_decay_hours: float = 24.0
    recency_boost: float = 1.5


@dataclass
class AppConfig:
    # 공통 설정
    vector_db: VectorDBConfig = field(default_factory=VectorDBConfig)
    neuro_paper_rag_storage: NeuroPaperRAGStorageConfig = field(
        default_factory=NeuroPaperRAGStorageConfig
    )
    embedding: EmbeddingConfig = field(default_factory=EmbeddingConfig)
    llm: LLMConfig = field(default_factory=LLMConfig)
    arxiv: ArxivFetchConfig = field(default_factory=ArxivFetchConfig)
    scoring: ScoringConfig = field(default_factory=ScoringConfig)
    recommendation: RecommendationConfig = field(default_factory=RecommendationConfig)
    prediction: PredictionModelConfig = field(default_factory=PredictionModelConfig)
    hrn: HRNConfig = field(default_factory=HRNConfig)
    data_path: str = field(default_factory=lambda: str(DEFAULT_DATA_PATH))
    model_save_path: str = field(default_factory=lambda: str(DEFAULT_MODEL_SAVE_PATH))
    log_level: str = "INFO"


config = AppConfig()