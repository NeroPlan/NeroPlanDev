package com.neroplan.backendPlan.domain.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 마이페이지 요약 카드
@Getter
@AllArgsConstructor
public class GetPlanSummaryResponseDto {
    // 계획을 하나 이상 완료한 날이 이어진 일수 - 오늘 아직 완료한 게 없으면 어제까지로 셈
    private Integer streakDays;
    // 이번 주(일~토, 계획 페이지 달력과 같은 기준)에 잡힌 계획 수와 그중 완료한 수
    private Integer weekTotalCount;
    private Integer weekDoneCount;
    // 지금까지 완료한 계획 수
    private Integer totalDoneCount;
}
