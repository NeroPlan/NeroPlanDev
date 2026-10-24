package com.neroplan.backendUser.global.apiPayload.code;

import com.neroplan.backendUser.global.apiPayload.code.errorDto.ReasonDto;

public interface BaseCode {
    public ReasonDto getReason();

    public ReasonDto getReasonHttpStatus();
}