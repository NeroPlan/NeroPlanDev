package com.neroplan.backendPlan.global.apiPayload.code;

import com.neroplan.backendPlan.global.apiPayload.code.errorDto.ReasonDto;

public interface BaseCode {
    public ReasonDto getReason();

    public ReasonDto getReasonHttpStatus();
}