package com.neroplan.backendPlan.global.ai;

// aiService 연결·응답 실패 - 여러 건을 연달아 호출할 때 남은 호출을 멈추는 데 사용
public class AiServiceUnavailableException extends RuntimeException {
    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
