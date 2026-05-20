package com.neroplan.neroplan.domain.plan.dto;

import com.neroplan.neroplan.domain.plan.entity.Plan;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Builder;

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


    public static CreatePlanResponseDto from(Plan plan) {
        return CreatePlanResponseDto.builder()
                .planId(plan.getPlanId())
                .content(plan.getContent())
                .createdTime(plan.getCreatedTime())
                .priority(plan.getPriority())
                .userId(plan.getUserId())
                .categoryId(plan.getCategoryId())
                .build();
    }
}
