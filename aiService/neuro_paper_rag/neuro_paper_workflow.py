"""뇌과학 논문 파이프라인의 LangGraph 조립부입니다."""

import logging
from typing import Literal

from langgraph.graph import END, StateGraph

from neuro_paper_rag.adapters.vector_store_adapter import NeuroPaperVectorStore
from neuro_paper_rag.neuro_paper_workflow_state import PaperRAGState, create_initial_state
from neuro_paper_rag.services.ingest_service import IngestAgent
from neuro_paper_rag.services.recommend_service import (
    SECTION_DEFINITIONS,
    MergeAgent,
    RankAgent,
    SectionAgent,
)
from neuro_paper_rag.services.search_service import SearchAgent
from shared.llm_client import BaseLLMClient, build_llm_client

logger = logging.getLogger(__name__)


def build_rag_pipeline(
    vector_store: NeuroPaperVectorStore = None,
    llm: BaseLLMClient = None,
):
    """수집, 검색, 랭킹, 4개 섹션 병렬 생성, 합치기 노드를 하나의 LangGraph 파이프라인으로 묶습니다.

    구조: ingest -> search -> rank -> (4개 SectionAgent 병렬) -> merge -> END
    """
    vector_store = vector_store if vector_store is not None else NeuroPaperVectorStore()
    llm = llm if llm is not None else build_llm_client()

    ingest_agent = IngestAgent(vector_store)
    search_agent = SearchAgent(vector_store, llm)
    rank_agent = RankAgent(vector_store)
    section_agents = {
        key: SectionAgent(key, vector_store, llm) for key in SECTION_DEFINITIONS
    }
    merge_agent = MergeAgent(llm)

    graph = StateGraph(PaperRAGState)
    graph.add_node("ingest", ingest_agent)
    graph.add_node("search", search_agent)
    graph.add_node("rank", rank_agent)
    for key, agent in section_agents.items():
        graph.add_node(f"section_{key}", agent)
    graph.add_node("merge", merge_agent)

    def should_continue_after_search(state: PaperRAGState) -> Literal["rank", "end"]:
        """검색 결과가 없으면 랭킹/추천 단계로 가지 않도록 분기합니다."""
        if state.get("retrieved_count", 0) == 0:
            return "end"
        return "rank"

    graph.set_entry_point("ingest")
    graph.add_edge("ingest", "search")
    graph.add_conditional_edges(
        "search",
        should_continue_after_search,
        {"rank": "rank", "end": END},
    )

    # fan-out: rank 완료 후 4개 섹션 노드를 병렬로 실행
    for key in section_agents:
        graph.add_edge("rank", f"section_{key}")

    # fan-in: 4개 섹션이 모두 끝나야 merge 실행 (LangGraph가 자동으로 대기)
    for key in section_agents:
        graph.add_edge(f"section_{key}", "merge")

    graph.add_edge("merge", END)
    return graph.compile()


class NeuroPaperRAGSystem:
    """뇌과학 논문 파이프라인을 단독 실행하기 위한 래퍼입니다."""

    def __init__(
        self,
        vector_store: NeuroPaperVectorStore = None,
        llm: BaseLLMClient = None,
    ):
        """벡터 저장소와 LLM을 초기화하고 실행 가능한 그래프를 준비합니다."""
        self.vector_store = (
            vector_store if vector_store is not None else NeuroPaperVectorStore()
        )
        self.llm = llm if llm is not None else build_llm_client()
        self.rag_pipeline = build_rag_pipeline(self.vector_store, self.llm)
        logger.info("NeuroPaperRAGSystem initialized")

    def run(
        self,
        user_query: str,
    ) -> PaperRAGState:
        """사용자 질의를 받아 전체 파이프라인을 실행합니다."""
        initial_state: PaperRAGState = create_initial_state(user_query)
        return self.rag_pipeline.invoke(initial_state)
