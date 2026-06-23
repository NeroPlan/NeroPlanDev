package com.neroplan.backendPlan.domain.plan.controller;

import com.neroplan.backendPlan.domain.plan.dto.CreatePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.CreatePlanResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.UpdatePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.service.PlanService;
import com.neroplan.backendPlan.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {
    private final PlanService planService;

    // 1. 플랜 생성
    @PostMapping
    public ApiResponse<CreatePlanResponseDto> createPlan(@RequestBody CreatePlanRequestDto requestDto) {
        return ApiResponse.onSuccess(planService.createPlan(requestDto));
    }

    // 2. 플랜별 단건 조회 - api 명세에 없으나 단건 조회가 필요한 경우 사용
    @GetMapping("/{planId}")
    public ApiResponse<GetPlanResponseDto> getPlan(@PathVariable Long planId) {
        return ApiResponse.onSuccess(planService.getPlan(planId));
    }

    // 3. 유저별 전체 조회 - date 파라미터를 붙일지 고민하고 수정 필요
    @GetMapping
    public ApiResponse<List<GetPlanResponseDto>> getPlansByUser(@RequestParam Long userId) {
        return ApiResponse.onSuccess(planService.getPlansByUserId(userId));
    }

    // 4. 플랜 수정
    @PatchMapping("/{planId}")
    public ApiResponse<GetPlanResponseDto> updatePlan(
            @PathVariable Long planId,
            @RequestBody UpdatePlanRequestDto requestDto) {
        return ApiResponse.onSuccess(planService.updatePlan(planId, requestDto));
    }

    // 5. 플랜 삭제
    @DeleteMapping("/{planId}")
    public ApiResponse<String> deletePlan(@PathVariable Long planId) {
        planService.deletePlan(planId);
        return ApiResponse.onSuccess("성공적으로 삭제되었습니다.");
    }
}
