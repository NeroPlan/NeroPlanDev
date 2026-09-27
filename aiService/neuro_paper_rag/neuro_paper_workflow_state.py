"""뇌과학 논문 파이프라인 상태 스키마"""

import operator
from typing import Annotated, Any, Dict, List, Optional, TypedDict


class PaperRAGState(TypedDict):
    """수집, 검색, 추천 단계가 사용하는 상태 딕셔너리 구조"""

    user_query: str
    user_plan_context: str
    user_plan_keywords: List[str]
    ingested_paper_count: int
    ingest_status: str
    retrieved_papers: List[Dict[str, Any]]
    retrieved_count: int
    recommended_papers: List[Dict[str, Any]]
    # 4개 SectionAgent가 병렬로 각자 채우는 임시 결과 (fan-out 단계)
    section_problem: str
    section_time_recommendation: str
    section_time_slot: str
    section_method: str
    section_unusual_method: str
    recommendation_rationale: str
    recommendation_sections: Optional[Dict[str, Any]]
    errors: Annotated[List[str], operator.add]
    final_output: Optional[str]


def create_initial_state(user_query: str, plan_context: str = "") -> PaperRAGState:
    """파이프라인 실행 전 필요한 초기 state
    """
    return {
        "user_query": user_query,
        "user_plan_context": plan_context,
        "user_plan_keywords": [],
        "ingested_paper_count": 0,
        "ingest_status": "",
        "retrieved_papers": [],
        "retrieved_count": 0,
        "recommended_papers": [],
        "section_problem": "",
        "section_time_recommendation": "",
        "section_time_slot": "",
        "section_method": "",
        "section_unusual_method": "",
        "recommendation_rationale": "",
        "recommendation_sections": {},
        "errors": [],
        "final_output": None,
    }