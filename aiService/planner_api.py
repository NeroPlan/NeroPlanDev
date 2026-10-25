"""
backendPlan이 호출하는 AI 서비스 HTTP API (FastAPI).
backendPlan의 global/ai/AiClient.java, global/ai/dto/*.java와 요청·응답 형식을 맞춤.

- GET  /health                  : 서버·모델·LLM 상태 확인
- POST /api/ai/plans/classify   : 계획 내용 → 카테고리 (LLM)
- POST /api/ai/analyze          : 오늘 계획 위험도 판정 + 재배열 (success_rate_scheduler.schedule_plans)

실행 (aiService/ 폴더에서):
    uvicorn planner_api:app --host 0.0.0.0 --port 8000
backendPlan의 ai.base-url 기본값이 http://aiService:8000 이므로 포트는 8000.
"""
import logging
import os
import sys
from typing import Literal

from fastapi import FastAPI
from pydantic import BaseModel, Field

AI_SERVICE_DIR = os.path.dirname(os.path.abspath(__file__))
# planning_fallacy_detector, success_rate_scheduler 안의 모듈이 서로를 파일명으로 import하므로 경로 추가
sys.path.append(os.path.join(AI_SERVICE_DIR, "planning_fallacy_detector"))
sys.path.append(os.path.join(AI_SERVICE_DIR, "success_rate_scheduler"))

from bias_diagnosis import load_model  # noqa: E402
from plan_classifier.classify_category import classify_category  # noqa: E402
from schedule_plans import schedule_plans  # noqa: E402
from shared.llm_client import build_llm_client  # noqa: E402

logging.basicConfig(level=logging.INFO)

app = FastAPI(title="NeroPlan AI API")

# 서버 시작 시 한 번만 로드
_, _model, _threshold = load_model()
_llm = build_llm_client()


# ---- 요청/응답 형식 ----

class ClassifyRequest(BaseModel):
    content: str = Field(min_length=1)
    categories: list[str] = []  # 사용자가 현재 가진 카테고리 이름 목록


class ClassifyResponse(BaseModel):
    category: str | None  # 분류 실패 시 null → backendPlan은 미분류로 저장
    is_new: bool  # 기존 카테고리에 없어 새로 만든 이름인지
    warning: str | None = None  # 실패 시 사용자에게 한 번 보여줄 경고 문장


class PendingPlan(BaseModel):
    plan_id: int
    category: str
    estimated_minutes: int = Field(gt=0)
    postponed_count: int = Field(default=0, ge=0)


class TaskHistory(BaseModel):
    category: str
    estimated_minutes: int = Field(gt=0)
    outcome: Literal["완료", "미룸", "실패"]


class AnalyzeRequest(BaseModel):
    pending_plans: list[PendingPlan]
    task_history: list[TaskHistory] = []  # 확정된 과거 이력, 오래된 것부터 시간순


# ---- 엔드포인트 ----

@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": _model is not None, "llm_available": _llm.is_available()}


@app.post("/api/ai/plans/classify", response_model=ClassifyResponse)
def classify(req: ClassifyRequest):
    return classify_category(req.content, req.categories, _llm)


@app.post("/api/ai/analyze")
def analyze(req: AnalyzeRequest):
    """plans: 위험도 높은 순(추천 수행 순서)으로 정렬된 계획 리스트.
    각 항목: plan_id, status("기록 없음" / "기록 수집 중(규칙판정)" / "판정됨(모델)"), record_count,
    risk_score, risk_flag (+ 모델 판정이면 risk_prob, reason, duration_bin / 기록이 있으면 recent_failure_streak)
    """
    plans = schedule_plans(
        [p.model_dump() for p in req.pending_plans],
        [t.model_dump() for t in req.task_history],
        _model,
        _threshold,
    )
    return {"plans": plans}