package com.neroplan.backendPlan.domain.plan.dto;

import com.neroplan.backendPlan.domain.plan.entity.Plan;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@NoArgsConstructor
@Getter
@AllArgsConstructor
public class CreatePlanDetailRequestDto {

    private Double successRate;

    private Timestamp recommendedTime;

    private String priorityReason;

    private Boolean isBiased;

    private String biasedReason;

    private Plan plan;
}
