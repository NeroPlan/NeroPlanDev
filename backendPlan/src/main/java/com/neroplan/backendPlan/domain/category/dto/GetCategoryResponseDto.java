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

    public static GetCategoryResponseDto from(Category category) {
        return GetCategoryResponseDto.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .build();
    }
}
