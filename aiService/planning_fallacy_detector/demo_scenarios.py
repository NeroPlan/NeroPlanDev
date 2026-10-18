"""
데모용 고정 사용자 이력 시나리오 5종 (각 항목: category, estimated_minutes, outcome, 오래된 것부터 시간순).
demo_bias_detection_v2.py와 success_rate_scheduler/test_schedule.py가 가져다 씀.
모델 판정은 카테고리 기록 10건 이상부터 켜지므로, 보여주려는 카테고리는 10건 이상으로 구성함.
"""

# 데모 1: 운동 카테고리를 반복적으로 회피하는 사용자 (운동 10건 이상)
demo_user_biased = [
    {"category": "운동", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "공부", "estimated_minutes": 90, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 60, "outcome": "미룸"},
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 60, "outcome": "실패"},
    {"category": "공부", "estimated_minutes": 90, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 90, "outcome": "미룸"},
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 60, "outcome": "실패"},
    {"category": "공부", "estimated_minutes": 90, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 90, "outcome": "미룸"},
    {"category": "공부", "estimated_minutes": 90, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 60, "outcome": "실패"},
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 60, "outcome": "실패"},
    {"category": "운동", "estimated_minutes": 60, "outcome": "미룸"},
    {"category": "운동", "estimated_minutes": 60, "outcome": "실패"},
]

# 데모 2: 특별한 편향 패턴 없이 고르게 잘 하는 사용자 (운동 10건 이상, 모델 판정에서도 위험 없음)
demo_user_healthy = [
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "공부", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "개인일정", "estimated_minutes": 30, "outcome": "미룸"},
    {"category": "공부", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "공부", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "공부", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
]

# 데모 3: 여러 카테고리에서 동시에 편향이 발생하는 사용자 (운동·공부 둘 다 회피, 둘 다 10건 이상)
demo_user_multi_bias = [
    {"category": "운동", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "공부", "estimated_minutes": 120, "outcome": "미룸"},
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 90, "outcome": "실패"},
    {"category": "공부", "estimated_minutes": 120, "outcome": "실패"},
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 60, "outcome": "미룸"},
    {"category": "공부", "estimated_minutes": 90, "outcome": "미룸"},
    {"category": "업무", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 90, "outcome": "실패"},
    {"category": "공부", "estimated_minutes": 120, "outcome": "실패"},
    {"category": "업무", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 90, "outcome": "실패"},
    {"category": "업무", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 90, "outcome": "실패"},
    {"category": "공부", "estimated_minutes": 120, "outcome": "미룸"},
    {"category": "운동", "estimated_minutes": 60, "outcome": "미룸"},
    {"category": "공부", "estimated_minutes": 90, "outcome": "실패"},
    {"category": "운동", "estimated_minutes": 90, "outcome": "실패"},
    {"category": "공부", "estimated_minutes": 120, "outcome": "미룸"},
    {"category": "운동", "estimated_minutes": 60, "outcome": "미룸"},
    {"category": "공부", "estimated_minutes": 90, "outcome": "실패"},
    {"category": "공부", "estimated_minutes": 120, "outcome": "미룸"},
    {"category": "운동", "estimated_minutes": 90, "outcome": "실패"},
    {"category": "공부", "estimated_minutes": 90, "outcome": "실패"},
]

# 데모 4: 신규 사용자 (이력 4건뿐 — 모든 카테고리가 '기록 수집 중')
demo_user_cold_start = [
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "공부", "estimated_minutes": 60, "outcome": "미룸"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "실패"},
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
]

# 데모 5: 초반엔 운동을 반복 회피하다가 최근엔 꾸준히 완료로 돌아선 "회복" 사용자 (운동 10건 이상)
demo_user_recovering = [
    {"category": "운동", "estimated_minutes": 60, "outcome": "미룸"},
    {"category": "공부", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 90, "outcome": "실패"},
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 60, "outcome": "실패"},
    {"category": "공부", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},  # 여기서부터 회복 시작
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "공부", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "공부", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "업무", "estimated_minutes": 60, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
    {"category": "운동", "estimated_minutes": 30, "outcome": "완료"},
]

SCENARIOS = {
    "biased": demo_user_biased,
    "healthy": demo_user_healthy,
    "multi": demo_user_multi_bias,
    "cold": demo_user_cold_start,
    "recovering": demo_user_recovering,
}

SCENARIO_LABELS = {
    "biased": "사용자 A (운동 반복 회피 패턴)",
    "healthy": "사용자 B (특이 패턴 없음)",
    "multi": "사용자 C (운동+공부 동시 회피 패턴)",
    "cold": "사용자 D (콜드스타트, 이력 4건뿐)",
    "recovering": "사용자 E (운동 회피 → 최근 회복 패턴)",
}