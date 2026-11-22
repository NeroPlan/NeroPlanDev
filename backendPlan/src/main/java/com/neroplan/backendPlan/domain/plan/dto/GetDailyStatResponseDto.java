package com.neroplan.backendPlan.domain.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

// 완료 잔디 한 칸 - 그 날짜(planDate)에 잡힌 계획 수와 완료한 수, 계획이 없는 날은 응답에 없음
@Getter
@AllArgsConstructor
public class GetDailyStatResponseDto {
    private LocalDate date;
    private Integer totalCount;
    private Integer doneCount;
}
