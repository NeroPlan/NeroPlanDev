"""
편향 판정 데모(CLI): 고정 시나리오 5종의 과제 이력을 넣어 카테고리별 판정과 편향 경고를 출력한다.
- 행 단위 표: 과거 각 기록 시점의 피처와 모델 위험확률(디버깅·설명용, 프론트에는 안 나감)
- 최종 요약: bias_diagnosis.diagnose_history() 결과

실행: python3 demo_bias_detection_v2.py
"""
from bias_data_pipeline import MIN_RECORDS_FOR_PREDICTION, MODEL_FEATURES
from bias_diagnosis import compute_features_for_history, diagnose_history, load_model, model_info
from demo_scenarios import SCENARIO_LABELS, SCENARIOS

_, model, threshold = load_model()

print("=" * 70)
info = model_info()
print(f"[로드된 모델 정보] 페르소나 {info['n_personas']}개 기준 학습됨")
print(f"  교차검증 AUC={info['auc']:.3f} (95% CI {info['auc_ci'][0]:.3f}-{info['auc_ci'][1]:.3f}), "
      f"Average Precision={info['ap']:.3f} (95% CI {info['ap_ci'][0]:.3f}-{info['ap_ci'][1]:.3f})")
print(f"  판정 임계값(precision>=0.8 지점) = {info['threshold']:.3f}")
print("=" * 70)


def show(scenario_key: str):
    task_history = SCENARIOS[scenario_key]
    label = SCENARIO_LABELS[scenario_key]

    # 행 단위 표: 카테고리 기록이 10건 이상 쌓인 시점의 행만 모델 위험확률을 채움
    df = compute_features_for_history(task_history)
    df["risk_prob"] = None
    enough = df["category_record_count"] + 1 >= MIN_RECORDS_FOR_PREDICTION
    if enough.any():
        df.loc[enough, "risk_prob"] = model.predict_proba(df.loc[enough, MODEL_FEATURES])[:, 1]
    df["risk_flag"] = df["risk_prob"].apply(lambda p: (p is not None) and p >= threshold)
    print(f"\n--- {label} ---")
    print(df[["category", "outcome", "recent_failure_streak", "streak_bin", "category_relative_rate", "risk_prob", "risk_flag"]]
          .to_string(index=False))

    # 최종 요약
    result = diagnose_history(task_history)
    print()
    for cat, status in result["categories"].items():
        if status["status"] == "기록 수집 중":
            print(f"[{cat}] 기록 수집 중 ({status['record_count']}/{status['required']}건) — 아직 판정 안 함")
    if result["bias_detected"]:
        for w in result["warnings"]:
            print(f"[편향 경고] {w['message']}")
            print(f"    근거(최초 감지 시점): {w['reason']}")
            print(f"    당시 모델 예측 위험확률: {w['risk_prob_at_detection']:.2f} (판정임계값 {w['threshold']:.2f} 초과)")
            if w["recovered"]:
                print(f"    ※ 현재(가장 최근 결과 반영) 위험확률 {w['current_risk_prob']:.2f}로 임계값 밑 — 회복 신호")
    elif not any(s["status"] == "기록 수집 중" for s in result["categories"].values()):
        print("특이 편향 패턴이 감지되지 않았습니다.")


for key in SCENARIOS:
    show(key)