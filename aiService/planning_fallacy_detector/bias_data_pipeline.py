"""
계획 미완료(미룸/실패) 위험 예측 모델의 데이터, 학습 공용 모듈.

- 합성 사용자(페르소나)와 과제 수행 이력(완료/미룸/실패) 생성
- 모델 입력 피처 계산: 예상 소요시간 구간, 카테고리 완료율 편차, 연속 실패 구간
- 전처리 + 로지스틱 회귀 파이프라인, Platt scaling 확률 보정
- 페르소나(클러스터) 단위 붓스트랩 신뢰구간

train_final_model.py, bias_diagnosis.py에서 import해서 사용.
"""

import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import FunctionTransformer, OneHotEncoder, StandardScaler

# ---- 합성 데이터 생성용 설정 (모델 입력과는 무관) ----
CATEGORIES = ["운동", "공부", "업무", "개인일정"]  # 합성 사용자의 카테고리. 실제 서비스는 LLM이 사용자별로 생성
TIME_SLOTS = ["아침", "오후", "저녁", "밤"]  # 합성 기록에만 채우는 값, 모델·판정에는 쓰지 않음
ESTIMATED_MINUTES = [30, 60, 90, 120]  # 합성 기록의 예상 소요시간 후보 (LLM 추정값도 이 네 값 중 하나로 맞춤)
STREAK_PENALTY = 0.15  # 연속 미완료 1회당 완료 확률 감소폭 (최대 2회분)

# ---- 판정 기준 ----
MIN_RECORDS_FOR_PREDICTION = 10  # 카테고리 기록이 이 건수 미만이면 "기록 수집 중"(모델 예측 안 함)
MIN_RECORDS_FOR_RATE = 3  # 카테고리 기록이 이 건수 미만이면 완료율 편차를 0으로 처리
RULE_STREAK_THRESHOLD = 2  # 기록 수집 중 구간에서 "위험"으로 볼 연속 미완료 횟수 (재배열 순서용)
DURATION_SCALE = 0.5  # 예상 소요시간 피처 축소 계수 — LLM 추정 오차의 영향을 줄이기 위함

MODEL_FEATURES = ["duration_bin", "category_relative_rate", "streak_bin"]


def _duration_bin(minutes: float) -> str:
    """예상 소요시간(분) → 짧음(30분 이하) / 보통(31~60분) / 김(60분 초과)."""
    if minutes <= 30:
        return "짧음"
    if minutes <= 60:
        return "보통"
    return "김"


def _streak_bin(streak: int) -> str:
    """연속 미완료 횟수 → 0회 / 1~2회 / 3회 이상."""
    if streak == 0:
        return "0회"
    if streak <= 2:
        return "1~2회"
    return "3회 이상"


def relative_rate(cat_done: dict, category: str) -> float:
    """카테고리 완료율 − 본인 전체 완료율. 카테고리 기록이 MIN_RECORDS_FOR_RATE건 미만이면 0.
    cat_done: {카테고리: [완료=1, 미완료=0, ...]} (이 시점까지 확정된 기록)
    """
    records = cat_done.get(category, [])
    if len(records) < MIN_RECORDS_FOR_RATE:
        return 0.0
    all_done = [v for vals in cat_done.values() for v in vals]
    return sum(records) / len(records) - sum(all_done) / len(all_done)


def half_scale(X: np.ndarray) -> np.ndarray:
    """예상 소요시간 원-핫 결과에 DURATION_SCALE을 곱함. joblib 저장을 위해 모듈 top-level 함수로 둠."""
    return X * DURATION_SCALE


# ---- Platt scaling 확률 보정 ----

class PlattCalibratedModel:
    """Platt scaling으로 보정한 확률을 내보내는 모델 래퍼.

    class_weight="balanced"로 학습하면 미완료 확률이 실제 빈도와 어긋나므로(순위·AUC에는 영향 없음),
    사용자에게 보여주는 확률을 실제 빈도에 맞춘다.
    보정식: p = 1 / (1 + exp(-(a * logit(p_원본) + b))), a·b는 교차검증 out-of-fold 확률로 학습.
    predict_proba 인터페이스가 sklearn 모델과 같아서 그대로 바꿔 끼워 쓸 수 있음.
    """

    def __init__(self, base_model, a: float, b: float):
        self.base_model = base_model
        self.a = a
        self.b = b

    def raw_proba(self, X) -> np.ndarray:
        return self.base_model.predict_proba(X)[:, 1]

    def predict_proba(self, X) -> np.ndarray:
        p = calibrate(self.raw_proba(X), self.a, self.b)
        return np.column_stack([1 - p, p])


def _logit(p: np.ndarray) -> np.ndarray:
    p = np.clip(p, 1e-6, 1 - 1e-6)
    return np.log(p / (1 - p))


def fit_platt(raw_probs: np.ndarray, y: np.ndarray) -> tuple:
    """out-of-fold 원본 확률과 정답으로 Platt 계수(a, b)를 학습."""
    lr = LogisticRegression(C=1e6, max_iter=1000)  # 계수 2개만 맞추는 용도라 사실상 규제 없음
    lr.fit(_logit(raw_probs).reshape(-1, 1), y)
    return float(lr.coef_[0][0]), float(lr.intercept_[0])


