package com.neroplan.backendPlan.domain.plan.dto;

import com.neroplan.backendPlan.domain.plan.entity.PlanStatus;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UpdatePlanRequestDto {

    private String content;
    private Long priority;
    private PlanStatus status;

}
