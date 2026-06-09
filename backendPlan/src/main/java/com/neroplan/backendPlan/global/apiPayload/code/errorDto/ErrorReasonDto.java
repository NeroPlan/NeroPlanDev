package com.neroplan.backendPlan.global.apiPayload.code.errorDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorReasonDto {
    private String message;
    private String code;
    private Boolean isSuccess;
    private HttpStatus httpStatus;
}
