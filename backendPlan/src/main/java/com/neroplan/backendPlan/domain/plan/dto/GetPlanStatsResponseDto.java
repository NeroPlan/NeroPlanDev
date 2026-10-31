package com.neroplan.backendPlan.domain.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// 분석 페이지 통계 - 결과가 확정된 계획(PlanOutcome)만 집계
// 성공률은 계획한 날 완료한 비율(0~1)이고, 기록이 MIN_RECORDS_FOR_RATE건 미만이면 null
@Getter
@AllArgsConstructor
public class GetPlanStatsResponseDto {

    private Integer recordCount;
    private Double successRate;

    // 기록 많은 순 - 미분류 계획은 categoryName이 null
    private List<CategoryStat> categories;

    // 예상 소요시간 구간별 - aiService의 소요시간 구간(짧음/보통/김)과 같은 기준
    private List<RateStat> durationStats;

    // 하루에 세운 계획 수 구간별 - 지난 날짜만 집계
    private List<RateStat> dailyLoadStats;

    private PostponeStat postponeStat;

    @Getter
    @AllArgsConstructor
    public static class CategoryStat {
        private String categoryName;
        private Integer recordCount;
        private Integer doneCount;
        private Integer postponedCount;
        private Integer failedCount;
        private Double successRate;

        // aiService가 쓰는 기록 수 (소요시간이 있는 기록만) - 10건부터 모델이 성공률을 예측
        private Integer aiRecordCount;
    }

    @Getter
    @AllArgsConstructor
    public static class RateStat {
        private String label;
        private Integer recordCount;
        private Double successRate;
    }

    @Getter
    @AllArgsConstructor
    public static class PostponeStat {
        private Integer postponedCount;          // 한 번이라도 미룬 계획 수
        private Double postponedCompletionRate;  // 미룬 계획 중 결국 완료한 비율
        private Double onTimeCompletionRate;     // 미루지 않은 계획 중 완료한 비율 (비교용)
    }
}
