package com.neroplan.backendPlan.domain.plan.service;

import com.neroplan.backendPlan.domain.category.service.CategoryService;
import com.neroplan.backendPlan.domain.plan.dto.CreatePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanResponseDto;
import com.neroplan.backendPlan.domain.plan.entity.Plan;
import com.neroplan.backendPlan.domain.plan.repository.PlanDetailRepository;
import com.neroplan.backendPlan.domain.plan.repository.PlanRepository;
import com.neroplan.backendPlan.global.ai.AiClient;
import com.neroplan.backendPlan.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 달력 화면용 기능 - 기간 조회와 날짜를 지정한 계획 생성
class PlanServiceDateTest {

    private static final LocalDate TODAY = LocalDate.now();

    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final PlanDetailRepository planDetailRepository = mock(PlanDetailRepository.class);
    private final PlanService planService = new PlanService(
            planRepository,
            planDetailRepository,
            mock(CategoryService.class),
            mock(com.neroplan.backendPlan.domain.category.repository.CategoryRepository.class),
            mock(AiClient.class),
            mock(PlanScheduleService.class),
            mock(ApplicationEventPublisher.class),
            mock(TransactionTemplate.class));

    @Test
    void 기간_조회는_날짜순_같은_날은_생성순으로_정렬한다() {
        Plan later = plan(1L, TODAY.plusDays(1), LocalDateTime.now().minusHours(1));
        Plan first = plan(2L, TODAY, LocalDateTime.now());
        Plan second = plan(3L, TODAY, LocalDateTime.now().plusMinutes(1));
        when(planRepository.findByUserIdAndPlanDateBetween(1L, TODAY, TODAY.plusDays(6)))
                .thenReturn(List.of(later, second, first));
        when(planDetailRepository.findMapByPlanIn(anyList())).thenReturn(Map.of());

        List<GetPlanResponseDto> result = planService.getPlansByDateRange(1L, TODAY, TODAY.plusDays(6));

        assertThat(result).extracting(GetPlanResponseDto::getPlanId).containsExactly(2L, 3L, 1L);
    }

    @Test
    void 시작일이_종료일보다_늦거나_기간이_62일을_넘으면_거절한다() {
        assertThatThrownBy(() -> planService.getPlansByDateRange(1L, TODAY, TODAY.minusDays(1)))
                .isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> planService.getPlansByDateRange(1L, TODAY, TODAY.plusDays(63)))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    void 월간_보기_42일은_조회할_수_있다() {
        when(planRepository.findByUserIdAndPlanDateBetween(any(), any(), any())).thenReturn(List.of());
        when(planDetailRepository.findMapByPlanIn(anyList())).thenReturn(Map.of());

        assertThat(planService.getPlansByDateRange(1L, TODAY, TODAY.plusDays(41))).isEmpty();
    }

    @Test
    void 날짜를_지정하면_그_날짜로_생성하고_없으면_오늘로_생성한다() {
        when(planRepository.save(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(planService.createPlan(1L, request(TODAY.plusDays(3))).getPlanDate()).isEqualTo(TODAY.plusDays(3));
        assertThat(planService.createPlan(1L, request(null)).getPlanDate()).isEqualTo(TODAY);
        verify(planRepository, org.mockito.Mockito.times(2)).save(any(Plan.class));
    }

    @Test
    void 지난_날짜로는_생성할_수_없다() {
        assertThatThrownBy(() -> planService.createPlan(1L, request(TODAY.minusDays(1))))
                .isInstanceOf(GeneralException.class);
    }

    private CreatePlanRequestDto request(LocalDate planDate) {
        return CreatePlanRequestDto.builder()
                .content("테스트")
                .priority(1L)
                .estimatedMinutes(30)
                .planDate(planDate)
                .build();
    }

    private Plan plan(Long id, LocalDate planDate, LocalDateTime createdTime) {
        return Plan.builder()
                .planId(id)
                .userId(1L)
                .content("계획" + id)
                .priority(1L)
                .planDate(planDate)
                .createdTime(createdTime)
                .build();
    }
}
