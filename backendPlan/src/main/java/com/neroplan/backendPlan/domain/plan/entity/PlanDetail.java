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

    // 모델 판정일 때만 - 예측 성공률(0~1)
    private Double successRate;

    // 기록 수집 중일 때만 - 같은 카테고리의 지금까지 실제 완료율(0~1)
    private Double completionRate;

    private Timestamp recommendedTime;

    // AI 판단 근거 문장 - 판정 상태와 상관없이 항상 채워짐
    private String priorityReason;

    private Boolean isBiased;

    private String biasedReason;

    // AI 재배열 결과 수행 순서(1부터) - 재배열 대상에서 빠진 계획은 null
    private Integer recommendedOrder;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AnalysisStatus analysisStatus;

    // 같은 카테고리의 확정된 과거 기록 수
    private Integer recordCount;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="plan_id")
    private Plan plan;

    // AI 재배열 결과 반영
    public void updateSchedule(Integer recommendedOrder, AnalysisStatus analysisStatus, Integer recordCount,
                               Double successRate, Double completionRate, String priorityReason,
                               Boolean isBiased, String biasedReason) {
        this.recommendedOrder = recommendedOrder;
        this.analysisStatus = analysisStatus;
        this.recordCount = recordCount;
        this.successRate = successRate;
        this.completionRate = completionRate;
        this.priorityReason = priorityReason;
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