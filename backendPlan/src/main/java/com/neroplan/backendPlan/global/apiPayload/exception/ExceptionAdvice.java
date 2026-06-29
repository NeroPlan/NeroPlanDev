package com.neroplan.backendPlan.global.apiPayload.exception;

import com.neroplan.backendPlan.global.apiPayload.ApiResponse;
import com.neroplan.backendPlan.global.apiPayload.code.errorDto.ErrorReasonDto;
import com.neroplan.backendPlan.global.apiPayload.code.status.ErrorStatus;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Hidden
@RestControllerAdvice(annotations = {RestController.class})
public class ExceptionAdvice extends ResponseEntityExceptionHandler {

    // 1. @Validated 에 의한 쿼리 파라미터/패스 Variable 검증 실패 처리
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException e, WebRequest request) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(cv -> cv.getMessage())
                .findFirst()
                .orElse("검증 오류가 발생했습니다.");

        return handleExceptionInternal(e, ErrorStatus._BAD_REQUEST, errorMessage, request);
    }

    // 2. @Valid 에 의한 DTO 및 @RequestBody 검증 실패 처리
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String fieldName = fieldError.getField();
            String errorMessage = Optional.ofNullable(fieldError.getDefaultMessage()).orElse("");
            errors.merge(fieldName, errorMessage, (exist, replace) -> exist + ", " + replace);
        });

        return handleExceptionInternal(e, ErrorStatus._BAD_REQUEST, errors, request);
    }

    // 3. 비즈니스 로직 중 개발자가 던진 커스텀 예외 처리
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<Object> handleGeneralException(GeneralException e, WebRequest request) {
        ErrorReasonDto errorReason = e.getErrorReasonHttpStatus();

        // ApiResponse.onFailure 구조에 맞게 매핑
        ApiResponse<Object> body = ApiResponse.onFailure(errorReason.getCode(), errorReason.getMessage(), null);
        return super.handleExceptionInternal(e, body, HttpHeaders.EMPTY, errorReason.getHttpStatus(), request);
    }

    // 4. 그 외 서버에서 예측하지 못한 모든 예외(500) 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllException(Exception e, WebRequest request) {
        // e.printStackTrace() 대신 log 사용
        log.error("정의되지 않은 서버 에러 발생: ", e);
        return handleExceptionInternal(e, ErrorStatus._INTERNAL_SERVER_ERROR, e.getMessage(), request);
    }

    // 5. 응답 포맷 통합 : ApiResponse 규격을 생성하여 Spring 부모 메서드로 넘겨주는 메서드
    private ResponseEntity<Object> handleExceptionInternal(Exception e, ErrorStatus errorStatus, Object errorData, WebRequest request) {
        ApiResponse<Object> body = ApiResponse.onFailure(errorStatus.getCode(), errorStatus.getMessage(), errorData);
        return super.handleExceptionInternal(e, body, HttpHeaders.EMPTY, errorStatus.getHttpStatus(), request);
    }
}