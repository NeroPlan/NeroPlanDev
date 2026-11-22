package com.neroplan.backendPlan.domain.plan.service;

import com.neroplan.backendPlan.domain.category.entity.Category;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanStatsResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanSummaryResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanStatsResponseDto.CategoryStat;
import com.neroplan.backendPlan.domain.plan.entity.Plan;
import com.neroplan.backendPlan.domain.plan.entity.PlanStatus;
import com.neroplan.backendPlan.domain.plan.repository.PlanRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanStatsServiceTest {

    private static final LocalDate TODAY = LocalDate.now();

    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final PlanStatsService planStatsService = new PlanStatsService(planRepository);

    private final Category study = Category.builder().categoryId(1L).userId(1L).name("공부").build();
    private final Category workout = Category.builder().categoryId(2L).userId(1L).name("운동").build();

    @Test
    void 카테고리별로_완료_미룸_실패를_집계하고_기록_많은_순으로_정렬한다() {
        List<Plan> plans = new ArrayList<>();
        // 공부: 완료 2, 미룸 1, 실패 1 (어제)
        plans.add(plan(study, 30, TODAY.minusDays(1), PlanStatus.DONE, 0));
        plans.add(plan(study, 30, TODAY.minusDays(1), PlanStatus.DONE, 0));
        plans.add(plan(study, 90, TODAY.minusDays(1), PlanStatus.DONE, 1));
        plans.add(plan(study, 90, TODAY.minusDays(1), PlanStatus.TODO, 0));
        // 운동: 완료 1
        plans.add(plan(workout, 60, TODAY.minusDays(2), PlanStatus.DONE, 0));
        // 오늘 진행 중인 계획은 집계하지 않음
        plans.add(plan(study, 30, TODAY, PlanStatus.TODO, 0));
        when(planRepository.findByUserId(1L)).thenReturn(plans);

        GetPlanStatsResponseDto stats = planStatsService.getStats(1L, null);

        assertThat(stats.getRecordCount()).isEqualTo(5);
        assertThat(stats.getSuccessRate()).isEqualTo(0.6);

        CategoryStat first = stats.getCategories().get(0);
        assertThat(first.getCategoryName()).isEqualTo("공부");
        assertThat(first.getDoneCount()).isEqualTo(2);
        assertThat(first.getPostponedCount()).isEqualTo(1);
        assertThat(first.getFailedCount()).isEqualTo(1);
        assertThat(first.getSuccessRate()).isEqualTo(0.5);
        // 기록 3건 미만이면 성공률을 보여주지 않음
        assertThat(stats.getCategories().get(1).getSuccessRate()).isNull();
    }

    @Test
    void 소요시간과_하루_계획량_구간별로_성공률을_집계한다() {
        List<Plan> plans = new ArrayList<>();
        // 어제: 계획 5개 중 1개만 완료
        plans.add(plan(study, 90, TODAY.minusDays(1), PlanStatus.DONE, 0));
        for (int i = 0; i < 4; i++) {
            plans.add(plan(study, 90, TODAY.minusDays(1), PlanStatus.TODO, 0));
        }
        // 그제: 계획 3개 모두 완료
        for (int i = 0; i < 3; i++) {
            plans.add(plan(workout, 30, TODAY.minusDays(2), PlanStatus.DONE, 0));
        }
        when(planRepository.findByUserId(1L)).thenReturn(plans);

        GetPlanStatsResponseDto stats = planStatsService.getStats(1L, null);

        assertThat(stats.getDurationStats()).extracting("label", "recordCount", "successRate")
                .containsExactly(
                        tuple("30분 이하", 3, 1.0),
                        tuple("31~60분", 0, null),
                        tuple("60분 초과", 5, 0.2));
        assertThat(stats.getDailyLoadStats()).extracting("label", "recordCount", "successRate")
                .containsExactly(
                        tuple("1~2개", 0, null),
                        tuple("3~4개", 3, 1.0),
                        tuple("5개 이상", 5, 0.2));
    }

    @Test
    void 미룬_계획과_미루지_않은_계획의_완료율을_비교한다() {
        List<Plan> plans = new ArrayList<>();
        // 미룬 계획 4개 중 1개 완료
        plans.add(plan(study, 30, TODAY.minusDays(1), PlanStatus.DONE, 1));
        for (int i = 0; i < 3; i++) {
            plans.add(plan(study, 30, TODAY.minusDays(1), PlanStatus.TODO, 2));
        }
        // 미루지 않은 계획 3개 모두 완료
        for (int i = 0; i < 3; i++) {
            plans.add(plan(study, 30, TODAY.minusDays(1), PlanStatus.DONE, 0));
        }
        when(planRepository.findByUserId(1L)).thenReturn(plans);

        GetPlanStatsResponseDto.PostponeStat postponeStat = planStatsService.getStats(1L, null).getPostponeStat();

        assertThat(postponeStat.getPostponedCount()).isEqualTo(4);
        assertThat(postponeStat.getPostponedCompletionRate()).isEqualTo(0.25);
        assertThat(postponeStat.getOnTimeCompletionRate()).isEqualTo(1.0);
    }

    @Test
    void 기간을_주면_최근_N일_수행일의_계획만_집계한다() {
        List<Plan> plans = new ArrayList<>();
        plans.add(plan(study, 30, TODAY.minusDays(1), PlanStatus.DONE, 0));   // 최근 7일
        plans.add(plan(study, 30, TODAY.minusDays(6), PlanStatus.TODO, 0));   // 최근 7일 (경계)
        plans.add(plan(study, 30, TODAY.minusDays(7), PlanStatus.DONE, 0));   // 7일 밖
        plans.add(plan(study, 30, TODAY.minusDays(40), PlanStatus.DONE, 0));  // 30일 밖
        when(planRepository.findByUserId(1L)).thenReturn(plans);

        assertThat(planStatsService.getStats(1L, 7).getRecordCount()).isEqualTo(2);
        assertThat(planStatsService.getStats(1L, 30).getRecordCount()).isEqualTo(3);
        assertThat(planStatsService.getStats(1L, null).getRecordCount()).isEqualTo(4);
    }

    @Test
    void 기간이_1에서_365일_밖이면_거절한다() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> planStatsService.getStats(1L, 0))
                .isInstanceOf(com.neroplan.backendPlan.global.apiPayload.exception.GeneralException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> planStatsService.getStats(1L, 366))
                .isInstanceOf(com.neroplan.backendPlan.global.apiPayload.exception.GeneralException.class);
    }

    @Test
    void 날짜별_잔디는_계획_수와_완료_수를_날짜순으로_준다() {
        List<Plan> plans = List.of(
                plan(study, 30, TODAY, PlanStatus.TODO, 0),
                plan(study, 30, TODAY.minusDays(2), PlanStatus.DONE, 0),
                plan(study, 30, TODAY.minusDays(2), PlanStatus.TODO, 0));
        when(planRepository.findByUserIdAndPlanDateBetween(1L, TODAY.minusDays(83), TODAY)).thenReturn(plans);

        assertThat(planStatsService.getDailyStats(1L, TODAY.minusDays(83), TODAY))
                .extracting("date", "totalCount", "doneCount")
                .containsExactly(tuple(TODAY.minusDays(2), 2, 1), tuple(TODAY, 1, 0));
    }

    @Test
    void 연속_달성은_완료한_날이_이어진_일수이고_오늘_완료가_없으면_어제부터_센다() {
        List<Plan> plans = new ArrayList<>();
        plans.add(done(TODAY.minusDays(1)));
        plans.add(done(TODAY.minusDays(2)));
        plans.add(done(TODAY.minusDays(2)));
        plans.add(done(TODAY.minusDays(4))); // 3일 전이 비어서 끊김
        when(planRepository.findByUserId(1L)).thenReturn(plans);
        assertThat(planStatsService.getSummary(1L).getStreakDays()).isEqualTo(2);
        assertThat(planStatsService.getSummary(1L).getTotalDoneCount()).isEqualTo(4);

        plans.add(done(TODAY));
        assertThat(planStatsService.getSummary(1L).getStreakDays()).isEqualTo(3);
    }

    @Test
    void 이번_주는_일요일부터_토요일까지_수행일_기준으로_센다() {
        LocalDate sunday = TODAY.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
        List<Plan> plans = List.of(
                plan(study, 30, sunday, PlanStatus.DONE, 0),
                plan(study, 30, sunday.plusDays(6), PlanStatus.TODO, 0),
                plan(study, 30, sunday.minusDays(1), PlanStatus.DONE, 0)); // 지난주 토요일
        when(planRepository.findByUserId(1L)).thenReturn(plans);

        GetPlanSummaryResponseDto summary = planStatsService.getSummary(1L);
        assertThat(summary.getWeekTotalCount()).isEqualTo(2);
        assertThat(summary.getWeekDoneCount()).isEqualTo(1);
    }

    private Plan done(LocalDate completedDate) {
        return Plan.builder()
                .userId(1L).content("계획").priority(1L)
                .planDate(completedDate).status(PlanStatus.DONE)
                .completedTime(completedDate.atTime(12, 0))
                .build();
    }

    private Plan plan(Category category, int minutes, LocalDate planDate, PlanStatus status, int postponeCount) {
        return Plan.builder()
                .userId(1L)
                .content("계획")
                .priority(1L)
                .category(category)
                .estimatedMinutes(minutes)
                .planDate(planDate)
                .status(status)
                .postponeCount(postponeCount)
                .build();
    }
}
