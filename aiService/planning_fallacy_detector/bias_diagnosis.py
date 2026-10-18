"""
계획 미완료 위험 판정 공용 로직.

- 과거 이력으로 피처 계산(학습 시점과 같은 방식)과 다음 계획용 피처 계산
- 카테고리별 편향 경고(diagnose_history): 기록 10건 미만은 "기록 수집 중", 10건 이상은 모델 판정
- 위험 판정의 주요 영향 요인 문장(explain_factors)

여러 계획을 위험도 순으로 재배열하는 로직은 success_rate_scheduler/schedule_plans.py에 있고,
이 모듈의 features_for_next_task, explain_factors를 가져다 씀.
"""
import os

import joblib
import pandas as pd

from bias_data_pipeline import (
    MIN_RECORDS_FOR_PREDICTION,
    MODEL_FEATURES,
    _duration_bin,
    _streak_bin,
    relative_rate,
)

DEFAULT_MODEL_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "final_bias_model.joblib")

# 같은 카테고리에서 모델 위험 판정이 이 횟수만큼 연속되면 편향 경고.
# 개별 판정 임계값이 precision 0.8 기준이라 2회 연속이면 패턴으로 볼 수 있고, 경고를 1건이라도 빨리 주기 위함.
STREAK_THRESHOLD_FOR_FLAG = 2

_bundle = None
_model = None
_threshold = None


def load_model(path: str = DEFAULT_MODEL_PATH):
    """모델을 한 번만 로드해서 재사용."""
    global _bundle, _model, _threshold
    if _model is None:
        _bundle = joblib.load(path)
        _model = _bundle["model"]
        _threshold = _bundle["threshold"]
    return _bundle, _model, _threshold


def model_info() -> dict:
    bundle, _, threshold = load_model()
    return {
        "n_personas": bundle["n_personas"],
        "auc": round(bundle["auc"], 3),
        "auc_ci": [round(v, 3) for v in bundle["auc_ci"]],
        "ap": round(bundle["ap"], 3),
        "ap_ci": [round(v, 3) for v in bundle["ap_ci"]],
        "threshold": round(threshold, 3),
    }


def compute_features_for_history(task_history: list[dict]) -> pd.DataFrame:
    """과거 이력(오래된 것부터 시간순)의 각 기록에 대해, 그 기록 '직전' 시점의 피처를 계산.
    편향 경고의 연속 위험 판정과 데모 표에 씀. 입력 필드: category, estimated_minutes, outcome(완료/미룸/실패)
    """
    rows = []
    cat_done, recent_streak = {}, {}
    for task in task_history:
        cat = task["category"]
        cat_done.setdefault(cat, [])
        recent_streak.setdefault(cat, 0)

        rows.append({
            **task,
            "duration_bin": _duration_bin(task["estimated_minutes"]),
            "category_relative_rate": relative_rate(cat_done, cat),
            "recent_failure_streak": recent_streak[cat],
            "streak_bin": _streak_bin(recent_streak[cat]),
            "category_record_count": len(cat_done[cat]),  # 이 기록 직전까지 확정된 같은 카테고리 기록 수
        })

        is_done = 1 if task["outcome"] == "완료" else 0
        cat_done[cat].append(is_done)
        recent_streak[cat] = 0 if is_done else recent_streak[cat] + 1

    return pd.DataFrame(rows)


def features_for_next_task(task_history: list[dict], category: str, estimated_minutes: float) -> dict:
    """아직 수행 전인 다음 계획 하나의 피처.
    완료율 편차·연속 실패는 과거 기록 전체(가장 최근 결과 포함)로 계산하고, 소요시간은 이 계획의 값을 씀.
    """
    cat_done, recent_streak = {}, {}
    for task in task_history:
        cat = task["category"]
        is_done = 1 if task["outcome"] == "완료" else 0
        cat_done.setdefault(cat, []).append(is_done)
        recent_streak[cat] = 0 if is_done else recent_streak.get(cat, 0) + 1

    streak = recent_streak.get(category, 0)
    return {
        "duration_bin": _duration_bin(estimated_minutes),
        "category_relative_rate": relative_rate(cat_done, category),
        "recent_failure_streak": streak,
        "streak_bin": _streak_bin(streak),
        "category_record_count": len(cat_done.get(category, [])),
    }


_REFERENCE = {"duration_bin": "짧음", "streak_bin": "0회"}  # 기여도 비교 기준(가장 안전한 수준)
_DURATION_TEXT = {"보통": "30분 넘게", "김": "60분 넘게"}


