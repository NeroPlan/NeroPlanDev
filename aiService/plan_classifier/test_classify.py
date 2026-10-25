"""
classify_category() 실제 LLM 호출 테스트.
위치: aiService/plan_classifier/test_classify.py

실행 전 환경변수 GEMINI_API_KEY 설정 필요 (PowerShell: $env:GEMINI_API_KEY = "발급받은키")
실행: python test_classify.py   (어느 폴더에서 실행해도 됨)

계획을 하나씩 순서대로 분류하면서, 새로 생긴 카테고리를 다음 계획의 기존 목록에 넣어
backendPlan이 계획을 하나씩 만들 때와 같은 흐름으로 확인한다.
"""
import os
import sys

AI_SERVICE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.append(AI_SERVICE_DIR)

from plan_classifier.classify_category import classify_category  # noqa: E402
from shared.llm_client import build_llm_client  # noqa: E402

llm = build_llm_client()
print(f"LLM 연결: {llm.is_available()} ({llm.provider_name} / {llm.model_name})")

plans = [
    "저녁에 헬스장 가기",
    "토익 문제집 2챕터 풀기",
    "아침 러닝 5km",
    "팀 회의 자료 만들기",
    "영어 단어 50개 외우기",
    "은행 다녀오기",
    "요가 수업",
    "보고서 마감",
]

categories: list[str] = []  # 처음 쓰는 사용자라고 가정
for content in plans:
    result = classify_category(content, categories, llm)
    if result["category"] is None:
        print(f"[실패] {content} → 미분류 / 경고: {result['warning']}")
        continue
    if result["is_new"]:
        categories.append(result["category"])
    mark = "새 카테고리" if result["is_new"] else "기존 카테고리"
    print(f"{content} → {result['category']} ({mark})")

print(f"\n최종 카테고리 목록: {categories}")