"""
뇌과학 논문 에이전트 로컬 실행 스크립트

실행 전 준비:
1. Qdrant 서버가 켜져 있어야 함 (docker run ... qdrant/qdrant)
2. 환경변수 GEMINI_API_KEY 가 설정돼 있어야 함
   $env:GEMINI_API_KEY = "발급받은키"

실행:
    python3 neuro_paper_rag_main_forPython.py
"""

import os
import sys
from pathlib import Path
from neuro_paper_rag.neuro_paper_workflow_state import create_initial_state

# Windows 콘솔 기본 인코딩(cp949)이 특수문자를 못 담아 죽는 문제 방지
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
sys.stderr.reconfigure(encoding="utf-8", errors="replace")

# API 키 확인
if os.environ.get("GEMINI_API_KEY"):
    print("GEMINI_API_KEY가 확인되었습니다.")
else:
    print("GEMINI_API_KEY가 설정되지 않았습니다. LLM 호출은 실패하고 fallback으로 동작합니다.")

from shared.settings import config
from shared.llm_client import build_llm_client
from neuro_paper_rag.services.ingest_service import IngestAgent
from neuro_paper_rag.adapters.vector_store_adapter import NeuroPaperVectorStore
from neuro_paper_rag.neuro_paper_workflow import build_rag_pipeline


# ── STEP 2. 이번 실행에서 쓸 설정값을 조정 ──
config.scoring.top_k_retrieve = 10
config.scoring.top_n_recommend = 3

print("ArXiv 카테고리:", config.arxiv.categories)
print("카테고리당 최대 수집 개수:", config.arxiv.max_results_per_category)
print("최소 연도:", config.arxiv.min_year)


# 벡터 저장소, LLM, RAG
vector_store = NeuroPaperVectorStore()
llm = build_llm_client()
rag_app = build_rag_pipeline(vector_store, llm)

print("\nLLM provider:", config.llm.provider)
print("LLM model:", config.llm.model_name)
print("Embedding model:", config.embedding.model_name)
print("Qdrant 서버 모드:", config.vector_db.use_server)
if config.vector_db.use_server:
    print("Qdrant 접속 주소:", f"{config.vector_db.host}:{config.vector_db.port}")
else:
    print("Qdrant 파일 경로:", config.neuro_paper_rag_storage.vector_store_path)
print("산출물 저장 경로:", config.neuro_paper_rag_storage.artifact_dir)
print("컬렉션 정보:", vector_store.get_collection_info())


# 논문 수집
def rebuild_collection(vs: NeuroPaperVectorStore) -> None:
    """컬렉션을 삭제 후 재생성합니다."""
    existing = [c.name for c in vs._client.get_collections().collections]
    if vs.collection_name in existing:
        vs._client.delete_collection(vs.collection_name)
    vs._ensure_collection()


def run_indexing(mode: str = "skip") -> None:
    if mode not in {"skip", "append", "rebuild"}:
        raise ValueError("mode는 skip, append, rebuild 중 하나여야 합니다.")

    if mode == "rebuild":
        rebuild_collection(vector_store)
        print("컬렉션을 삭제하고 다시 생성했습니다.")

    before = vector_store.get_collection_info()
    print("수집 전 상태:", before)

    if mode == "skip" and before["points_count"] > 0:
        print("이미 저장된 논문이 있어 수집을 건너뜁니다.")
    else:
        ingest_agent = IngestAgent(vector_store)
        count = ingest_agent.ingest_from_arxiv_api()
        print("이번에 새로 수집한 논문 수:", count)

    after = vector_store.get_collection_info()
    print("수집 후 상태:", after)

    print("\n저장된 논문 목록 (최대 10개):")
    for index, paper in enumerate(vector_store.list_papers(limit=10), 1):
        title = paper.get("title", "제목 없음")
        year = paper.get("published_year", "연도 없음")
        print(f"{index}. {title} ({year})")

    artifact_dir = Path(config.neuro_paper_rag_storage.artifact_dir)
    artifact_dir.mkdir(parents=True, exist_ok=True)
    print("\n산출물 경로:", artifact_dir)
    for artifact_path in sorted(artifact_dir.iterdir()):
        print("-", artifact_path.name)


run_indexing(mode="skip")


# 실행 (검색 + 추천)
def run_neuro_rag(user_query: str, plan_context: str = ""):
    """사용자 질문과 계획 문맥을 넣어 전체 RAG 파이프라인을 실행합니다."""
    result = rag_app.invoke(create_initial_state(user_query, plan_context))
    print(result.get("final_output", "결과가 없습니다."))
    return result


result = run_neuro_rag(
    user_query="집중력이 낮고 공부 중 자꾸 산만해집니다. 오늘 공부 계획에 바로 적용할 수 있는 뇌과학 기반 방법을 알려주세요.",
    plan_context="오늘 플랜: 오전 수학 공부 2시간, 오후 영어 독해 1시간",
)


# 결과 검증
def validate_result(result: dict) -> None:
    """실행 결과가 정상 범위인지 항목별로 확인합니다."""
    checks = {
        "ingest_status가 success 또는 skip인가": result.get("ingest_status") in {"success", "skip"},
        "retrieved_count가 1 이상인가": result.get("retrieved_count", 0) > 0,
        "recommended_papers가 비어있지 않은가": len(result.get("recommended_papers", [])) > 0,
        "final_output이 생성되었는가": bool(result.get("final_output")),
    }

    for message, ok in checks.items():
        status = "PASS" if ok else "FAIL"
        print(f"[{status}] {message}")

    if result.get("recommended_papers"):
        print("\n추천된 논문 목록:")
        for index, paper in enumerate(result["recommended_papers"], 1):
            print(f"{index}. {paper.get('title', '제목 없음')}")


validate_result(result)