package com.neroplan.backendPlan.global.apiPayload.code;

import com.neroplan.backendPlan.global.apiPayload.code.errorDto.ErrorReasonDto;


public interface BaseErrorCode {

    public ErrorReasonDto getReason();

    public ErrorReasonDto getReasonHttpStatus();
}