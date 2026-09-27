# 프론트엔드/Spring 백엔드가 호출할 뇌과학 RAG 파이프라인 HTTP API 서버 - 임시

from fastapi import FastAPI
from pydantic import BaseModel

from neuro_paper_rag.adapters.vector_store_adapter import NeuroPaperVectorStore
from neuro_paper_rag.neuro_paper_workflow import build_rag_pipeline
from neuro_paper_rag.neuro_paper_workflow_state import create_initial_state
from shared.llm_client import build_llm_client

app = FastAPI(title="NeuroPlan RAG API")

# 서버 시작 시 한 번만 초기화
_vector_store = NeuroPaperVectorStore()
_llm = build_llm_client()
_rag_app = build_rag_pipeline(_vector_store, _llm)


class RecommendRequest(BaseModel):
    query: str

# endpoint 수정 필요
@app.post("/recommend")
def recommend(req: RecommendRequest):
    result = _rag_app.invoke(create_initial_state(req.query))
    
    return {
        "papers": result.get("recommended_papers", []),
        "sections": result.get("recommendation_sections", {}),
        "errors": result.get("errors", []),
    }