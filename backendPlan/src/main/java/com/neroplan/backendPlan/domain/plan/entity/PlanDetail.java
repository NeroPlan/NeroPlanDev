package com.neroplan.backendPlan.domain.plan.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PlanDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long planDetailId;

    private Double successRate;

    private Timestamp recommendedTime;

    private String priorityReason;

    private Boolean isBiased;

    private String biasedReason;

    // AI 재배열 결과 수행 순서(1부터) - 재배열 대상에서 빠진 계획은 null
    private Integer recommendedOrder;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="plan_id")
    private Plan plan;

    // AI 재배열 결과 반영
    public void updateSchedule(Integer recommendedOrder, Double successRate,
                               Boolean isBiased, String biasedReason) {
        this.recommendedOrder = recommendedOrder;
        this.successRate = successRate;
        this.isBiased = isBiased;
        this.biasedReason = biasedReason;
    }

    public void clearRecommendedOrder() {
        this.recommendedOrder = null;
    }

    // 필요한 경우 update 메서드 추가
    public void updatePlanDetail(Double successRate, Timestamp recommendedTime,
                                 String priorityReason, Boolean isBiased,
                                 String biasedReason) {
        if (successRate != null) this.successRate = successRate;
        if (recommendedTime != null) this.recommendedTime = recommendedTime;
        if (priorityReason != null) this.priorityReason = priorityReason;
        if (isBiased != null) this.isBiased = isBiased;
        if (biasedReason != null) this.biasedReason = biasedReason;
    }
}