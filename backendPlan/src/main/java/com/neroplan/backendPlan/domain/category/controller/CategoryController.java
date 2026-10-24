package com.neroplan.backendPlan.domain.category.controller;

import com.neroplan.backendPlan.domain.category.dto.GetCategoryResponseDto;
import com.neroplan.backendPlan.domain.category.service.CategoryService;
import com.neroplan.backendPlan.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// nginx가 /api/v1/plans 경로만 backendPlan으로 넘기므로 그 하위에 둠
@RestController
@RequestMapping("/api/v1/plans/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    // 1. 유저별 카테고리 전체 조회
    @GetMapping
    public ApiResponse<List<GetCategoryResponseDto>> getCategories(@AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(categoryService.getCategories(userId));
    }
}
