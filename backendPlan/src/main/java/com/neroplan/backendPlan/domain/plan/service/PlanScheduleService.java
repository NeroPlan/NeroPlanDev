package com.neroplan.backendPlan.domain.plan.service;

import com.neroplan.backendPlan.domain.plan.entity.Plan;
import com.neroplan.backendPlan.domain.plan.entity.PlanDetail;
import com.neroplan.backendPlan.domain.plan.entity.PlanStatus;
import com.neroplan.backendPlan.domain.plan.event.PlanChangedEvent;
import com.neroplan.backendPlan.domain.plan.repository.PlanDetailRepository;
import com.neroplan.backendPlan.domain.plan.repository.PlanRepository;
import com.neroplan.backendPlan.global.ai.AiClient;
import com.neroplan.backendPlan.global.ai.dto.SchedulePlansRequestDto;
import com.neroplan.backendPlan.global.ai.dto.SchedulePlansResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// 계획 내용이 바뀔 때만 aiService로 재배열을 다시 계산해 PlanDetail에 저장
// 변경 트랜잭션 커밋 후 별도 스레드에서 실행되므로 API 응답이 AI 호출을 기다리지 않음
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanScheduleService {

    private static final String OUTCOME_DONE = "완료";
    private static final String OUTCOME_POSTPONED = "미룸";
    private static final String OUTCOME_FAILED = "실패";

    private final PlanRepository planRepository;
    private final PlanDetailRepository planDetailRepository;
    private final AiClient aiClient;

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePlanChanged(PlanChangedEvent event) {
        recalculate(event.userId(), event.planDate());
    }

    private void recalculate(Long userId, LocalDate planDate) {
        LocalDate today = LocalDate.now();
        List<Plan> userPlans = planRepository.findByUserId(userId);

        List<Plan> sameDatePlans = userPlans.stream()
                .filter(plan -> planDate.equals(plan.getPlanDate()))
                .toList();
        // aiService는 category, estimated_minutes가 필수라 둘 다 있는 미완료 계획만 재배열
        List<Plan> pendingPlans = sameDatePlans.stream()
                .filter(plan -> plan.getStatus() == PlanStatus.TODO)
                .filter(plan -> plan.getCategory() != null && plan.getEstimatedMinutes() != null)
                .toList();

        Map<Long, PlanDetail> detailsByPlanId = planDetailRepository.findByPlanIn(sameDatePlans).stream()
                .collect(Collectors.toMap(detail -> detail.getPlan().getPlanId(), Function.identity()));

        if (pendingPlans.isEmpty()) {
            detailsByPlanId.values().forEach(PlanDetail::clearRecommendedOrder);
            return;
        }

        SchedulePlansRequestDto requestDto = new SchedulePlansRequestDto(
                pendingPlans.stream().map(this::toPendingPlan).toList(),
                toTaskHistory(userPlans, today));

        // AI 호출 실패 시 기존 재배열 결과 유지
        Optional<SchedulePlansResponseDto> response = aiClient.schedulePlans(requestDto);
        if (response.isEmpty() || response.get().getPlans() == null) {
            return;
        }

        detailsByPlanId.values().forEach(PlanDetail::clearRecommendedOrder);
        Map<Long, Plan> pendingById = pendingPlans.stream()
                .collect(Collectors.toMap(Plan::getPlanId, Function.identity()));

        int order = 1;
        for (SchedulePlansResponseDto.ScheduledPlan scheduled : response.get().getPlans()) {
            Plan plan = pendingById.get(scheduled.getPlanId());
            if (plan == null) {
                log.warn("aiService가 요청에 없는 planId를 반환: {}", scheduled.getPlanId());
                continue;
            }
            PlanDetail detail = detailsByPlanId.get(plan.getPlanId());
            if (detail == null) {
                detail = planDetailRepository.save(PlanDetail.builder().plan(plan).build());
            }
            // 모델 판정일 때만 risk_prob가 있으므로 그때만 성공률 계산
            Double successRate = scheduled.getRiskProb() != null ? 1 - scheduled.getRiskProb() : null;
            detail.updateSchedule(order++, successRate, scheduled.getRiskFlag(), scheduled.getReason());
        }
    }

    private SchedulePlansRequestDto.PendingPlan toPendingPlan(Plan plan) {
        return new SchedulePlansRequestDto.PendingPlan(
                plan.getPlanId(),
                plan.getCategory().getName(),
                plan.getEstimatedMinutes(),
                plan.getPostponeCount());
    }

    // 결과가 확정된 계획만 이력으로 사용 - 수행일, 생성 시각 순으로 정렬
    private List<SchedulePlansRequestDto.TaskHistory> toTaskHistory(List<Plan> userPlans, LocalDate today) {
        return userPlans.stream()
                .filter(plan -> plan.getCategory() != null && plan.getEstimatedMinutes() != null)
                .filter(plan -> toOutcome(plan, today) != null)
                .sorted(Comparator.comparing(Plan::getPlanDate).thenComparing(Plan::getCreatedTime))
                .map(plan -> new SchedulePlansRequestDto.TaskHistory(
                        plan.getCategory().getName(),
                        plan.getEstimatedMinutes(),
                        toOutcome(plan, today)))
                .toList();
    }

    // 완료 > 연기된 적 있음(postponeCount) > 수행일이 지났는데 미완료 순으로 판정, 아직 진행 중이면 null
    private String toOutcome(Plan plan, LocalDate today) {
        if (plan.getStatus() == PlanStatus.DONE) {
            return OUTCOME_DONE;
        }
        if (plan.getPostponeCount() > 0) {
            return OUTCOME_POSTPONED;
        }
        if (plan.getPlanDate().isBefore(today)) {
            return OUTCOME_FAILED;
        }
        return null;
    }
}
