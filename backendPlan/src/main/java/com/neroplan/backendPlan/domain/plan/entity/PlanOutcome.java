package com.neroplan.backendPlan.domain.plan.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

// 결과가 확정된 계획의 판정 - aiService 이력(task_history)과 분석 페이지 통계가 같은 기준을 사용
// 계획한 날 끝내지 못했으면 나중에 완료했어도 미룸으로 판정
@Getter
@RequiredArgsConstructor
public enum PlanOutcome {
    DONE("완료"),
    POSTPONED("미룸"),
    FAILED("실패");

    // aiService에 보내는 값
    private final String label;

    // 아직 진행 중(오늘 이후의 미완료 계획)이면 null
    public static PlanOutcome of(Plan plan, LocalDate today) {
        boolean postponed = plan.getPostponeCount() > 0;
        if (plan.getStatus() == PlanStatus.DONE) {
            return postponed ? POSTPONED : DONE;
        }
        if (plan.getPlanDate().isBefore(today)) {
            return postponed ? POSTPONED : FAILED;
        }
        return null;
    }
}
