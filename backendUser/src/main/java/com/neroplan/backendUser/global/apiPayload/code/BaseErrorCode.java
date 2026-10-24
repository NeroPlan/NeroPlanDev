package com.neroplan.backendUser.global.apiPayload.code;

import com.neroplan.backendUser.global.apiPayload.code.errorDto.ErrorReasonDto;


public interface BaseErrorCode {

    public ErrorReasonDto getReason();

    public ErrorReasonDto getReasonHttpStatus();
}