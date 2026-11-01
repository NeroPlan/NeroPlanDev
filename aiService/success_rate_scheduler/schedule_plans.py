"""
성공률 기반 계획 재배열.

입력된 계획들을 사용자의 과거 이력 기준 미완료 위험도가 높은 순으로 재배열한다(hard_first).
하기 힘든 계획을 앞 순서에 처리하게 하고, 위험한 계획이 계속 뒤로 밀리는 것을 막기 위함.

위험 판정(피처 계산·모델)은 planning_fallacy_detector의 bias_data_pipeline.py / bias_diagnosis.py를 씀.
두 폴더가 aiService/ 아래 나란히 있으므로 import 경로(sys.path 등)를 맞춰서 사용(test_schedule.py 참고).
"""
import pandas as pd

from bias_data_pipeline import MIN_RECORDS_FOR_PREDICTION, MODEL_FEATURES, RULE_STREAK_THRESHOLD
from bias_diagnosis import explain_factors, features_for_next_task


def _plan_risk(plan: dict, task_history: list[dict], model, threshold: float) -> dict:
    """계획 하나의 현재 위험도.

    - 같은 카테고리 기록 0건: "기록 없음", 위험 0
    - 10건 미만: "기록 수집 중(규칙판정)", 최근 연속 미완료가 RULE_STREAK_THRESHOLD회 이상이면 위험 1, 아니면 0
      예측은 하지 않고, 지금까지의 실제 완료율(completion_rate)을 같이 줌
    - 10건 이상: "판정됨(모델)", 보정된 미완료 확률(risk_prob)
    risk_score는 정렬용 값(0~1, 클수록 위험), reason은 사용자에게 보여줄 판단 근거 문장으로 항상 채움.
    """
    if "estimated_minutes" not in plan:
        raise ValueError(f"계획에 estimated_minutes가 없음: {plan}")

    f = features_for_next_task(task_history, plan["category"], plan["estimated_minutes"])
    n_records = f["category_record_count"]

    category = plan["category"]
    if n_records == 0:
        return {"status": "기록 없음", "record_count": 0, "risk_score": 0.0, "risk_flag": False,
                "reason": f"{category} 카테고리의 첫 계획이라 아직 판단할 기록이 없어요."}

    if n_records < MIN_RECORDS_FOR_PREDICTION:
        streak = f["recent_failure_streak"]
        is_risky = streak >= RULE_STREAK_THRESHOLD
        done_count = sum(1 for t in task_history if t["category"] == category and t["outcome"] == "완료")
        if is_risky:
            reason = f"{category} 카테고리의 최근 계획 {streak}개를 연속으로 완료하지 못했어요(미룸·실패 포함)."
        else:
            reason = (f"{category} 카테고리 기록 {n_records}건 중 {done_count}건을 완료했어요. "
                      f"기록이 {MIN_RECORDS_FOR_PREDICTION}건 쌓이면 성공률을 예측해요.")
        return {
            "status": "기록 수집 중(규칙판정)",
            "record_count": n_records,
            "recent_failure_streak": streak,
            "completion_rate": round(done_count / n_records, 3),
            "risk_score": 1.0 if is_risky else 0.0,
            "risk_flag": bool(is_risky),
            "reason": reason,
        }

    X = pd.DataFrame([{k: f[k] for k in MODEL_FEATURES}])
    risk_prob = float(model.predict_proba(X)[:, 1][0])
    return {
        "status": "판정됨(모델)",
        "record_count": n_records,
        "recent_failure_streak": f["recent_failure_streak"],
        "duration_bin": f["duration_bin"],
        "risk_score": risk_prob,
        "risk_prob": round(risk_prob, 3),
        "risk_flag": bool(risk_prob >= threshold),
        "reason": explain_factors(model, f, category)["reason"],
    }


def schedule_plans(pending_plans: list[dict], task_history: list[dict], model, threshold: float) -> list[dict]:
    """입력된 계획들을 위험도 높은 순으로 재배열해서 돌려줌.

    - pending_plans: 재배열할 계획 리스트.
        필수: "category", "estimated_minutes"(분)
        선택: "postponed_count"(이 계획이 미뤄진 횟수, 없으면 0). 그 외 필드(plan_id, title 등)는 그대로 돌려줌
    - task_history: 확정된 과거 이력(오래된 것부터 시간순), 각 항목 category / estimated_minutes / outcome
    - model, threshold: bias_diagnosis.load_model()로 불러온 모델과 판정 임계값

    정렬: risk_score 높은 순 → 같으면 postponed_count 많은 순 → 그래도 같으면 입력 순서(안정 정렬).
    반환: 각 계획에 status / record_count / risk_score / risk_flag / reason
         (+기록 수집 중이면 completion_rate, 모델 판정이면 risk_prob)를 덧붙인 리스트.
    """
    annotated = [{**plan, **_plan_risk(plan, task_history, model, threshold)} for plan in pending_plans]
    annotated.sort(key=lambda p: (p["risk_score"], p.get("postponed_count", 0)), reverse=True)
    return annotated