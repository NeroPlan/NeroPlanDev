package com.neroplan.backendPlan.domain.category.controller;

import com.neroplan.backendPlan.domain.category.dto.GetCategoryResponseDto;
import com.neroplan.backendPlan.domain.category.dto.UpdateCategoryRequestDto;
import com.neroplan.backendPlan.domain.category.service.CategoryService;
import com.neroplan.backendPlan.domain.plan.dto.GetPlanResponseDto;
import com.neroplan.backendPlan.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// nginx가 /api/v1/plans 경로만 backendPlan으로 넘기므로 그 하위에 둠
@RestController
@RequestMapping("/api/v1/plans/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    // 1. 유저별 카테고리 전체 조회 - 카테고리별 계획 수 포함
    @GetMapping
    public ApiResponse<List<GetCategoryResponseDto>> getCategories(@AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(categoryService.getCategories(userId));
    }

    // 4. 카테고리에 속한 계획 목록 - 최근 수행일부터
    @GetMapping("/{categoryId}/plans")
    public ApiResponse<List<GetPlanResponseDto>> getCategoryPlans(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long categoryId) {
        return ApiResponse.onSuccess(categoryService.getPlans(userId, categoryId));
    }

    // 2. 이름 변경
    @PatchMapping("/{categoryId}")
    public ApiResponse<GetCategoryResponseDto> renameCategory(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long categoryId,
            @RequestBody UpdateCategoryRequestDto requestDto) {
        return ApiResponse.onSuccess(categoryService.rename(userId, categoryId, requestDto.getName()));
    }

    // 3. 삭제 - moveTo가 있으면 계획을 그 카테고리로 옮기고, 없으면 미분류로 둠 (다음 분석하기 때 다시 분류됨)
    @DeleteMapping("/{categoryId}")
    public ApiResponse<String> deleteCategory(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long categoryId,
            @RequestParam(required = false) Long moveTo) {
        categoryService.delete(userId, categoryId, moveTo);
        return ApiResponse.onSuccess("카테고리를 삭제했습니다.");
    }
}
