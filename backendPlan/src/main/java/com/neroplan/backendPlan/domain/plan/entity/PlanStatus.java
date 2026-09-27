package com.neroplan.backendPlan.domain.plan.entity;

// aiService 인지편향 모델의 outcome(완료/미룸/실패)과 매핑되는 계획 상태
// 기본값은 FAILED이며 사용자가 완료/연기를 누르면 COMPLETED/POSTPONED로 변경
public enum PlanStatus {
    FAILED,     // 실패 (기본값)
    COMPLETED,  // 완료
    POSTPONED   // 연기(미룸)
}
