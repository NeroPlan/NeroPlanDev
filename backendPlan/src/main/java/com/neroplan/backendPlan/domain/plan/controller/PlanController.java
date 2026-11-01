package com.neroplan.backendPlan.domain.plan.controller;

import com.neroplan.backendPlan.domain.plan.dto.CompletePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.CreatePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.CreatePlanResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanStatsResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.PostponePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.UpdatePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.service.PlanService;
import com.neroplan.backendPlan.domain.plan.service.PlanStatsService;
import com.neroplan.backendPlan.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {
    private final PlanService planService;
    private final PlanStatsService planStatsService;

    // 1. 플랜 생성
    @PostMapping
    public ApiResponse<CreatePlanResponseDto> createPlan(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreatePlanRequestDto requestDto) {
        return ApiResponse.onSuccess(planService.createPlan(userId, requestDto));
    }

    // 2. 플랜별 단건 조회 - api 명세에 없으나 단건 조회가 필요한 경우 사용
    @GetMapping("/{planId}")
    public ApiResponse<GetPlanResponseDto> getPlan(@AuthenticationPrincipal Long userId, @PathVariable Long planId) {
        return ApiResponse.onSuccess(planService.getPlan(userId, planId));
    }

    // 3. 유저별 전체 조회 - date 파라미터를 붙일지 고민하고 수정 필요
    // 오늘의 계획만 가져와야함.
    @GetMapping
    public ApiResponse<List<GetPlanResponseDto>> getPlansByUser(@AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(planService.getTodayPlansByUserId(userId));
//        return ApiResponse.onSuccess(planService.getPlansByUserId(userId));
    }

    // 8. 오늘 계획 분석 - aiService로 위험도 판정과 재배열을 다시 계산해 추천 수행 순서대로 반환
    @PostMapping("/analyze")
    public ApiResponse<List<GetPlanResponseDto>> analyzeTodayPlans(@AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(planService.analyzeTodayPlans(userId));
    }

    // 9. 분석 페이지 통계 - 카테고리별 성공률과 수행 패턴
    @GetMapping("/stats")
    public ApiResponse<GetPlanStatsResponseDto> getPlanStats(@AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(planStatsService.getStats(userId));
    }

    // 4. 플랜 수정
    @PatchMapping("/{planId}")
    public ApiResponse<GetPlanResponseDto> updatePlan(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @Valid @RequestBody UpdatePlanRequestDto requestDto) {
        return ApiResponse.onSuccess(planService.updatePlan(userId, planId, requestDto));
    }

    // 6. 플랜 완료 처리 / 완료 취소
    @PatchMapping("/{planId}/complete")
    public ApiResponse<GetPlanResponseDto> completePlan(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @Valid @RequestBody CompletePlanRequestDto requestDto) {
        return ApiResponse.onSuccess(planService.completePlan(userId, planId, requestDto));
    }

    // 7. 플랜 연기 - body 없이 호출하면 다음 날로 연기
    @PatchMapping("/{planId}/postpone")
    public ApiResponse<GetPlanResponseDto> postponePlan(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @RequestBody(required = false) PostponePlanRequestDto requestDto) {
        return ApiResponse.onSuccess(planService.postponePlan(userId, planId,
                requestDto != null ? requestDto : new PostponePlanRequestDto(null)));
    }

    // 5. 플랜 삭제
    @DeleteMapping("/{planId}")
    public ApiResponse<String> deletePlan(@AuthenticationPrincipal Long userId, @PathVariable Long planId) {
        planService.deletePlan(userId, planId);
        return ApiResponse.onSuccess("성공적으로 삭제되었습니다.");
    }
}
