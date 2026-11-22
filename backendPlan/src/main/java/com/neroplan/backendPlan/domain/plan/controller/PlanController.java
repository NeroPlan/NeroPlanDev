package com.neroplan.backendPlan.domain.plan.controller;

import com.neroplan.backendPlan.domain.plan.dto.CompletePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.FocusPlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.CreatePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.CreatePlanResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.GetDailyStatResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanStatsResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanSummaryResponseDto;
import com.neroplan.backendPlan.domain.plan.dto.PostponePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.dto.UpdatePlanRequestDto;
import com.neroplan.backendPlan.domain.plan.service.PlanService;
import com.neroplan.backendPlan.domain.plan.service.PlanStatsService;
import com.neroplan.backendPlan.global.apiPayload.ApiResponse;
import com.neroplan.backendPlan.global.apiPayload.code.status.ErrorStatus;
import com.neroplan.backendPlan.global.apiPayload.exception.GeneralException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    // 3. 계획 조회 - 파라미터가 없으면 오늘 계획, from/to(YYYY-MM-DD)를 주면 그 기간의 계획 (달력 화면)
    @GetMapping
    public ApiResponse<List<GetPlanResponseDto>> getPlansByUser(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null && to == null) {
            return ApiResponse.onSuccess(planService.getTodayPlansByUserId(userId));
        }
        if (from == null || to == null) {
            throw new GeneralException(ErrorStatus._INVALID_DATE_RANGE);
        }
        return ApiResponse.onSuccess(planService.getPlansByDateRange(userId, from, to));
    }

    // 8. 오늘 계획 분석 - aiService로 위험도 판정과 재배열을 다시 계산해 추천 수행 순서대로 반환
    @PostMapping("/analyze")
    public ApiResponse<List<GetPlanResponseDto>> analyzeTodayPlans(@AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(planService.analyzeTodayPlans(userId));
    }

    // 9. 분석 페이지 통계 - 카테고리별 성공률과 수행 패턴, days(최근 N일)가 없으면 전체 기간
    @GetMapping("/stats")
    public ApiResponse<GetPlanStatsResponseDto> getPlanStats(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Integer days) {
        return ApiResponse.onSuccess(planStatsService.getStats(userId, days));
    }

    // 10. 완료 잔디 - from~to(최대 120일) 날짜별 계획 수/완료 수
    @GetMapping("/stats/daily")
    public ApiResponse<List<GetDailyStatResponseDto>> getDailyStats(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.onSuccess(planStatsService.getDailyStats(userId, from, to));
    }

    // 11. 마이페이지 요약 - 연속 달성 일수, 이번 주 달성, 총 완료 수
    @GetMapping("/stats/summary")
    public ApiResponse<GetPlanSummaryResponseDto> getPlanSummary(@AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(planStatsService.getSummary(userId));
    }

    // 12. 회원 탈퇴 시 계획 데이터 전체 삭제 - 프론트가 backendUser 탈퇴 전에 호출
    @DeleteMapping("/me")
    public ApiResponse<String> deleteMyData(@AuthenticationPrincipal Long userId) {
        planService.deleteAllData(userId);
        return ApiResponse.onSuccess("계획 데이터를 모두 삭제했습니다.");
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

    // 13. 집중 타이머 기록 - 집중한 시간을 누적하고, complete가 true면 완료 처리
    @PostMapping("/{planId}/focus")
    public ApiResponse<GetPlanResponseDto> recordFocus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long planId,
            @Valid @RequestBody FocusPlanRequestDto requestDto) {
        return ApiResponse.onSuccess(planService.recordFocus(userId, planId, requestDto));
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
