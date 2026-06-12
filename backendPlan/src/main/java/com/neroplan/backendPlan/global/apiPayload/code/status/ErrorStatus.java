package com.neroplan.backendPlan.global.apiPayload.code.status;

import com.neroplan.backendPlan.global.apiPayload.code.BaseCode;
import com.neroplan.backendPlan.global.apiPayload.code.BaseErrorCode;

import com.neroplan.backendPlan.global.apiPayload.code.errorDto.ErrorReasonDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {
    // 500 Internal Server Error
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "서버 에러, 관리자에게 문의 바랍니다."),
    _DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "데이터베이스 에러, 관리자에게 문의 바랍니다."),
    _FILE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "파일 업로드에 실패하였습니다."),
    _FILE_DOWNLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "파일 다운로드에 실패하였습니다."),
    _FILE_DELETE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "파일 삭제에 실패하였습니다."),

    // 400 Bad Request
    _BAD_REQUEST(HttpStatus.BAD_REQUEST, "400", "잘못된 요청입니다."),
    _MAIL_ERROR(HttpStatus.BAD_REQUEST, "400", "인증 이메일 전송에 실패하였습니다."),
    _DUPLICATED_EMAIL(HttpStatus.BAD_REQUEST, "400", "중복된 이메일입니다."),
    _DUPLICATED_LOGIN_ID(HttpStatus.BAD_REQUEST, "400", "중복된 로그인 ID입니다."),
    _NON_EXIST_EMAIL(HttpStatus.BAD_REQUEST, "400", "존재하지 않는 이메일입니다."),
    _BAD_PASSWORD(HttpStatus.BAD_REQUEST, "400", "잘못된 패스워드입니다."),
    _PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "400", "패스워드가 일치하지 않습니다."),
    _IMAGE_UPLOAD_FAIL(HttpStatus.BAD_REQUEST, "400", "이미지 업로드에 실패하였습니다."),
    _IMAGE_DELETE_FAIL(HttpStatus.BAD_REQUEST, "400", "이미지 삭제에 실패하였습니다."),
    _IMAGE_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "400", "지원하지 않는 이미지 형식입니다. (jpg, jpeg, png, gif)"),
    _USER_NOT_MATCH(HttpStatus.BAD_REQUEST, "400", "해당 유저와 일치하지 않습니다."),
    _USER_AlREADY_REGISTERED(HttpStatus.BAD_REQUEST, "400", "이미 등록된 사용자입니다."),

    // 401 Unauthorized
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401", "인증이 필요합니다."),
    _TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "401", "해당 토큰이 만료되었습니다."),
    _TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "401", "해당 토큰이 유효하지 않습니다."),
    _MUST_BE_LEADER_TO_DO(HttpStatus.UNAUTHORIZED, "401", "해당 작업을 수행할 권한이 없습니다."),
    _USER_NOT_AUTHORIZED(HttpStatus.UNAUTHORIZED, "401", "해당 유저는 인증되지 않았습니다."),


    // 403 Forbidden
    _FORBIDDEN_PASSWORD(HttpStatus.FORBIDDEN, "403", "불가능한 패스워드입니다. 패스워드는 영어, 숫자 8~13글자만 가능합니다."),


    // 404 Not Found
    _TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "해당 토큰을 찾을 수 없습니다."),
    _USER_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "해당 유저를 찾을 수 없습니다."),

    // (Optional) 선택 추가 가능
    _METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "405", "허용되지 않은 메서드입니다."),
    _UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "415", "지원하지 않는 미디어 타입입니다."),
    _TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "429", "요청이 너무 많습니다. 잠시 후 다시 시도하세요."),
    _SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "503", "서비스를 사용할 수 없습니다. 점검 중일 수 있습니다.")

    ;


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDto getReason() {
        return ErrorReasonDto.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDto getReasonHttpStatus() {
        return ErrorReasonDto.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}