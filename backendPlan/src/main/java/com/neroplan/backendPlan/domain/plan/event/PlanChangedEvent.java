package com.neroplan.backendPlan.domain.plan.event;

import java.time.LocalDate;

// 계획 생성/수정/완료/연기/삭제로 해당 날짜의 재배열이 다시 필요할 때 발행
public record PlanChangedEvent(Long userId, LocalDate planDate) {
}
