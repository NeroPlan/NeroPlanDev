"""
시연용 데모: 가상의 사용자 과제 이력을 입력하면
"인지편향 위험" 여부를 모델이 판정하고, 왜 그렇게 판정했는지 설명까지 출력한다.

실행: python3 demo_bias_detection.py
"""
import joblib
import numpy as np
import pandas as pd

RECENT_WINDOW = 15
STREAK_THRESHOLD_FOR_FLAG = 3  # 같은 카테고리에서 몇 번 연속 위험이면 "편향 플래그"로 집계할지

bundle = joblib.load("final_bias_model.joblib")
model = bundle["model"]
threshold = bundle["threshold"]

print("=" * 70)
print(f"[로드된 모델 정보] 페르소나 {bundle['n_personas']}개 기준 학습됨")
if "auc_ci" in bundle:
    print(f"  교차검증 AUC={bundle['auc']:.3f} (95% CI {bundle['auc_ci'][0]:.3f}-{bundle['auc_ci'][1]:.3f}), "
          f"Average Precision={bundle['ap']:.3f} (95% CI {bundle['ap_ci'][0]:.3f}-{bundle['ap_ci'][1]:.3f})")
else:
    print(f"  교차검증 AUC={bundle['auc']:.3f}, Average Precision={bundle['ap']:.3f}")
print(f"  판정 임계값(precision>=0.8 지점) = {threshold:.3f}")
print("=" * 70)


def compute_features_for_history(task_history: list[dict]) -> pd.DataFrame:
    """과제 이력(시간순 리스트)을 받아 각 시점 기준 피처를 계산.
    실제 서비스에서는 DB에 쌓인 과거 이력을 조회해서 이 로직으로 피처를 만듦.
    """
    rows = []
    overall_done, cat_done, recent_streak = [], {}, {}
    for task in task_history:
        cat = task["category"]
        cat_done.setdefault(cat, [])
        recent_streak.setdefault(cat, 0)

        recent_overall = overall_done[-RECENT_WINDOW:]
        recent_cat = cat_done[cat][-RECENT_WINDOW:]
        overall_rate = np.mean(recent_overall) if recent_overall else np.nan
        cat_rate = np.mean(recent_cat) if recent_cat else np.nan
        relative_rate = 0.0 if (np.isnan(overall_rate) or np.isnan(cat_rate)) else cat_rate - overall_rate

        rows.append({
            **task,
            "category_relative_rate": relative_rate,
            "recent_failure_streak": recent_streak[cat],
        })

        is_done = 1 if task["outcome"] == "완료" else 0
        overall_done.append(is_done)
        cat_done[cat].append(is_done)
        recent_streak[cat] = 0 if is_done else recent_streak[cat] + 1

    return pd.DataFrame(rows)


def diagnose(task_history: list[dict], user_label: str):
    df = compute_features_for_history(task_history)
    X = df[["estimated_minutes", "category_relative_rate", "recent_failure_streak", "category", "time_slot"]]
    probs = model.predict_proba(X)[:, 1]
    df["risk_prob"] = probs
    df["risk_flag"] = df["risk_prob"] >= threshold

    print(f"\n--- {user_label} ---")
    print(df[["category", "time_slot", "outcome", "recent_failure_streak", "category_relative_rate", "risk_prob", "risk_flag"]]
          .round(3).to_string(index=False))

    # 카테고리별로 연속 위험플래그가 임계 횟수 이상이면 "편향 플래그" 집계.
    # 근거는 '가장 최근 행'이 아니라 '3연속이 처음 채워진 시점'의 행을 써야 함 —
    # 안 그러면 그 이후 회복해서 최근엔 안전한 사용자도 "최근 행" 기준으로는
    # 위험확률이 임계값 밑인데 "초과됨"이라고 잘못 표시되는 모순이 생김.
    bias_flags = {}  # cat -> 3연속이 처음 채워진 시점의 row
    for cat, group in df.groupby("category"):
        streak = 0
        for idx, row in group.iterrows():
            streak = streak + 1 if row["risk_flag"] else 0
            if streak >= STREAK_THRESHOLD_FOR_FLAG:
                bias_flags[cat] = row
                break

    print()
    if bias_flags:
        for cat, trigger_row in bias_flags.items():
            print(f"[편향 경고] '{cat}' 카테고리에서 계획 오류(planning fallacy) 패턴이 감지되었습니다.")
            print(f"    근거(최초 감지 시점): 이 카테고리 완료율이 본인 평균보다 {abs(trigger_row['category_relative_rate'])*100:.0f}%p 낮고, "
                  f"연속 {int(trigger_row['recent_failure_streak'])}회 미완료됨.")
            print(f"    당시 모델 예측 위험확률: {trigger_row['risk_prob']:.2f} (판정임계값 {threshold:.2f} 초과)")

            latest_row = df[df["category"] == cat].iloc[-1]
            if not latest_row["risk_flag"]:
                print(f"    ※ 현재(가장 최근 이력) 기준으로는 위험확률 {latest_row['risk_prob']:.2f}로 임계값 밑으로 내려감 — 회복 신호(패턴 소멸)")
    else:
        print("특이 편향 패턴이 감지되지 않았습니다.")


