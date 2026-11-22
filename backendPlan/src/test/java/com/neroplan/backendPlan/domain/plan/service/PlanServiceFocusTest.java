package com.neroplan.backendPlan.domain.plan.service;

import com.neroplan.backendPlan.domain.category.repository.CategoryRepository;
import com.neroplan.backendPlan.domain.category.service.CategoryService;
import com.neroplan.backendPlan.domain.plan.dto.FocusPlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanResponseDto;
import com.neroplan.backendPlan.domain.plan.entity.Plan;
import com.neroplan.backendPlan.domain.plan.entity.PlanStatus;
import com.neroplan.backendPlan.domain.plan.event.PlanChangedEvent;
import com.neroplan.backendPlan.domain.plan.repository.PlanDetailRepository;
import com.neroplan.backendPlan.domain.plan.repository.PlanRepository;
import com.neroplan.backendPlan.global.ai.AiClient;
import com.neroplan.backendPlan.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 집중 타이머 기록
class PlanServiceFocusTest {

    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final PlanService planService = new PlanService(
            planRepository,
            mock(PlanDetailRepository.class),
            mock(CategoryService.class),
            mock(CategoryRepository.class),
            mock(AiClient.class),
            mock(PlanScheduleService.class),
            eventPublisher,
            mock(TransactionTemplate.class));

    private Plan plan() {
        return Plan.builder().planId(1L).userId(1L).content("공부").priority(1L)
                .estimatedMinutes(30).planDate(LocalDate.now()).build();
    }

    @Test
    void 집중한_시간은_여러_번_나눠_기록하면_누적된다() {
        Plan plan = plan();
        when(planRepository.findByPlanIdAndUserId(1L, 1L)).thenReturn(Optional.of(plan));

        planService.recordFocus(1L, 1L, new FocusPlanRequestDto(20, false));
        GetPlanResponseDto result = planService.recordFocus(1L, 1L, new FocusPlanRequestDto(15, false));

        assertThat(result.getActualMinutes()).isEqualTo(35);
        assertThat(result.getStatus()).isEqualTo(PlanStatus.TODO);
        verify(eventPublisher, never()).publishEvent(any(PlanChangedEvent.class));
    }

    @Test
    void complete면_기록과_함께_완료하고_재배열_이벤트를_보낸다() {
        Plan plan = plan();
        when(planRepository.findByPlanIdAndUserId(1L, 1L)).thenReturn(Optional.of(plan));

        GetPlanResponseDto result = planService.recordFocus(1L, 1L, new FocusPlanRequestDto(40, true));

        assertThat(result.getActualMinutes()).isEqualTo(40);
        assertThat(result.getStatus()).isEqualTo(PlanStatus.DONE);
        assertThat(result.getCompletedTime()).isNotNull();
        verify(eventPublisher, times(1)).publishEvent(any(PlanChangedEvent.class));
    }

    @Test
    void 이미_완료한_계획도_시간은_더하지만_완료_시각은_바꾸지_않는다() {
        Plan plan = plan();
        plan.complete();
        var completedTime = plan.getCompletedTime();
        when(planRepository.findByPlanIdAndUserId(1L, 1L)).thenReturn(Optional.of(plan));

        GetPlanResponseDto result = planService.recordFocus(1L, 1L, new FocusPlanRequestDto(10, true));

        assertThat(result.getActualMinutes()).isEqualTo(10);
        assertThat(result.getCompletedTime()).isEqualTo(completedTime);
        verify(eventPublisher, never()).publishEvent(any(PlanChangedEvent.class));
    }

    @Test
    void 남의_계획에는_기록할_수_없다() {
        when(planRepository.findByPlanIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> planService.recordFocus(2L, 1L, new FocusPlanRequestDto(10, false)))
                .isInstanceOf(GeneralException.class);
    }
}
