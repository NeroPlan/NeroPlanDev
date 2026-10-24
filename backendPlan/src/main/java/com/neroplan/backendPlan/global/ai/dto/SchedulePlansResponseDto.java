package com.neroplan.backendPlan.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// aiService success_rate_scheduler.schedule_plans()의 반환 형식 - plans는 위험도 높은 순(추천 수행 순서)
@Getter
@NoArgsConstructor
public class SchedulePlansResponseDto {

    private List<ScheduledPlan> plans;

    @Getter
    @NoArgsConstructor
    public static class ScheduledPlan {
        @JsonProperty("plan_id")
        private Long planId;

        // "기록 없음" / "기록 수집 중(규칙판정)" / "판정됨(모델)"
        private String status;

        @JsonProperty("risk_score")
        private Double riskScore;

        @JsonProperty("risk_flag")
        private Boolean riskFlag;

        // 모델 판정일 때만 존재
        @JsonProperty("risk_prob")
        private Double riskProb;

        // 모델 판정일 때만 존재 - 위험 근거 문장
        private String reason;
    }
}
