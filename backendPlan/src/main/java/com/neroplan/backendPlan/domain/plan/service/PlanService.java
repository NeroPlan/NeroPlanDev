package com.neroplan.backendPlan.domain.plan.service;

import com.neroplan.backendPlan.domain.plan.dto.CompletePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.PostponePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.UpdatePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.entity.Plan;
import com.neroplan.backendPlan.domain.plan.entity.PlanStatus;
import com.neroplan.backendPlan.domain.plan.repository.PlanRepository;
import com.neroplan.backendPlan.global.apiPayload.code.status.ErrorStatus;
import com.neroplan.backendPlan.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neroplan.backendPlan.domain.plan.dto.CreatePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.CreatePlanResponseDto;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlanService {

    private final PlanRepository planRepository;

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
        return planRepository.findByUserIdAndPlanDate(userId, LocalDate.now()).stream()
                .map(plan -> GetPlanResponseDto.from(plan))
                .toList();
    }

    // 4. 플랜 수정
    @Transactional
    public GetPlanResponseDto updatePlan(Long userId, Long planId, UpdatePlanRequestDto requestDto) {
        Plan plan = planRepository.findByPlanIdAndUserId(planId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._PLAN_NOT_FOUND));
        plan.updatePlan(requestDto.getContent(), requestDto.getPriority(), requestDto.getEstimatedMinutes());
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

        plan.postpone(toDate);
        return GetPlanResponseDto.from(plan);
    }

    // 5. 플랜 삭제
    @Transactional
    public void deletePlan(Long userId, Long planId) {
        Plan plan = planRepository.findByPlanIdAndUserId(planId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._PLAN_NOT_FOUND));
        planRepository.delete(plan);
    }
}


