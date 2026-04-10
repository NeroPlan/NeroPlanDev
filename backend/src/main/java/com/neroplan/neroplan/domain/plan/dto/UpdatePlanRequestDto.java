package com.neroplan.neroplan.domain.plan.dto;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UpdatePlanRequestDto {

    private String content;
    private Long priority;

}
