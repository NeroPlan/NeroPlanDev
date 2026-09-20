"""논문 추천과 설명 생성을 담당하는 recommend 서비스

추천 단계는 3개 노드로 나뉨 (LangGraph fan-out/fan-in 패턴):

    RankAgent  -->  SectionAgent(problem)             -->  MergeAgent
               -->  SectionAgent(time_recommendation)  -->
               -->  SectionAgent(method)                -->
               -->  SectionAgent(unusual_method)         -->

RankAgent가 후보 논문을 재정렬한 뒤, 4개의 SectionAgent가 병렬로 각자의 섹션만
LLM에 물어봐서 채우고, MergeAgent가 이를 하나의 Markdown으로 합침

이렇게 나눈 이유: 나중에 사용자가 입력 중 일부(예: 시간대 관련 정보)만 바꿔서
다시 보내면, 바뀐 입력에 대응하는 SectionAgent 하나만 재호출하고 나머지 3개
섹션은 이전 결과를 그대로 재사용할 수 있도록 하기 위함
"""

import json
import logging
from typing import Any, Dict, List

from neuro_paper_rag.adapters.artifact_store_adapter import (
    save_json_artifact,
    save_text_artifact,
)
from neuro_paper_rag.adapters.vector_store_adapter import NeuroPaperVectorStore
from neuro_paper_rag.domain.ranking_paper import rank_papers
from neuro_paper_rag.neuro_paper_workflow_state import PaperRAGState
from shared.llm_client import (
    BaseLLMClient,
    get_llm_model_name,
    get_llm_provider_name,
)
from shared.settings import config

logger = logging.getLogger(__name__)

# 정의
SECTION_DEFINITIONS: Dict[str, Dict[str, str]] = {
    "problem": {
        "title": "지금 겪고 있는 문제",
        "instruction": "사용자의 현재 상황과 지금 풀어야 할 과제를 1~2문장으로 정리하세요.",
    },
    "time_recommendation": {
        "title": "언제 하면 좋을까",
        "instruction": (
            "아침/오전, 점심/오후, 저녁, 밤 중 이 과제를 수행하기 가장 적합한 시간대와, "
            "근거가 된 논문(짧은 제목)을 자연스럽게 엮어서 설명하세요."
        ),
    },
    "method": {
        "title": "어떻게 하면 효과적일까",
        "instruction": (
            "논문에서 제시된 해결 방법이나 효과적인 수행 순서를 근거와 함께 설명하세요. "
            "가능하면 단계별로(먼저 ~, 그다음 ~) 풀어서 설명하세요."
        ),
    },
    "unusual_method": {
        "title": "오늘 시도해볼 만한 낯선 방법",
        "instruction": (
            "흔히 쓰이지 않지만 논문에서 근거를 찾을 수 있는 의외의 방법론을 하나 제시하고, "
            "왜 효과가 있다고 여겨지는지 비전공자도 이해할 수 있는 말로, 그러나 전문적으로 "
            "정확하게 설명하세요. 유치하게 풀어쓰지 말고, 낯설어서 오히려 시도해보고 싶어지도록 "
            "흥미를 자극하는 톤으로 마무리하세요."
        ),
    },
}

# 모든 섹션이 공통으로 지키는 안전 및 톤 지침
SHARED_SAFETY_PROMPT = """당신은 뇌과학 논문을 근거로 사용자의 하루를 설계해주는 전문가입니다.
반드시 조심스럽게만 답변하세요.
과장하거나 단정하지 말고, 논문 초록에서 직접 뒷받침되는 범위 안에서만 설명하세요.
불확실한 부분은 "추정" 또는 "해석"이라고 명시하세요.
논문 제목을 본문에서 언급할 때는 전체 제목을 그대로 쓰지 말고 짧게 줄여서 부르세요."""


def _build_papers_context(recommended_papers: List[Dict[str, Any]]) -> str:
    """추천 논문들을 LLM 입력용 짧은 요약 묶음으로 변환합니다 (섹션 공통으로 재사용)."""
    summary_chars = config.recommendation.summary_chars_per_paper
    sections = []
    for index, paper in enumerate(recommended_papers, 1):
        sections.append(
            "\n".join(
                [
                    f"[논문 {index}]",
                    f"제목: {paper['title']}",
                    f"출판 연도: {paper.get('published_year', 'N/A')}",
                    f"관련도 점수: {paper.get('final_score', 0):.3f}",
                    f"저자: {paper.get('authors', 'N/A')}",
                    (
                        "카테고리: "
                        f"{', '.join(paper.get('categories', [])) or paper.get('primary_category', 'N/A')}"
                    ),
                    f"초록 요약: {paper.get('summary', '')[:summary_chars]}",
                ]
            )
        )
    return "\n\n".join(sections)


