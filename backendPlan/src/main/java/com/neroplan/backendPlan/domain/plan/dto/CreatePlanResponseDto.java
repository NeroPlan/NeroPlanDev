package com.neroplan.backendPlan.domain.plan.dto;

import com.neroplan.backendPlan.domain.plan.entity.Plan;
import com.neroplan.backendPlan.domain.plan.entity.PlanStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CreatePlanResponseDto {

    private Long planId;
    private String content;
    private Long priority;
    private LocalDateTime createdTime;
    private Long userId;
    private Long categoryId;
    private String categoryName;
    private Integer estimatedMinutes;
    private LocalDate planDate;
    private Integer postponeCount;
    private PlanStatus status;
    private LocalDateTime completedTime;


    public static CreatePlanResponseDto from(Plan plan) {
        return CreatePlanResponseDto.builder()
                .planId(plan.getPlanId())
                .content(plan.getContent())
                .createdTime(plan.getCreatedTime())
                .priority(plan.getPriority())
                .userId(plan.getUserId())
                .categoryId(plan.getCategory() != null ? plan.getCategory().getCategoryId() : null)
                .categoryName(plan.getCategory() != null ? plan.getCategory().getName() : null)
                .estimatedMinutes(plan.getEstimatedMinutes())
                .planDate(plan.getPlanDate())
                .postponeCount(plan.getPostponeCount())
                .status(plan.getStatus())
                .completedTime(plan.getCompletedTime())
                .build();
    }
}