# 데모 1: 운동 카테고리를 반복적으로 회피하는 사용자
demo_user_biased = [
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "공부", "time_slot": "아침", "estimated_minutes": 90, "outcome": "완료"},
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 60, "outcome": "미룸"},
    {"category": "업무", "time_slot": "오후", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 60, "outcome": "실패"},
    {"category": "공부", "time_slot": "아침", "estimated_minutes": 90, "outcome": "완료"},
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 90, "outcome": "미룸"},
    {"category": "업무", "time_slot": "오후", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 60, "outcome": "실패"},
]

# 데모 2: 특별한 편향 패턴 없이 고르게 잘 하는 사용자
demo_user_healthy = [
    {"category": "운동", "time_slot": "아침", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "공부", "time_slot": "아침", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "업무", "time_slot": "오후", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "time_slot": "아침", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "개인일정", "time_slot": "저녁", "estimated_minutes": 30, "outcome": "미룸"},
    {"category": "공부", "time_slot": "아침", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "time_slot": "아침", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "업무", "time_slot": "오후", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "time_slot": "아침", "estimated_minutes": 30, "outcome": "완료"},
]

# 데모 3: 여러 카테고리에서 동시에 편향이 발생하는 사용자 (운동 + 공부 둘 다 회피)
demo_user_multi_bias = [
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "공부", "time_slot": "밤", "estimated_minutes": 120, "outcome": "미룸"},
    {"category": "업무", "time_slot": "오후", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 90, "outcome": "실패"},
    {"category": "공부", "time_slot": "밤", "estimated_minutes": 120, "outcome": "실패"},
    {"category": "업무", "time_slot": "오후", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 60, "outcome": "미룸"},
    {"category": "공부", "time_slot": "밤", "estimated_minutes": 90, "outcome": "미룸"},
    {"category": "업무", "time_slot": "오후", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 90, "outcome": "실패"},
    {"category": "공부", "time_slot": "밤", "estimated_minutes": 120, "outcome": "실패"},
    {"category": "업무", "time_slot": "오후", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 90, "outcome": "실패"},
    {"category": "업무", "time_slot": "오후", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 90, "outcome": "실패"},
]

# 데모 4: 콜드스타트 신규 사용자 (이력 4건뿐 — 상대완료율은 아직 0으로 계산됨을 보여줌)
# 실제 서비스라면 과제 20건 쌓이기 전이라 모델이 아니라 규칙(연속미완료>=2)으로 판정해야 하는 구간.
# 여기서는 "데이터가 적을 때 모델에 넣으면 어떻게 나오는지"를 참고용으로 보여줌.
demo_user_cold_start = [
    {"category": "운동", "time_slot": "아침", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "공부", "time_slot": "아침", "estimated_minutes": 60, "outcome": "미룸"},
    {"category": "운동", "time_slot": "아침", "estimated_minutes": 30, "outcome": "실패"},
    {"category": "업무", "time_slot": "오후", "estimated_minutes": 60, "outcome": "완료"},
]

# 데모 5: 초반엔 운동을 반복 회피하다가 최근엔 꾸준히 완료로 돌아선 "회복" 사용자
# 결정사항.md 1번(패턴 소멸=개입 성공)과 직결 — 편향 경고 이후 실제로 좋아지면 위험확률이 다시 낮아져야 함을 보여줌
demo_user_recovering = [
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 60, "outcome": "미룸"},
    {"category": "공부", "time_slot": "아침", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 90, "outcome": "실패"},
    {"category": "업무", "time_slot": "오후", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "time_slot": "저녁", "estimated_minutes": 60, "outcome": "실패"},
    {"category": "공부", "time_slot": "아침", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "time_slot": "아침", "estimated_minutes": 30, "outcome": "완료"},  # 여기서부터 회복 시작
    {"category": "업무", "time_slot": "오후", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "time_slot": "아침", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "공부", "time_slot": "아침", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "time_slot": "아침", "estimated_minutes": 30, "outcome": "완료"},
]

diagnose(demo_user_biased, "사용자 A (운동 반복 회피 패턴)")
diagnose(demo_user_healthy, "사용자 B (특이 패턴 없음)")
diagnose(demo_user_multi_bias, "사용자 C (운동+공부 동시 회피 패턴)")
diagnose(demo_user_cold_start, "사용자 D (콜드스타트, 이력 4건뿐)")
diagnose(demo_user_recovering, "사용자 E (운동 회피 → 최근 회복 패턴)")