def explain_factors(model, features: dict, category: str = "같은") -> dict:
    """위험 판정의 주요 영향 요인과 설명 문장.

    로지스틱 회귀라서 각 피처의 기여도 = 계수 × (이 계획의 값 − 기준값).
    기준값: 소요시간 '짧음', 연속 실패 '0회', 완료율 편차는 학습 데이터 평균(표준화 0).
    위험을 가장 많이 올린 요인 하나를 골라 템플릿 문장으로 만듦(같은 판정에는 항상 같은 문장).
    """
    pipe = getattr(model, "base_model", model)
    prep, clf = pipe.named_steps["prep"], pipe.named_steps["clf"]
    row = pd.DataFrame([{k: features[k] for k in MODEL_FEATURES}])
    ref = pd.DataFrame([{**{k: features[k] for k in MODEL_FEATURES}, **_REFERENCE,
                         "category_relative_rate": float(prep.named_transformers_["rate"].mean_[0])}])
    diff = (prep.transform(row) - prep.transform(ref))[0] * clf.coef_[0]

    contributions = {name: float(diff[prep.output_indices_[name]].sum()) for name in ("duration", "rate", "streak")}
    main = max(contributions, key=contributions.get)
    if contributions[main] <= 0:
        return {"main_factor": None, "reason": "위험을 크게 높이는 요인이 없습니다.", "contributions": contributions}

    if main == "streak":
        # '이 계획의 미룸 횟수'와 헷갈리지 않게, 같은 카테고리의 과거 다른 계획들 결과라는 점을 밝힘
        reason = (f"{category} 카테고리의 최근 계획 {features['recent_failure_streak']}개를 "
                  "연속으로 완료하지 못했어요(미룸·실패 포함).")
    elif main == "rate":
        gap = abs(features["category_relative_rate"]) * 100
        reason = f"이 카테고리 완료율이 본인 평균보다 {gap:.0f}%p 낮은 것이 가장 큰 요인입니다."
    else:
        # 소요시간은 사용자가 정한 값이 아니라 LLM 추정값이라, 추정임을 밝히고 행동을 제안함
        reason = (f"이 계획은 {_DURATION_TEXT.get(features['duration_bin'], features['duration_bin'])} 걸릴 것으로 예상돼요. "
                  "나눠서 해보는 건 어떨까요?")
    return {"main_factor": main, "reason": reason, "contributions": contributions}


def diagnose_history(task_history: list[dict]) -> dict:
    """과거 이력으로 카테고리별 현재 상태와 편향 경고를 반환.

    - 카테고리 기록 10건 미만: "기록 수집 중" 상태만 반환(모델 예측 안 함)
    - 10건 이상: 과거 각 시점의 모델 판정이 STREAK_THRESHOLD_FOR_FLAG회 연속 위험이면 편향 경고.
      경고 근거는 처음 연속 위험이 채워진 시점 값을 쓰고, 현재 위험(가장 최근 결과까지 반영)이
      임계값 밑이면 recovered=True
    """
    if not task_history:
        return {"bias_detected": False, "warnings": [], "categories": {}}

    _, model, threshold = load_model()
    df = compute_features_for_history(task_history)

    categories_status = {}
    warnings = []
    for cat, group in df.groupby("category"):
        latest = group.iloc[-1]
        n_records = len(group)

        if n_records < MIN_RECORDS_FOR_PREDICTION:
            categories_status[cat] = {
                "status": "기록 수집 중",
                "record_count": n_records,
                "required": MIN_RECORDS_FOR_PREDICTION,
            }
            continue

        risk_prob = model.predict_proba(group[MODEL_FEATURES])[:, 1]
        risk_flag = risk_prob >= threshold

        streak = 0
        trigger_idx = None
        for i, flag in enumerate(risk_flag):
            streak = streak + 1 if flag else 0
            if streak >= STREAK_THRESHOLD_FOR_FLAG:
                trigger_idx = i
                break

        # 현재 상태: 다음 계획의 소요시간은 아직 모르므로 이 카테고리 마지막 기록의 소요시간을 씀
        now = features_for_next_task(task_history, cat, latest["estimated_minutes"])
        current_risk_prob = float(model.predict_proba(pd.DataFrame([{k: now[k] for k in MODEL_FEATURES}]))[:, 1][0])
        current_risk_flag = current_risk_prob >= threshold
        categories_status[cat] = {
            "status": "판정됨",
            "record_count": n_records,
            "category_relative_rate": round(float(now["category_relative_rate"]), 3),
            "recent_failure_streak": int(now["recent_failure_streak"]),
            "risk_prob": round(current_risk_prob, 3),
            "risk_flag": bool(current_risk_flag),
        }

        if trigger_idx is not None:
            trigger_row = group.iloc[trigger_idx]
            rate_at = float(trigger_row["category_relative_rate"])
            rate_text = f"완료율이 본인 평균보다 {abs(rate_at) * 100:.0f}%p 낮고, " if rate_at < 0 else ""
            warnings.append({
                "category": cat,
                "message": f"'{cat}' 카테고리에서 반복해서 미루거나 완료하지 못하는 패턴이 감지되었습니다.",
                "reason": f"{rate_text}연속 {int(trigger_row['recent_failure_streak'])}회 미완료됨.",
                "risk_prob_at_detection": round(float(risk_prob[trigger_idx]), 3),
                "threshold": round(float(threshold), 3),
                "recovered": bool(not current_risk_flag),
                "current_risk_prob": round(current_risk_prob, 3),
            })

    return {
        "bias_detected": len(warnings) > 0,
        "warnings": warnings,
        "categories": categories_status,  # 프론트 "기록 수집 중" 배지 표시용
    }