class RankAgent:
    """검색된 논문 후보를 복합 점수 + MMR로 재정렬합니다 (fan-out 이전 단계)."""

    def __init__(self, vector_store: NeuroPaperVectorStore):
        self.vector_store = vector_store

    def __call__(self, state: PaperRAGState) -> PaperRAGState:
        retrieved_papers = state.get("retrieved_papers", [])
        plan_keywords = state.get("user_plan_keywords", [])

        # 검색 결과가 없으면 이후 섹션 노드로 넘어가지 않도록 빈 결과 반환
        if not retrieved_papers:
            return {
                **state,
                "recommended_papers": [],
                "errors": ["RankAgent: 검색된 논문이 없어 추천을 건너뜁니다."],
            }

        try:
            recommended = rank_papers(
                papers=retrieved_papers,
                user_plan_keywords=plan_keywords,
                top_n=config.scoring.top_n_recommend,
            )

            return {**state, "recommended_papers": recommended}
        except Exception as exc:
            logger.error("RankAgent failed: %s", exc)
            return {
                **state,
                "recommended_papers": [],
                "errors": [f"RankAgent error: {str(exc)}"],
            }


class SectionAgent:
    """추천 논문을 근거로 4개 섹션 중 하나만 담당해서 채우는 병렬 노드.

    section_key에 따라 SECTION_DEFINITIONS에서 지침을 가져와 LLM을 호출하고,
    결과를 state["section_{section_key}"]에 저장합니다. 각 SectionAgent는
    서로 다른 state 키에만 쓰기 때문에 LangGraph에서 병렬 실행이 나음
    """

    def __init__(self, section_key: str, vector_store: NeuroPaperVectorStore, llm: BaseLLMClient):
        if section_key not in SECTION_DEFINITIONS:
            raise ValueError(f"알 수 없는 섹션 키: {section_key}")
        self.section_key = section_key
        self.definition = SECTION_DEFINITIONS[section_key]
        self.vector_store = vector_store
        self.llm = llm

    def __call__(self, state: PaperRAGState) -> PaperRAGState:   # ← __init__과 같은 들여쓰기(4칸)로 이동
        state_key = f"section_{self.section_key}"
        recommended_papers = state.get("recommended_papers", [])
        user_query = state.get("user_query", "")
        plan_context = state.get("user_plan_context", "")

        if not recommended_papers:
            return {state_key: ""}

        try:
            if self.section_key == "time_recommendation":
                papers_text = _build_papers_context(recommended_papers)
                time_slot, reason = self._generate_time_recommendation_section(
                    user_query, papers_text, plan_context
                )
                return {state_key: reason, "section_time_slot": time_slot}

            content = self._generate_section(user_query, recommended_papers, plan_context)
            return {state_key: content}
        except Exception as exc:
            logger.warning("SectionAgent(%s) failed: %s", self.section_key, exc)
            return {
                state_key: "",
                "errors": [f"SectionAgent({self.section_key}) error: {str(exc)}"],
            }
        
    # time_recommendation 시간대 목록
    TIME_SLOTS = ("아침", "점심", "저녁", "밤")

    def _generate_section(
        self,
        user_query: str,
        recommended_papers: List[Dict[str, Any]],
        plan_context: str,
    ) -> str:
        papers_text = _build_papers_context(recommended_papers)
        system_prompt = f"""{SHARED_SAFETY_PROMPT}

지금 담당하는 섹션: "{self.definition['title']}"
이 섹션에서 할 일: {self.definition['instruction']}

반드시 아래 형식의 순수 JSON 객체 하나만 응답하세요. 다른 설명, 마크다운, 코드블록 없이:
{{"{self.section_key}": "완결된 문단(3~5문장)"}}"""

        prompt = f"""사용자 질문: {user_query}
현재 플랜: {plan_context[:300] if plan_context else '없음'}
논문 수: {len(recommended_papers)}

추천된 뇌과학 논문 후보:
{papers_text}"""

        parsed = self._call_llm_json(prompt, system_prompt)
        return str(parsed.get(self.section_key, ""))

    def _generate_time_recommendation_section(
        self,
        user_query: str,
        papers_text: str,
        plan_context: str,
    ) -> tuple[str, str]:
        """아침/점심/저녁/밤 중 하나를 명시적으로 고르고, 그 선택을
        {"time_slot": "<시간대>", "reason": "<이유>"} 형태의 JSON으로 반환합니다.
        """
        slots_text = "/".join(self.TIME_SLOTS)
        system_prompt = f"""{SHARED_SAFETY_PROMPT}

지금 담당하는 섹션: "{self.definition['title']}"
이 섹션에서 할 일: {self.definition['instruction']}

반드시 {slots_text} 중 정확히 하나만 골라야 합니다 (두 개 이상 고르거나 애매하게 답하지 마세요).

반드시 아래 형식의 순수 JSON 객체 하나만 응답하세요. 다른 설명, 마크다운, 코드블록 없이:
{{"time_slot": "{slots_text} 중 하나", "reason": "왜 그 시간대인지 근거 논문과 함께 3~5문장으로 설명"}}"""

        prompt = f"""사용자 질문: {user_query}
현재 플랜: {plan_context[:300] if plan_context else '없음'}

추천된 뇌과학 논문 후보:
{papers_text}"""

        parsed = self._call_llm_json(prompt, system_prompt)
        time_slot = str(parsed.get("time_slot", "")).strip()
        reason = str(parsed.get("reason", ""))
        if time_slot not in self.TIME_SLOTS:
            logger.warning("time_recommendation: 알 수 없는 time_slot '%s', 원문 유지", time_slot)
            time_slot = time_slot or "미정"
        return time_slot, reason

    def _call_llm_json(self, prompt: str, system_prompt: str) -> Dict[str, Any]:
        """LLM을 호출해 JSON을 파싱하고, 파싱이 끊기면 한 번만 재시도합니다."""
        raw = self.llm.generate(prompt, system_prompt, response_json=True)
        try:
            return json.loads(self._clean_json_response(raw), strict=False)
        except json.JSONDecodeError as exc:
            logger.warning(
                "SectionAgent(%s) JSON 파싱 실패, 1회 재시도: %s", self.section_key, exc
            )
            raw_retry = self.llm.generate(prompt, system_prompt, response_json=True)
            try:
                return json.loads(self._clean_json_response(raw_retry), strict=False)
            except json.JSONDecodeError:
                logger.error(
                    "SectionAgent(%s) 재시도도 실패, 빈 섹션으로 처리", self.section_key
                )
                return {}

    def _clean_json_response(self, raw: str) -> str:
        cleaned = raw.strip()
        if cleaned.startswith("```"):
            cleaned = cleaned.strip("`")
            if cleaned.lower().startswith("json"):
                cleaned = cleaned[4:]
            cleaned = cleaned.strip()
        return cleaned

