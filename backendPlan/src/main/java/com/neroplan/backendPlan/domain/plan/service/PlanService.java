package com.neroplan.backendPlan.domain.plan.service;

import com.neroplan.backendPlan.domain.category.service.CategoryService;
import com.neroplan.backendPlan.domain.plan.dto.CompletePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.PostponePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.UpdatePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.entity.Plan;
import com.neroplan.backendPlan.domain.plan.entity.PlanDetail;
import com.neroplan.backendPlan.domain.plan.entity.PlanStatus;
import com.neroplan.backendPlan.domain.plan.event.PlanChangedEvent;
import com.neroplan.backendPlan.domain.plan.repository.PlanDetailRepository;
import com.neroplan.backendPlan.domain.plan.repository.PlanRepository;
import com.neroplan.backendPlan.global.ai.AiClient;
import com.neroplan.backendPlan.global.apiPayload.code.status.ErrorStatus;
import com.neroplan.backendPlan.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neroplan.backendPlan.domain.plan.dto.CreatePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.CreatePlanResponseDto;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanDetailRepository planDetailRepository;
    private final CategoryService categoryService;
    private final AiClient aiClient;
    private final PlanScheduleService planScheduleService;
    private final ApplicationEventPublisher eventPublisher;

    // 1. 플랜 생성
    @Transactional
    public CreatePlanResponseDto createPlan(Long userId, CreatePlanRequestDto requestDto) {
        if (userId == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        Plan plan = Plan.builder()
                .userId(userId)
                .content(requestDto.getContent())
                .priority(requestDto.getPriority())
                .estimatedMinutes(requestDto.getEstimatedMinutes())
                .build();

        // 생성 시에는 aiService를 호출하지 않음 - 카테고리 분류와 재배열은 분석하기(analyzeTodayPlans)에서 수행
        Plan savedPlan = planRepository.save(plan);

        return CreatePlanResponseDto.from(savedPlan);
    }

    // 2. 플랜 id 별 단건 조회
    @Transactional(readOnly = true)
    public GetPlanResponseDto getPlan(Long userId, Long planId) {
        Plan plan = planRepository.findByPlanIdAndUserId(planId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._PLAN_NOT_FOUND));
        return GetPlanResponseDto.from(plan);
    }

    // 3. 유저별 플랜 전체 조회
    @Transactional(readOnly = true)
    public List<GetPlanResponseDto> getPlansByUserId(Long userId) {
        return planRepository.findByUserId(userId).stream()
                .map(plan -> GetPlanResponseDto.from(plan))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GetPlanResponseDto> getTodayPlansByUserId(Long userId){
        List<Plan> plans = planRepository.findByUserIdAndPlanDate(userId, LocalDate.now());
        Map<Long, PlanDetail> detailsByPlanId = planDetailRepository.findByPlanIn(plans).stream()
                .collect(Collectors.toMap(detail -> detail.getPlan().getPlanId(), Function.identity()));

        // AI 추천 순서대로 정렬, 순서가 없는 계획(분석 전/완료)은 뒤로
        return plans.stream()
                .map(plan -> GetPlanResponseDto.from(plan, detailsByPlanId.get(plan.getPlanId())))
                .sorted(Comparator.comparing(GetPlanResponseDto::getRecommendedOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    // 8. 오늘 계획 분석 - 미리 계산된 결과가 없거나 오래됐을 수 있으므로 요청 시 다시 계산
    @Transactional
    public List<GetPlanResponseDto> analyzeTodayPlans(Long userId) {
        if (userId == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        LocalDate today = LocalDate.now();

        // 생성 시에는 분류하지 않으므로 카테고리가 없는 계획을 여기서 분류
        // 지난 계획도 AI 이력과 분석 페이지 통계에 들어가야 하므로 날짜·상태와 관계없이 모두 분류
        // 먼저 만든 계획부터 순서대로 분류해야 새로 생긴 카테고리를 다음 계획이 재사용함
        planRepository.findByUserId(userId).stream()
                .filter(plan -> plan.getCategory() == null)
                .sorted(Comparator.comparing(Plan::getPlanDate).thenComparing(Plan::getCreatedTime))
                .forEach(plan -> aiClient.classifyCategory(plan.getContent(), categoryService.getCategoryNames(userId))
                        .map(name -> categoryService.findOrCreate(userId, name))
                        .ifPresent(plan::assignCategory));

        if (!planScheduleService.recalculate(userId, today)) {
            throw new GeneralException(ErrorStatus._AI_ANALYSIS_FAILED);
        }
        return getTodayPlansByUserId(userId);
    }

    // 4. 플랜 수정
    @Transactional
    public GetPlanResponseDto updatePlan(Long userId, Long planId, UpdatePlanRequestDto requestDto) {
        Plan plan = planRepository.findByPlanIdAndUserId(planId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._PLAN_NOT_FOUND));
        plan.updatePlan(requestDto.getContent(), requestDto.getPriority(), requestDto.getEstimatedMinutes());
        eventPublisher.publishEvent(new PlanChangedEvent(userId, plan.getPlanDate()));
        return GetPlanResponseDto.from(plan);
    }

    // 6. 플랜 완료 처리 / 완료 취소
    @Transactional
    public GetPlanResponseDto completePlan(Long userId, Long planId, CompletePlanRequestDto requestDto) {
        Plan plan = planRepository.findByPlanIdAndUserId(planId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._PLAN_NOT_FOUND));
        if (requestDto.getCompleted()) {
            plan.complete();
        } else {
            plan.cancelComplete();
        }
        eventPublisher.publishEvent(new PlanChangedEvent(userId, plan.getPlanDate()));
        return GetPlanResponseDto.from(plan);
    }

    // 7. 플랜 연기 - 수행일(planDate)만 변경하고 연기 횟수 증가
    @Transactional
    public GetPlanResponseDto postponePlan(Long userId, Long planId, PostponePlanRequestDto requestDto) {
        Plan plan = planRepository.findByPlanIdAndUserId(planId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._PLAN_NOT_FOUND));
        if (plan.getStatus() == PlanStatus.DONE) {
            throw new GeneralException(ErrorStatus._ALREADY_COMPLETED_PLAN);
        }

        LocalDate toDate = requestDto.getToDate() != null
                ? requestDto.getToDate()
                : plan.getPlanDate().plusDays(1);
        if (!toDate.isAfter(plan.getPlanDate()) || toDate.isBefore(LocalDate.now())) {
            throw new GeneralException(ErrorStatus._INVALID_POSTPONE_DATE);
        }

        LocalDate fromDate = plan.getPlanDate();
        plan.postpone(toDate);
        eventPublisher.publishEvent(new PlanChangedEvent(userId, fromDate));
        eventPublisher.publishEvent(new PlanChangedEvent(userId, toDate));
        return GetPlanResponseDto.from(plan);
    }

    // 5. 플랜 삭제
    @Transactional
    public void deletePlan(Long userId, Long planId) {
        Plan plan = planRepository.findByPlanIdAndUserId(planId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._PLAN_NOT_FOUND));
        planDetailRepository.deleteByPlan(plan);
        planRepository.delete(plan);
        eventPublisher.publishEvent(new PlanChangedEvent(userId, plan.getPlanDate()));
    }
}


