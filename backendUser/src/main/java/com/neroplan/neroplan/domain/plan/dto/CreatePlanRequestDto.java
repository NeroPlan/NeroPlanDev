package com.neroplan.neroplan.domain.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatePlanRequestDto {

    @NotBlank(message = "계획 내용은 필수입니다.")
    private String content;

    @NotNull(message = "우선순위는 필수입니다.")
    private Long priority;

    // userId의 경우 서버가 토큰에서 추출
    // categoryId의 경우 서버가 AI를 통해 결정
    // private Long userId;
    // private Long categoryId;
}
