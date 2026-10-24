"""
시드 재현성 점검 - 페르소나 생성 시드를 여러 개로 바꿔가며 A/B/C 성능이 일관되게 나오는지 확인.

SEED=42 하나만으로 낸 성능이 그 시드에서 우연히 좋게 나온 것인지,
어떤 시드를 써도 비슷한 진짜 패턴인지 구분하기 위함.
"""
import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import average_precision_score, roc_auc_score
from sklearn.model_selection import GroupKFold, cross_val_predict
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from bias_data_pipeline import make_random_personas, generate_raw_events, add_features, build_pipeline

N_PERSONAS = 20
SEEDS = [2, 34, 167, 584, 981]  # 5개 시드로 반복


def run_one_seed(seed: int):
    personas = make_random_personas(N_PERSONAS, seed=seed)
    raw = generate_raw_events(personas, seed=seed + 1000)
    data = add_features(raw)

    y = data["y"].values
    groups = data["persona_id"].values
    streak = data["recent_failure_streak"].values.reshape(-1, 1)
    X_full = data[["estimated_minutes", "category_relative_rate", "recent_failure_streak", "category", "time_slot"]]

    gkf = GroupKFold(n_splits=5)

    score_A = (data["recent_failure_streak"].values >= 2).astype(float)
    pipe_B = Pipeline([("scale", StandardScaler()), ("clf", LogisticRegression(class_weight="balanced", max_iter=1000))])
    score_B = cross_val_predict(pipe_B, streak, y, cv=gkf.split(streak, y, groups), method="predict_proba")[:, 1]
    score_C = cross_val_predict(build_pipeline(), X_full, y, cv=gkf.split(X_full, y, groups), method="predict_proba")[:, 1]

    return {
        "seed": seed,
        "auc_A": roc_auc_score(y, score_A), "ap_A": average_precision_score(y, score_A),
        "auc_B": roc_auc_score(y, score_B), "ap_B": average_precision_score(y, score_B),
        "auc_C": roc_auc_score(y, score_C), "ap_C": average_precision_score(y, score_C),
    }


if __name__ == "__main__":
    results = [run_one_seed(s) for s in SEEDS]

    print("=" * 90)
    print(f"{'seed':>6} | {'AUC_A':>7} {'AUC_B':>7} {'AUC_C':>7} | {'AP_A':>7} {'AP_B':>7} {'AP_C':>7}")
    print("-" * 90)
    for r in results:
        print(f"{r['seed']:>6} | {r['auc_A']:>7.3f} {r['auc_B']:>7.3f} {r['auc_C']:>7.3f} | "
              f"{r['ap_A']:>7.3f} {r['ap_B']:>7.3f} {r['ap_C']:>7.3f}")
    print("-" * 90)

    for metric in ["auc_A", "auc_B", "auc_C", "ap_A", "ap_B", "ap_C"]:
        values = np.array([r[metric] for r in results])
        print(f"{metric:>6}: mean={values.mean():.3f}  SD={values.std(ddof=1):.3f}  "
              f"range=[{values.min():.3f}, {values.max():.3f}]")
    print("=" * 90)

    c_beats_a = all(r["auc_C"] > r["auc_A"] for r in results)
    c_beats_b = all(r["auc_C"] > r["auc_B"] for r in results)
    print(f"\n모든 시드에서 C > A (AUC): {c_beats_a}")
    print(f"모든 시드에서 C > B (AUC): {c_beats_b}")