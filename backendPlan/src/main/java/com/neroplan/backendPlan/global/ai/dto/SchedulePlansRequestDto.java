package com.neroplan.backendPlan.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// aiService POST /api/ai/analyze 요청 - success_rate_scheduler.schedule_plans()의 입력 형식
@Getter
@AllArgsConstructor
public class SchedulePlansRequestDto {

    @JsonProperty("pending_plans")
    private List<PendingPlan> pendingPlans;

    // 확정된 과거 이력 - 오래된 것부터 시간순
    @JsonProperty("task_history")
    private List<TaskHistory> taskHistory;

    @Getter
    @AllArgsConstructor
    public static class PendingPlan {
        @JsonProperty("plan_id")
        private Long planId;

        private String category;

        @JsonProperty("estimated_minutes")
        private Integer estimatedMinutes;

        @JsonProperty("postponed_count")
        private Integer postponedCount;
    }

    @Getter
    @AllArgsConstructor
    public static class TaskHistory {
        private String category;

        @JsonProperty("estimated_minutes")
        private Integer estimatedMinutes;

        // "완료" / "미룸" / "실패"
        private String outcome;
    }
}
