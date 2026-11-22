package com.neroplan.backendPlan.domain.plan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 집중 타이머 한 번의 기록
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FocusPlanRequestDto {

    // 이번에 집중한 시간(분) - 기존 기록에 더함
    @NotNull(message = "집중한 시간은 필수입니다.")
    @Min(value = 1, message = "집중한 시간은 1분 이상이어야 합니다.")
    @Max(value = 720, message = "집중한 시간은 한 번에 12시간을 넘을 수 없습니다.")
    private Integer minutes;

    // true면 기록과 함께 완료 처리
    private boolean complete;
}
