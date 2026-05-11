package com.neroplan.neroplan.domain.plan.entity;

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

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="plan_id")
    private Plan plan;

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