package com.neroplan.backendPlan.domain.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePlanRequestDto {

    @NotBlank(message = "계획 내용은 필수입니다.")
    private String content;

    @NotNull(message = "우선순위는 필수입니다.")
    private Long priority;

    // 예상 소요 시간(분)
    @Positive(message = "예상 소요 시간은 1분 이상이어야 합니다.")
    private Integer estimatedMinutes;

    // 수행 예정일 - 없으면 오늘, 지난 날짜는 불가 (달력에서 미래 날짜에 바로 추가할 때 사용)
    private LocalDate planDate;

    // categoryId의 경우 서버가 AI를 통해 결정
    // private Long categoryId;
}
