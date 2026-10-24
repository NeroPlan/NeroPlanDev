package com.neroplan.backendPlan.domain.plan.dto;

import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UpdatePlanRequestDto {

    private String content;
    private Long priority;
    @Positive(message = "예상 소요 시간은 1분 이상이어야 합니다.")
    private Integer estimatedMinutes;

}
