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
import com.neroplan.backendPlan.domain.category.entity.Category;
import com.neroplan.backendPlan.global.ai.AiClient;
import com.neroplan.backendPlan.global.ai.AiServiceUnavailableException;
import com.neroplan.backendPlan.global.apiPayload.code.status.ErrorStatus;
import com.neroplan.backendPlan.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.neroplan.backendPlan.domain.plan.dto.CreatePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.CreatePlanResponseDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlanService {

    // 분석하기 한 번에 LLM으로 분류할 최대 계획 수 - 미분류 계획이 많이 쌓여도 응답 시간이 끝없이 늘지 않도록 제한
    private static final int MAX_CLASSIFY_PER_REQUEST = 20;

    // 달력 한 번 조회의 최대 기간(일) - 월간 보기는 6주(42일)라 여유 있게 잡음
    private static final int MAX_RANGE_DAYS = 62;

    private final PlanRepository planRepository;
    private final PlanDetailRepository planDetailRepository;
    private final CategoryService categoryService;
    private final AiClient aiClient;
    private final PlanScheduleService planScheduleService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    // 1. 플랜 생성
    @Transactional
    public CreatePlanResponseDto createPlan(Long userId, CreatePlanRequestDto requestDto) {
        if (userId == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        LocalDate today = LocalDate.now();
        LocalDate planDate = requestDto.getPlanDate() != null ? requestDto.getPlanDate() : today;
        if (planDate.isBefore(today)) {
            throw new GeneralException(ErrorStatus._INVALID_PLAN_DATE);
        }
        Plan plan = Plan.builder()
                .userId(userId)
                .content(requestDto.getContent())
                .priority(requestDto.getPriority())
                .estimatedMinutes(requestDto.getEstimatedMinutes())
                .planDate(planDate)
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
        Map<Long, PlanDetail> detailsByPlanId = planDetailRepository.findMapByPlanIn(plans);

        // AI 추천 순서대로 정렬, 순서가 없는 계획(분석 전/완료)은 뒤로
        return plans.stream()
                .map(plan -> GetPlanResponseDto.from(plan, detailsByPlanId.get(plan.getPlanId())))
                .sorted(Comparator.comparing(GetPlanResponseDto::getRecommendedOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    // 3-1. 기간별 조회 (달력) - 날짜순, 같은 날 안에서는 AI 추천 순서 → 생성순
    @Transactional(readOnly = true)
    public List<GetPlanResponseDto> getPlansByDateRange(Long userId, LocalDate from, LocalDate to) {
        if (from.isAfter(to) || from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw new GeneralException(ErrorStatus._INVALID_DATE_RANGE);
        }
        List<Plan> plans = planRepository.findByUserIdAndPlanDateBetween(userId, from, to);
        Map<Long, PlanDetail> detailsByPlanId = planDetailRepository.findMapByPlanIn(plans);

        return plans.stream()
                .map(plan -> GetPlanResponseDto.from(plan, detailsByPlanId.get(plan.getPlanId())))
                .sorted(Comparator.comparing(GetPlanResponseDto::getPlanDate)
                        .thenComparing(GetPlanResponseDto::getRecommendedOrder,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(GetPlanResponseDto::getCreatedTime))
                .toList();
    }

    // 8. 오늘 계획 분석 - 미리 계산된 결과가 없거나 오래됐을 수 있으므로 요청 시 다시 계산
    // LLM 호출이 오래 걸릴 수 있어 전체를 하나의 트랜잭션으로 묶지 않음 (DB 커넥션을 붙잡지 않도록)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<GetPlanResponseDto> analyzeTodayPlans(Long userId) {
        if (userId == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        LocalDate today = LocalDate.now();

        classifyUncategorizedPlans(userId, today);

        // 재배열은 PlanScheduleService.recalculate의 자체 트랜잭션에서 저장
        if (!planScheduleService.recalculate(userId, today)) {
            throw new GeneralException(ErrorStatus._AI_ANALYSIS_FAILED);
        }
        // 같은 클래스 안의 호출이라 @Transactional이 적용되지 않으므로 직접 트랜잭션을 염
        return transactionTemplate.execute(status -> getTodayPlansByUserId(userId));
    }

    // 생성 시에는 분류하지 않으므로 카테고리가 없는 계획을 여기서 분류
    // 지난 계획도 AI 이력과 분석 페이지 통계에 들어가야 하므로 날짜·상태와 관계없이 분류하되,
    // 오늘 계획을 먼저(바로 재배열 대상), 그다음 지난 계획을 오래된 순으로 요청당 최대 MAX_CLASSIFY_PER_REQUEST개만 분류
    // 남은 계획은 다음 분석하기 때 이어서 분류됨
    private void classifyUncategorizedPlans(Long userId, LocalDate today) {
        List<Plan> targets = planRepository.findByUserIdAndCategoryIsNull(userId).stream()
                .sorted(Comparator.comparing((Plan plan) -> !today.equals(plan.getPlanDate()))
                        .thenComparing(Plan::getPlanDate)
                        .thenComparing(Plan::getCreatedTime))
                .limit(MAX_CLASSIFY_PER_REQUEST)
                .toList();
        if (targets.isEmpty()) {
            return;
        }

        // 새로 생긴 카테고리를 다음 계획 분류 때 재사용하도록 이름 목록을 직접 갱신
        List<String> categoryNames = new ArrayList<>(categoryService.getCategoryNames(userId));
        for (Plan plan : targets) {
            Optional<String> name;
            try {
                // 트랜잭션 밖에서 호출 - 응답을 기다리는 동안 DB 커넥션을 쓰지 않음
                name = aiClient.classifyCategory(plan.getContent(), List.copyOf(categoryNames));
            } catch (AiServiceUnavailableException e) {
                // aiService 장애 시 남은 계획마다 타임아웃을 기다리지 않고 중단
                break;
            }
            name.ifPresent(categoryName -> transactionTemplate.executeWithoutResult(status ->
                    assignCategory(userId, plan.getPlanId(), categoryName, categoryNames)));
        }
    }

    // 분류 결과 저장 - 계획마다 짧은 트랜잭션으로 실행
    private void assignCategory(Long userId, Long planId, String categoryName, List<String> categoryNames) {
        planRepository.findByPlanIdAndUserId(planId, userId)
                // 분류하는 동안 삭제됐거나 다른 요청이 먼저 분류했으면 건너뜀
                .filter(plan -> plan.getCategory() == null)
                .ifPresent(plan -> {
                    Category category = categoryService.findOrCreate(userId, categoryName);
                    if (category == null) {
                        return;
                    }
                    plan.assignCategory(category);
                    if (!categoryNames.contains(category.getName())) {
                        categoryNames.add(category.getName());
                    }
                });
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


