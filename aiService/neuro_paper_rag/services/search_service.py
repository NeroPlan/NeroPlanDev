"""논문 검색을 담당하는 search 서비스"""

import logging
import re
from typing import Any, Dict, List

from neuro_paper_rag.adapters.artifact_store_adapter import save_json_artifact
from neuro_paper_rag.adapters.vector_store_adapter import NeuroPaperVectorStore
from neuro_paper_rag.neuro_paper_workflow_state import PaperRAGState
from shared.llm_client import (
    BaseLLMClient,
    get_llm_model_name,
    get_llm_provider_name,
)
from shared.settings import config

logger = logging.getLogger(__name__)


class SearchAgent:
    """사용자 상황에 맞는 뇌과학 논문 후보를 검색합니다."""

    SEARCH_SYSTEM_PROMPT = """당신은 뇌과학 전문가입니다.
사용자의 생활/플래닝 상황을 뇌과학 학술 검색 쿼리로 변환합니다.
원본 쿼리에서 관련 뇌과학 개념(신경전달물질, 뇌 영역, 인지 기능 등)을 추출하여
검색에 적합한 영어 쿼리 3개를 줄바꿈으로 구분해서 생성하세요.
형식: 쿼리1\n쿼리2\n쿼리3"""

    # 벡터 저장소 + LLM 초기화
    def __init__(self, vector_store: NeuroPaperVectorStore, llm: BaseLLMClient):
        self.vector_store = vector_store
        self.llm = llm

    # 원본 질의와 확장 질의를 함께 사용하여 후보 논문 검색
    def __call__(self, state: PaperRAGState) -> PaperRAGState:
        user_query = state.get("user_query", "")
        user_plan_context = state.get("user_plan_context", "")

        try:
            # 원본 질문 -> 검색용 학술 쿼리로 확장
            expanded_queries = self._expand_query_with_llm(user_query, user_plan_context)

            # 원본 질문도 함께 사용
            all_queries = [user_query] + expanded_queries

            # 원본 질의와 확장 질의를 함께 사용하여 벡터 저장소에서 후보 논문 검색
            all_results: Dict[str, Dict[str, Any]] = {}
            for query in all_queries:
                results = self.vector_store.search(
                    query=query,
                    top_k=config.scoring.top_k_retrieve,
                    domain_filter="neuroscience",
                    min_year=config.arxiv.min_year,
                )

                # 한 쿼리에서 나온 검색 결과를 다시 보며 질문과의 관련 가능성 수치 조정 및 중복 제거 수행
                for result in results:
                    paper_id = result["id"]
                    if paper_id not in all_results:
                        all_results[paper_id] = result

                    # 다른 쿼리에서 나온 논문이면 중복, 기존 점수와 새 점수 중 더 높은 점수를 골라 1.1의 가중치 주어 업데이트
                    else:
                        all_results[paper_id]["score"] = max(
                            all_results[paper_id]["score"],
                            result["score"],
                        ) * 1.1

            keywords = self._extract_keywords(" ".join(expanded_queries))
            if not keywords:
            # LLM 쿼리 확장이 실패해 expanded_queries가 비어있는 경우
                keywords = self._extract_keywords(user_query)


            retrieved = list(all_results.values())

            # 병합 결과 점수 기준으로 내림차순 정렬
            retrieved.sort(key=lambda item: item["score"], reverse=True)
            retrieved = retrieved[: config.scoring.top_k_retrieve]

            save_json_artifact(
                "last_search_results.json",
                {
                    "user_query": user_query,
                    "plan_context": user_plan_context,
                    "llm_provider": get_llm_provider_name(self.llm),
                    "llm_model_name": get_llm_model_name(self.llm),
                    "expanded_queries": expanded_queries,
                    "retrieved_count": len(retrieved),
                    "retrieved_papers": retrieved,
                },
            )

            # 성공 시 기존 상태에 검색 결과 + 키워드 담아 반환하여 RecommendAgent로 전달
            return {
                **state,
                "retrieved_papers": retrieved,
                "retrieved_count": len(retrieved),
                "user_plan_keywords": keywords,
            }

        except Exception as exc:
            logger.error("SearchAgent failed: %s", exc)
            return {
                **state,
                "retrieved_papers": [],
                "retrieved_count": 0,
                "errors": [f"SearchAgent error: {str(exc)}"],
            }

    # 사용자 질의를 뇌과학 학술 검색용 영어 쿼리 여러 개로 확장
    def _expand_query_with_llm(self, user_query: str, user_plan_context: str) -> List[str]:
        prompt = (
            f"사용자 상황: {user_query} "
            f"현재 플랜: {user_plan_context[:200] if user_plan_context else '없음'} "
            "이 상황과 관련된 뇌과학 논문을 찾기 위한 검색 쿼리를 생성해주세요."
        )

        try:
            response = self.llm.generate(prompt, self.SEARCH_SYSTEM_PROMPT)
            queries = [query.strip() for query in response.strip().split("\n") if query.strip()]
            return queries[:3]

        except Exception as exc:
            logger.warning("Query expansion failed, falling back to original query: %s", exc)
            return [user_query]

    # 사용자 질의와 계획 문맥에서 간단한 키워드를 추출
    def _extract_keywords(self, text: str) -> List[str]:
        stopwords = {"the", "a", "an", "is", "are", "was", "in", "on", "at", "to"}
        words = re.findall(r"[A-Za-z가-힣]{2,}", text)
        return [word for word in words if word.lower() not in stopwords][:20]
