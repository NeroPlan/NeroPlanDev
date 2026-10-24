package com.neroplan.backendPlan.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ClassifyCategoryResponseDto {

    private String category;

    // 기존 카테고리에 없어 새로 만든 이름인지
    @JsonProperty("is_new")
    private Boolean isNew;
}