class MergeAgent:
    """4개 섹션과 논문 메타데이터를 합쳐 최종 Markdown 결과를 만듭니다 (fan-in)."""

    def __init__(self, llm: BaseLLMClient):
        # 산출물 저장 시 provider/model 이름 기록용으로만 사용
        self.llm = llm

    def __call__(self, state: PaperRAGState) -> PaperRAGState:
        recommended_papers = state.get("recommended_papers", [])
        user_query = state.get("user_query", "")
        plan_context = state.get("user_plan_context", "")
        if not recommended_papers:
            return {
                **state,
                "recommendation_sections": {},
                "recommendation_rationale": "검색된 논문이 없습니다.",
                "final_output": "관련 논문을 찾지 못했습니다.",
            }
        # 4개 SectionAgent가 각자 채워둔 값을 state에서 그대로 수집
        # time_recommendation만 {time_slot, reason} dict로 구조화
        rationale_sections = {
            key: (
                {
                    "time_slot": state.get("section_time_slot", ""),
                    "reason": state.get(f"section_{key}", ""),
                }
                if key == "time_recommendation"
                else state.get(f"section_{key}", "")
            )
            for key in SECTION_DEFINITIONS
        }
        final_output = self._format_recommendation_output(recommended_papers, rationale_sections)
        save_json_artifact(
            "last_recommendation.json",
            {
                "user_query": user_query,
                "plan_context": plan_context,
                "llm_provider": get_llm_provider_name(self.llm),
                "llm_model_name": get_llm_model_name(self.llm),
                "expected_llm_calls": len(SECTION_DEFINITIONS),
                "recommended_count": len(recommended_papers),
                "recommended_papers": recommended_papers,
                "recommendation_sections": rationale_sections,
                "final_output": final_output,
            },
        )
        save_text_artifact("last_recommendation.md", final_output)
        return {
            **state,
            "recommendation_rationale": json.dumps(rationale_sections, ensure_ascii=False),
            "recommendation_sections": rationale_sections,
            "final_output": final_output,
        }
        

    def _format_recommendation_output(
        self,
        recommended_papers: List[Dict[str, Any]],
        rationale_sections: Dict[str, str],
    ) -> str:
        output_lines = ["## 뇌과학 논문 추천 결과\n"]
        for index, paper in enumerate(recommended_papers, 1):
            output_lines.append(
                f"### {index}. {paper['title']}\n"
                f"- 출판 연도: {paper.get('published_year', 'N/A')}\n"
                f"- 관련도 점수: {paper.get('final_score', 0):.3f}\n"
                f"- 저자: {paper.get('authors', 'N/A')}\n"
                f"- PDF: {paper.get('pdf_url', 'N/A')}\n"
                f"- 초록 미리보기: {paper.get('summary', '')[:220]}...\n"
            )

        # 섹션 제목은 코드에서 고정 - LLM 출력 문구와 무관하게 항상 명확히 분리됨
        for index, (key, definition) in enumerate(SECTION_DEFINITIONS.items(), 1):
            output_lines.append(f"\n## {index}. {definition['title']}")
            content = rationale_sections.get(key, "")
            if key == "time_recommendation" and isinstance(content, dict):
                output_lines.append(content.get("time_slot", ""))
                output_lines.append(f"\n{content.get('reason', '')}")
                continue
            output_lines.append(content)

        return "\n".join(output_lines)