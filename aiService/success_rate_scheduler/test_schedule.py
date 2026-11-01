"""
schedule_plans() 동작 확인용 테스트 스크립트.
위치: aiService/success_rate_scheduler/test_schedule.py (planning_fallacy_detector/와 같은 레벨)

실행: python3 test_schedule.py
sys.path를 이 파일 위치 기준으로 잡기 때문에 어느 폴더에서 실행해도 동작함.
"""
import os
import sys

THIS_DIR = os.path.dirname(os.path.abspath(__file__))
DETECTOR_DIR = os.path.join(os.path.dirname(THIS_DIR), "planning_fallacy_detector")
sys.path.append(THIS_DIR)
sys.path.append(DETECTOR_DIR)

from bias_diagnosis import load_model  # noqa: E402
from demo_scenarios import demo_user_multi_bias  # noqa: E402
from schedule_plans import schedule_plans  # noqa: E402

_, model, threshold = load_model()

# 과거 이력: 운동·공부는 10건 이상(모델 판정), 업무는 10건 미만(기록 수집 중).
# 실제 서비스에서는 DB에서 조회한 이 사용자의 확정된 Plan 이력이 들어감.
task_history = demo_user_multi_bias

# 오늘 입력한 계획 10개(재배열 전).
# 개인일정은 이력에 없는 카테고리라 "기록 없음" 경우까지 확인.
# estimated_minutes는 실제로는 LLM 추정값, postponed_count는 백엔드가 주는 계획별 미룸 횟수.
# 같은 카테고리 안에서 소요시간·미룸 횟수를 다르게 넣어 동점 처리를 확인.
pending_plans = [
    {"category": "운동", "title": "헬스장 가기", "estimated_minutes": 90, "postponed_count": 0},
    {"category": "공부", "title": "토익 문제집", "estimated_minutes": 60, "postponed_count": 0},
    {"category": "업무", "title": "보고서 작성", "estimated_minutes": 120, "postponed_count": 1},
    {"category": "개인일정", "title": "은행 다녀오기", "estimated_minutes": 30, "postponed_count": 0},
    {"category": "운동", "title": "저녁 러닝", "estimated_minutes": 30, "postponed_count": 2},
    {"category": "공부", "title": "스터디 복습", "estimated_minutes": 60, "postponed_count": 3},
    {"category": "업무", "title": "메일 회신", "estimated_minutes": 20, "postponed_count": 0},
    {"category": "운동", "title": "요가 수업", "estimated_minutes": 60, "postponed_count": 0},
    {"category": "공부", "title": "강의 수강", "estimated_minutes": 120, "postponed_count": 0},
    {"category": "개인일정", "title": "병원 예약", "estimated_minutes": 30, "postponed_count": 1},
]

print("=" * 60)
print(f"모델 판정임계값 = {threshold:.3f}")
print("=" * 60)

print("\n--- 입력 순서(재배열 전) ---")
for i, p in enumerate(pending_plans, 1):
    print(f"{i}. [{p['category']}] {p['title']}  ({p['estimated_minutes']}분, 이 계획 미룸 {p['postponed_count']}회)")

# status: "판정됨(모델)" = 카테고리 기록 10건 이상, 모델이 risk_prob 계산
#         "기록 수집 중(규칙판정)" = 10건 미만, 연속 미완료 규칙으로 순서만 정함(risk 0.0/1.0)
#         "기록 없음" = 이력에 그 카테고리가 없음
# 정렬: risk 높은 순 → 같으면 이 계획 미룸 횟수 많은 순 → 그래도 같으면 입력 순서
print("\n--- 재배열 후 (hard_first) ---")
for i, r in enumerate(schedule_plans(pending_plans, task_history, model, threshold), 1):
    risk_display = r.get("risk_prob", r["risk_score"])
    rate_display = f", 완료율={r['completion_rate']}" if "completion_rate" in r else ""
    print(f"{i}. [{r['category']}] {r['title']}  ({r['estimated_minutes']}분, 이 계획 미룸 {r['postponed_count']}회)  "
          f"status={r['status']}, risk={risk_display}, risk_flag={r['risk_flag']}{rate_display}")
    print(f"     └ 근거: {r['reason']}")