package com.neroplan.backendPlan.domain.plan.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PostponePlanRequestDto {

    // 연기할 날짜 - 없으면 기존 수행일 다음 날로 연기
    private LocalDate toDate;
}
