package com.neroplan.backendPlan.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// aiService POST /api/ai/analyze 응답 - plans는 success_rate_scheduler.schedule_plans() 반환값(위험도 높은 순, 추천 수행 순서)
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

        // 같은 카테고리 과거 기록 수 - 10건 이상부터 모델 판정
        @JsonProperty("record_count")
        private Integer recordCount;

        // 기록 없음일 때는 없음
        @JsonProperty("recent_failure_streak")
        private Integer recentFailureStreak;

        // 모델 판정일 때만 존재 - "짧음" / "보통" / "김"
        @JsonProperty("duration_bin")
        private String durationBin;

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
