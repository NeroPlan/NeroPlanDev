"""
계획 카테고리 분류 (LLM).

계획 내용과 사용자의 기존 카테고리 목록을 LLM에 함께 넣어,
의미가 같은 기존 카테고리가 있으면 그것을 고르고 없으면 새 카테고리 이름을 만든다.
("운동"과 "헬스"처럼 같은 뜻의 카테고리가 따로 생기는 것을 막기 위해 기존 목록을 같이 줌)

- LLM 호출은 shared/llm_client.py의 클라이언트를 그대로 씀 (기본 Gemini, 환경변수 GEMINI_API_KEY)
- 실패하거나 이상한 값이 오면 category=None과 경고 문장(warning)을 돌려줌
  → backendPlan은 category가 없으면 계획을 미분류(category null)로 저장하고, 나중에 다시 분류를 요청할 수 있음
"""
import json
import logging
import re

logger = logging.getLogger(__name__)

MAX_CATEGORY_LENGTH = 20  # DB 컬럼은 50자지만, 화면 표시와 중복 방지를 위해 짧게 제한
MAX_ATTEMPTS = 2  # 첫 호출 + 재시도 1회

WARNING_LLM_FAILED = "카테고리 분류에 실패했어요. 잠시 후 다시 분류해 주세요."
WARNING_INVALID_OUTPUT = "카테고리를 정하지 못했어요. 잠시 후 다시 분류해 주세요."

SYSTEM_PROMPT = """너는 플래너 앱에서 사용자의 계획을 카테고리로 분류하는 역할을 한다.

규칙:
1. 기존 카테고리 목록 중 계획과 의미가 같은 것이 있으면, 그 이름을 글자 그대로 골라라.
   (예: 기존에 "운동"이 있으면 "헬스장 가기", "러닝"은 모두 "운동")
2. 의미가 맞는 기존 카테고리가 없을 때만 새 카테고리를 만든다.
3. 새 카테고리는 2~6자의 짧은 한국어 명사로, 다른 계획도 담을 수 있게 넓은 범위로 짓는다.
   (예: "토익 문제집 풀기" → "공부", "팀 회의" → "업무")
4. 계획 내용 자체를 카테고리 이름으로 쓰지 마라.
5. 반드시 아래 JSON 형식으로만 답하라.
{"category": "카테고리 이름"}"""


def _build_prompt(content: str, categories: list[str]) -> str:
    existing = ", ".join(f'"{c}"' for c in categories) if categories else "(없음)"
    return f"기존 카테고리 목록: {existing}\n계획 내용: {content}"


def _normalize(name: str) -> str:
    return re.sub(r"\s+", "", name)


def _parse_category(raw: str) -> str | None:
    """LLM 응답 텍스트에서 category 값을 꺼냄. 형식이 틀리면 None."""
    text = raw.strip()
    if text.startswith("```"):  # 코드블록으로 감싸서 오는 경우
        text = text.strip("`").removeprefix("json").strip()
    try:
        data = json.loads(text)
    except json.JSONDecodeError:
        return None
    category = data.get("category") if isinstance(data, dict) else None
    if not isinstance(category, str):
        return None
    category = category.strip().strip('"').strip()
    if not category or len(category) > MAX_CATEGORY_LENGTH:
        return None
    return category


def classify_category(content: str, categories: list[str], llm) -> dict:
    """계획 하나의 카테고리를 정함.

    - content: 계획 내용
    - categories: 사용자가 현재 가진 카테고리 이름 목록 (처음에는 빈 리스트)
    - llm: shared.llm_client의 클라이언트 (generate(prompt, system_prompt, response_json) 제공)

    반환: {"category": str | None, "is_new": bool, "warning": str | None}
    - 성공: category에 이름, 기존 목록에 없던 이름이면 is_new=True, warning=None
    - 실패: category=None, is_new=False, warning에 사용자에게 한 번 보여줄 경고 문장
    """
    categories = [c for c in categories if c and c.strip()]
    by_normalized = {_normalize(c): c for c in categories}

    if not llm.is_available():
        logger.warning("LLM 미연결 상태라 카테고리 분류를 건너뜀 (API 키 확인 필요)")
        return {"category": None, "is_new": False, "warning": WARNING_LLM_FAILED}

    prompt = _build_prompt(content, categories)
    warning = WARNING_LLM_FAILED
    for attempt in range(1, MAX_ATTEMPTS + 1):
        try:
            raw = llm.generate(prompt, system_prompt=SYSTEM_PROMPT, response_json=True)
        except Exception as e:  # 네트워크 오류, 할당량 초과 등
            logger.warning("카테고리 분류 LLM 호출 실패 (%d/%d): %s", attempt, MAX_ATTEMPTS, e)
            warning = WARNING_LLM_FAILED
            continue

        category = _parse_category(raw or "")
        if category is None:
            logger.warning("카테고리 분류 LLM 응답 형식 오류 (%d/%d): %r", attempt, MAX_ATTEMPTS, (raw or "")[:200])
            warning = WARNING_INVALID_OUTPUT
            continue

        # 띄어쓰기만 다른 기존 카테고리가 있으면 기존 이름으로 맞춤 (예: "자기 계발" → "자기계발")
        existing = by_normalized.get(_normalize(category))
        if existing is not None:
            return {"category": existing, "is_new": False, "warning": None}
        return {"category": category, "is_new": True, "warning": None}

    return {"category": None, "is_new": False, "warning": warning}