package com.neroplan.backendUser.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

// refresh token 쿠키 - HttpOnly라 자바스크립트(XSS)로 읽을 수 없고, /api/v1/auth 요청에만 실려 감
@Component
public class RefreshTokenCookie {

    public static final String NAME = "refreshToken";
    private static final String PATH = "/api/v1/auth";

    private final boolean secure;

    public RefreshTokenCookie(@Value("${app.cookie.secure:true}") boolean secure) {
        this.secure = secure;
    }

    public ResponseCookie create(String refreshToken) {
        return build(refreshToken, TokenProvider.REFRESH_TOKEN_TTL);
    }

    // 로그아웃 - 같은 이름·경로로 maxAge 0을 보내 브라우저에서 삭제
    public ResponseCookie expire() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                // 다른 사이트에서 보낸 POST에는 쿠키가 실리지 않음 (CSRF 방지), 구글 로그인 후 돌아오는 이동에는 실림
                .sameSite("Lax")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
