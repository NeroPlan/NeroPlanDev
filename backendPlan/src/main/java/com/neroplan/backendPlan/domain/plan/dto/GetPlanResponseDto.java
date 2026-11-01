package com.neroplan.backendPlan.domain.plan.dto;

import com.neroplan.backendPlan.domain.plan.entity.AnalysisStatus;
import com.neroplan.backendPlan.domain.plan.entity.Plan;
import com.neroplan.backendPlan.domain.plan.entity.PlanDetail;
import com.neroplan.backendPlan.domain.plan.entity.PlanStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
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
    private String categoryName;
    private Integer estimatedMinutes;
    private LocalDate planDate;
    private Integer postponeCount;
    private PlanStatus status;
    private LocalDateTime completedTime;

    // AI 재배열 결과 - 아직 분석 전이면 null
    private Integer recommendedOrder;
    private AnalysisStatus analysisStatus;
    private Integer recordCount;
    private Double successRate;      // 모델 판정일 때만 - 예측 성공률
    private Double completionRate;   // 기록 수집 중일 때만 - 지금까지 실제 완료율
    private String priorityReason;   // 판단 근거 문장
    private Boolean isBiased;
    private String biasedReason;


    public static GetPlanResponseDto from(Plan plan) {
        return from(plan, null);
    }

    public static GetPlanResponseDto from(Plan plan, PlanDetail detail) {
        return GetPlanResponseDto.builder()
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
                .recommendedOrder(detail != null ? detail.getRecommendedOrder() : null)
                .analysisStatus(detail != null ? detail.getAnalysisStatus() : null)
                .recordCount(detail != null ? detail.getRecordCount() : null)
                .successRate(detail != null ? detail.getSuccessRate() : null)
                .completionRate(detail != null ? detail.getCompletionRate() : null)
                .priorityReason(detail != null ? detail.getPriorityReason() : null)
                .isBiased(detail != null ? detail.getIsBiased() : null)
                .biasedReason(detail != null ? detail.getBiasedReason() : null)
                .build();
    }
}
