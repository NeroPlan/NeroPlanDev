package com.neroplan.neroplan.domain.plan.dto;

import com.neroplan.neroplan.domain.plan.entity.Plan;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class GetPlanResponseDto {

    private Long planId;
    private String content;
    private LocalDateTime createdTime;
    private Long priority;
    private Long userId;
    private Long categoryId;


    public static GetPlanResponseDto from(Plan plan) {
        return GetPlanResponseDto.builder()
                .planId(plan.getPlanId())
                .content(plan.getContent())
                .createdTime(plan.getCreatedTime())
                .priority(plan.getPriority())
                .userId(plan.getUserId())
                .categoryId(plan.getCategoryId())
                .build();
    }
}
