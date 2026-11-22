package com.neroplan.backendPlan.domain.category.dto;

import com.neroplan.backendPlan.domain.category.entity.Category;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class GetCategoryResponseDto {

    private Long categoryId;
    private String name;
    private Long planCount; // 이 카테고리로 분류된 계획 수 (카테고리 관리 화면)

    public static GetCategoryResponseDto from(Category category) {
        return from(category, 0L);
    }

    public static GetCategoryResponseDto from(Category category, Long planCount) {
        return GetCategoryResponseDto.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .planCount(planCount)
                .build();
    }
}
