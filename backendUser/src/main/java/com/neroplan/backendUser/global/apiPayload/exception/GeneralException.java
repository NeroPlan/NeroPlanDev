package com.neroplan.backendUser.global.apiPayload.exception;

import com.neroplan.backendUser.global.apiPayload.code.BaseErrorCode;
import com.neroplan.backendUser.global.apiPayload.code.errorDto.ErrorReasonDto;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException{

    private BaseErrorCode code;

    public GeneralException(String message) {
        super(message);
        this.code = null;
    }

    public GeneralException(BaseErrorCode code) {
        super(code.getReason().getMessage());
        this.code = code;
    }

    public ErrorReasonDto getErrorReason(){
        return this.code.getReason();
    }

    public ErrorReasonDto getErrorReasonHttpStatus(){
        return this.code.getReasonHttpStatus();
    }
}