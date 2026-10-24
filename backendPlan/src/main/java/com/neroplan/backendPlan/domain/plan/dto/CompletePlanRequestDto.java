package com.neroplan.backendPlan.domain.plan.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CompletePlanRequestDto {

    // true: 완료 처리, false: 완료 취소
    @NotNull(message = "완료 여부는 필수입니다.")
    private Boolean completed;
}
