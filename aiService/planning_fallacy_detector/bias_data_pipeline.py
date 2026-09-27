"""
인지편향 탐지 모델의 중요 모듈.
- 합성 사용자(페르소나) 생성 및 과제 수행 이력(완료/미룸/실패) 시뮬레이션
- 이력으로부터 모델 입력용 피처(상대완료율, 연속실패횟수) 계산
- 전처리(StandardScaler+OneHotEncoder) + 로지스틱회귀 파이프라인 정의

train_final_model.py, ab_c_comparison.py 에서 import해서 사용.
페르소나 수(N)는 20개로 최종 확정됨 — 근거: Cameron & Miller(2015) 클러스터강건추론
"""

import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder, StandardScaler

CATEGORIES = ["운동", "공부", "업무", "개인일정"]
TIME_SLOTS = ["아침", "오후", "저녁", "밤"]
ESTIMATED_MINUTES = [30, 60, 90, 120]
STREAK_PENALTY = 0.15
RECENT_WINDOW = 15


def make_random_personas(n_personas: int, seed: int) -> dict:
    rng = np.random.default_rng(seed)
    personas = {}
    for idx in range(n_personas):
        base_rates = {c: rng.uniform(0.65, 0.92) for c in CATEGORIES}
        # 페르소나마다 1~2개 카테고리를 "약점"으로 설정 (완료율 크게 낮춤)
        n_weak = rng.integers(1, 3)
        weak_categories = rng.choice(CATEGORIES, size=n_weak, replace=False)
        for wc in weak_categories:
            base_rates[wc] = rng.uniform(0.25, 0.55)
        drift = {}
        for c in CATEGORIES:
            if rng.random() < 0.4:
                drift[c] = rng.uniform(-0.3, 0.3)
        personas[f"persona_{idx}"] = {
            **base_rates,
            "n_tasks": 220,
            "drift": drift,
        }
    return personas


def generate_raw_events(personas: dict, seed: int) -> pd.DataFrame:
    rng = np.random.default_rng(seed)
    rows = []
    for persona_id, spec in personas.items():
        n_tasks = spec["n_tasks"]
        recent_streak = {c: 0 for c in CATEGORIES}
        for i in range(n_tasks):
            category = rng.choice(CATEGORIES)
            time_slot = rng.choice(TIME_SLOTS)
            est_minutes = int(rng.choice(ESTIMATED_MINUTES))
            base_complete_prob = spec[category]
            progress = i / max(1, n_tasks - 1)
            drift_amount = spec.get("drift", {}).get(category, 0.0)
            base_complete_prob = base_complete_prob + drift_amount * progress
            minutes_penalty = (est_minutes - 30) / 90 * 0.18
            base_complete_prob = base_complete_prob - minutes_penalty
            streak = recent_streak[category]
            penalty = min(streak, 2) * STREAK_PENALTY
            complete_prob = min(0.97, max(0.05, base_complete_prob - penalty))
            completed = rng.random() < complete_prob
            if completed:
                outcome = "완료"
                recent_streak[category] = 0
            else:
                outcome = "미룸" if rng.random() < 0.55 else "실패"
                recent_streak[category] += 1
            rows.append(
                {
                    "persona_id": persona_id,
                    "task_index": i,
                    "category": category,
                    "time_slot": time_slot,
                    "estimated_minutes": est_minutes,
                    "outcome": outcome,
                }
            )
    return pd.DataFrame(rows)


def add_features(df: pd.DataFrame) -> pd.DataFrame:
    df = df.sort_values(["persona_id", "task_index"]).reset_index(drop=True)
    df["y"] = (df["outcome"] != "완료").astype(int)
    cat_rate_col, streak_col = [], []
    for persona_id, group in df.groupby("persona_id"):
        group = group.sort_values("task_index")
        overall_done = []
        cat_done = {c: [] for c in CATEGORIES}
        recent_streak = {c: 0 for c in CATEGORIES}
        for _, row in group.iterrows():
            cat = row["category"]
            recent_overall = overall_done[-RECENT_WINDOW:]
            recent_cat = cat_done[cat][-RECENT_WINDOW:]
            overall_rate = np.mean(recent_overall) if recent_overall else np.nan
            cat_rate = np.mean(recent_cat) if recent_cat else np.nan
            relative_rate = 0.0 if (np.isnan(overall_rate) or np.isnan(cat_rate)) else cat_rate - overall_rate
            cat_rate_col.append(relative_rate)
            streak_col.append(recent_streak[cat])
            is_done = 1 if row["outcome"] == "완료" else 0
            overall_done.append(is_done)
            cat_done[cat].append(is_done)
            recent_streak[cat] = 0 if is_done else recent_streak[cat] + 1
    df["category_relative_rate"] = cat_rate_col
    df["recent_failure_streak"] = streak_col
    return df


def build_pipeline():
    preprocessor = ColumnTransformer(
        transformers=[
            ("num", StandardScaler(), ["estimated_minutes", "category_relative_rate", "recent_failure_streak"]),
            ("cat", OneHotEncoder(handle_unknown="ignore"), ["category", "time_slot"]),
        ]
    )
    model = LogisticRegression(C=1.0, class_weight="balanced", max_iter=1000)
    return Pipeline([("prep", preprocessor), ("clf", model)])


def bootstrap_ci(persona_ids, y, score, metric_fn, n_boot: int = 2000, seed: int = 0):
    """페르소나(클러스터) 단위 붓스트랩으로 단일 지표(예: AUC, AP)의 95% CI를 구한다.
    EBSLN 논문(Table 4, 5, 7)처럼 비교 차이값뿐 아니라 절대 성능 지표에도 CI를 동반하기 위한 공용 함수.

    returns: (point_estimate, boot_mean, ci_low, ci_high)
    """
    rng = np.random.default_rng(seed)
    unique_personas = np.unique(persona_ids)
    idx_by_persona = {p: np.where(persona_ids == p)[0] for p in unique_personas}

    point_estimate = metric_fn(y, score)

    boot_values = []
    for _ in range(n_boot):
        sampled = rng.choice(unique_personas, size=len(unique_personas), replace=True)
        rows = np.concatenate([idx_by_persona[p] for p in sampled])
        # 리샘플링된 묶음 안에 클래스가 하나만 남는 경우(운 나쁘게 y=0 또는 1만 뽑힘) 해당 반복은 건너뜀
        if len(np.unique(y[rows])) < 2:
            continue
        boot_values.append(metric_fn(y[rows], score[rows]))

    boot_values = np.array(boot_values)
    ci_low, ci_high = np.percentile(boot_values, [2.5, 97.5])
    return point_estimate, boot_values.mean(), ci_low, ci_high