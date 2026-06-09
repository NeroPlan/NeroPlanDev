package com.neroplan.backendPlan.global.apiPayload.code.errorDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReasonDto {
    private String message;
    private String code;
    private Boolean isSuccess;
    private HttpStatus httpStatus;
}