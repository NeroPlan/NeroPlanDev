package com.neroplan.neroplan.domain.plan.service;

import com.neroplan.neroplan.domain.plan.dto.GetPlanResponseDto;
import com.neroplan.neroplan.domain.plan.dto.UpdatePlanRequestDto;
import com.neroplan.neroplan.domain.plan.entity.Plan;
import com.neroplan.neroplan.domain.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neroplan.neroplan.domain.plan.dto.CreatePlanRequestDto;
import com.neroplan.neroplan.domain.plan.dto.CreatePlanResponseDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlanService {

    private final PlanRepository planRepository;

    // 1. 플랜 생성
    @Transactional
    public CreatePlanResponseDto createPlan(CreatePlanRequestDto requestDto) {
        Plan plan = Plan.builder()
                .content(requestDto.getContent())
                .priority(requestDto.getPriority())
                .build();

        Plan savedPlan = planRepository.save(plan);

        return CreatePlanResponseDto.from(savedPlan);
    }

    // 2. 플랜 id 별 단건 조회
    @Transactional(readOnly = true)
    public GetPlanResponseDto getPlan(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계획입니다. 플랜아이디 : " + planId));
        return GetPlanResponseDto.from(plan);
    }

    // 3. 유저별 플랜 전체 조회
    @Transactional(readOnly = true)
    public List<GetPlanResponseDto> getPlansByUserId(Long userId) {
        return planRepository.findByUserId(userId).stream()
                .map(plan -> GetPlanResponseDto.from(plan))
                .toList();
    }

    // 4. 플랜 수정
    @Transactional
    public GetPlanResponseDto updatePlan(Long planId, UpdatePlanRequestDto requestDto) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계획입니다. 플랜아이디 : " + planId));
        plan.updatePlan(requestDto.getContent(), requestDto.getPriority());
        return GetPlanResponseDto.from(plan);
    }

    // 5. 플랜 삭제
    @Transactional
    public void deletePlan(Long planId) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 계획입니다. 플랜아이디 : " + planId));
        planRepository.delete(plan);
    }
}


