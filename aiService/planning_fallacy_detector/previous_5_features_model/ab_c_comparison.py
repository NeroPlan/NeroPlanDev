"""
A/B/C 3단 비교 (알고리즘 통제) + 페르소나 클러스터 붓스트랩(B=2000)
N=20 페르소나 기준, bias_data_pipeline.py의 생성 로직을 그대로 사용.

A: 규칙 기반(if문) — recent_failure_streak >= 2 면 위험(1), 아니면 0
B: 단일피처 로지스틱회귀 — recent_failure_streak 하나만 사용 (A와 피처 동일, 알고리즘만 다름)
C: 전체모델 — 절대피처3 + 상대피처2 (bias_data_pipeline.build_pipeline)

A vs C = ML 도입 자체의 가치
B vs C = 피처 추가의 가치 (알고리즘 통제)

모든 성능 지표(A/B/C 각각의 절대 AUC·AP, C-A/C-B 차이값)에 페르소나 클러스터 붓스트랩 95% CI를 동반.
"""
import numpy as np
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import average_precision_score, roc_auc_score
from sklearn.model_selection import GroupKFold, cross_val_predict
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from bias_data_pipeline import make_random_personas, generate_raw_events, add_features, build_pipeline, bootstrap_ci

N_PERSONAS = 20
SEED = 42
N_BOOTSTRAP = 2000


def get_oof_scores(n_personas: int, seed: int):
    personas = make_random_personas(n_personas, seed=seed)
    raw = generate_raw_events(personas, seed=seed + 1000)
    data = add_features(raw)

    y = data["y"].values
    groups = data["persona_id"].values
    streak = data["recent_failure_streak"].values.reshape(-1, 1)
    X_full = data[["estimated_minutes", "category_relative_rate", "recent_failure_streak", "category", "time_slot"]]

    k = min(5, n_personas)
    gkf = GroupKFold(n_splits=k)

    # A: 규칙(if문) — streak>=2 면 1, 아니면 0. 학습 없음. 그대로 스코어로 사용.
    score_A = (data["recent_failure_streak"].values >= 2).astype(float)

    # B: 단일피처 로지스틱회귀 (streak만)
    pipe_B = Pipeline([("scale", StandardScaler()), ("clf", LogisticRegression(class_weight="balanced", max_iter=1000))])
    score_B = cross_val_predict(pipe_B, streak, y, cv=gkf.split(streak, y, groups), method="predict_proba")[:, 1]

    # C: 전체모델
    score_C = cross_val_predict(build_pipeline(), X_full, y, cv=gkf.split(X_full, y, groups), method="predict_proba")[:, 1]

    return data["persona_id"].values, y, score_A, score_B, score_C


def cluster_bootstrap_diff(persona_ids, y, score_x, score_c, metric_fn, n_boot=N_BOOTSTRAP, seed=0):
    """페르소나 단위로 리샘플링해서 (C - X) 차이의 붓스트랩 분포를 만든다. (짝지은/paired 붓스트랩)"""
    rng = np.random.default_rng(seed)
    unique_personas = np.unique(persona_ids)
    diffs = []
    # persona_id -> row indices (재사용을 위해 미리 캐시)
    idx_by_persona = {p: np.where(persona_ids == p)[0] for p in unique_personas}
    for _ in range(n_boot):
        sampled = rng.choice(unique_personas, size=len(unique_personas), replace=True)
        rows = np.concatenate([idx_by_persona[p] for p in sampled])
        m_x = metric_fn(y[rows], score_x[rows])
        m_c = metric_fn(y[rows], score_c[rows])
        diffs.append(m_c - m_x)
    diffs = np.array(diffs)
    ci_low, ci_high = np.percentile(diffs, [2.5, 97.5])
    return diffs.mean(), ci_low, ci_high


def auc_only(y, s):
    return roc_auc_score(y, s)


def ap_only(y, s):
    return average_precision_score(y, s)


if __name__ == "__main__":
    persona_ids, y, score_A, score_B, score_C = get_oof_scores(N_PERSONAS, SEED)

    print("=" * 70)
    print(f"[N={N_PERSONAS} 페르소나] out-of-fold 성능 (점추정치, 페르소나 클러스터 붓스트랩 95% CI)")
    for label, score in [("A(규칙 if문)     ", score_A), ("B(단일피처 LR)   ", score_B), ("C(전체모델 5피처)", score_C)]:
        auc, _, auc_lo, auc_hi = bootstrap_ci(persona_ids, y, score, auc_only, seed=10)
        ap, _, ap_lo, ap_hi = bootstrap_ci(persona_ids, y, score, ap_only, seed=11)
        print(f"  {label}  AUC={auc:.3f} (95% CI {auc_lo:.3f}-{auc_hi:.3f})   AP={ap:.3f} (95% CI {ap_lo:.3f}-{ap_hi:.3f})")
    print("=" * 70)

    print(f"\n[페르소나 클러스터 붓스트랩(짝지은/paired), B={N_BOOTSTRAP}회]")
    print("  주의: 위 개별 CI는 각 모델 단독 붓스트랩이라 서로 겹칠 수 있음.")
    for label, score_x in [("A", score_A), ("B", score_B)]:
        d_auc, lo_auc, hi_auc = cluster_bootstrap_diff(persona_ids, y, score_x, score_C, auc_only, seed=1)
        d_ap, lo_ap, hi_ap = cluster_bootstrap_diff(persona_ids, y, score_x, score_C, ap_only, seed=2)
        sig_auc = "유의" if not (lo_auc <= 0 <= hi_auc) else "비유의(0 포함)"
        sig_ap = "유의" if not (lo_ap <= 0 <= hi_ap) else "비유의(0 포함)"
        print(f"\n  C - {label}  (AUC 차이)  mean={d_auc:+.3f}  95% CI=[{lo_auc:+.3f}, {hi_auc:+.3f}]  -> {sig_auc}")
        print(f"  C - {label}  (AP  차이)  mean={d_ap:+.3f}  95% CI=[{lo_ap:+.3f}, {hi_ap:+.3f}]  -> {sig_ap}")