def calibrate(raw_probs: np.ndarray, a: float, b: float) -> np.ndarray:
    return 1 / (1 + np.exp(-(a * _logit(raw_probs) + b)))


# ---- 합성 데이터 생성 ----

def make_random_personas(n_personas: int, seed: int) -> dict:
    """페르소나마다 카테고리별 기본 완료율을 정하고, 1~2개 카테고리를 약점(낮은 완료율)으로 둠."""
    rng = np.random.default_rng(seed)
    personas = {}
    for idx in range(n_personas):
        base_rates = {c: rng.uniform(0.65, 0.92) for c in CATEGORIES}
        n_weak = rng.integers(1, 3)
        weak_categories = rng.choice(CATEGORIES, size=n_weak, replace=False)
        for wc in weak_categories:
            base_rates[wc] = rng.uniform(0.25, 0.55)
        drift = {}  # 일부 카테고리는 시간이 지나며 완료율이 오르거나 내려감
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
    """페르소나별 과제 수행 이력 생성.
    완료 확률 = 카테고리 기본 완료율 + 드리프트 − 소요시간 패널티(최대 18%p) − 연속 미완료 패널티(최대 30%p).
    """
    rng = np.random.default_rng(seed)
    rows = []
    for persona_id, spec in personas.items():
        n_tasks = spec["n_tasks"]
        recent_streak = {c: 0 for c in CATEGORIES}
        for i in range(n_tasks):
            category = rng.choice(CATEGORIES)
            time_slot = rng.choice(TIME_SLOTS)  # 모델 미사용. 난수 순서 유지를 위해 생성은 그대로 둠
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
    """학습용 피처 추가. 각 과제 시점 '직전까지' 확정된 기록만으로 계산(데이터 누수 방지).
    - category_relative_rate: 카테고리 완료율 − 본인 전체 완료율 (카테고리 기록 3건 미만이면 0)
    - recent_failure_streak / streak_bin: 같은 카테고리의 직전 연속 미완료 횟수와 그 구간
    - duration_bin: 예상 소요시간 구간
    - y: 미룸·실패면 1, 완료면 0
    """
    df = df.sort_values(["persona_id", "task_index"]).reset_index(drop=True)
    df["y"] = (df["outcome"] != "완료").astype(int)
    cat_rate_col, streak_col, duration_bin_col, streak_bin_col, cat_record_count_col = [], [], [], [], []
    for _, group in df.groupby("persona_id"):
        group = group.sort_values("task_index")
        cat_done = {c: [] for c in CATEGORIES}
        recent_streak = {c: 0 for c in CATEGORIES}
        for _, row in group.iterrows():
            cat = row["category"]
            cat_rate_col.append(relative_rate(cat_done, cat))
            streak_col.append(recent_streak[cat])
            duration_bin_col.append(_duration_bin(row["estimated_minutes"]))
            streak_bin_col.append(_streak_bin(recent_streak[cat]))
            cat_record_count_col.append(len(cat_done[cat]))

            is_done = 1 if row["outcome"] == "완료" else 0
            cat_done[cat].append(is_done)
            recent_streak[cat] = 0 if is_done else recent_streak[cat] + 1
    df["category_relative_rate"] = cat_rate_col
    df["recent_failure_streak"] = streak_col
    df["duration_bin"] = duration_bin_col
    df["streak_bin"] = streak_bin_col
    df["category_record_count"] = cat_record_count_col
    return df


def build_pipeline():
    """전처리 + 로지스틱 회귀. 입력은 MODEL_FEATURES 3개.
    - duration_bin: 원-핫 후 DURATION_SCALE 곱함
    - streak_bin: 원-핫
    - category_relative_rate: 표준화
    """
    duration_pipe = Pipeline([
        ("ohe", OneHotEncoder(handle_unknown="ignore", sparse_output=False)),
        ("half", FunctionTransformer(half_scale)),
    ])
    preprocessor = ColumnTransformer(
        transformers=[
            ("duration", duration_pipe, ["duration_bin"]),
            ("streak", OneHotEncoder(handle_unknown="ignore", sparse_output=False), ["streak_bin"]),
            ("rate", StandardScaler(), ["category_relative_rate"]),
        ]
    )
    model = LogisticRegression(C=1.0, class_weight="balanced", max_iter=1000)
    return Pipeline([("prep", preprocessor), ("clf", model)])


def bootstrap_ci(persona_ids, y, score, metric_fn, n_boot: int = 2000, seed: int = 0):
    """페르소나(클러스터) 단위 붓스트랩으로 지표(AUC, AP 등)의 95% CI를 구한다.
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
        if len(np.unique(y[rows])) < 2:  # 한 클래스만 뽑힌 반복은 건너뜀
            continue
        boot_values.append(metric_fn(y[rows], score[rows]))

    boot_values = np.array(boot_values)
    ci_low, ci_high = np.percentile(boot_values, [2.5, 97.5])
    return point_estimate, boot_values.mean(), ci_low, ci_high