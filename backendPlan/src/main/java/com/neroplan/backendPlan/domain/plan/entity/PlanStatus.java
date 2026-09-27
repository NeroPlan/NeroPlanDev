package com.neroplan.backendPlan.domain.plan.entity;

// aiService 인지편향 모델의 outcome(완료/미룸/실패)과 매핑되는 계획 상태
public enum PlanStatus {
    PENDING,    // 진행 전
    COMPLETED,  // 완료
    POSTPONED,  // 연기(미룸)
    FAILED      // 실패
}
