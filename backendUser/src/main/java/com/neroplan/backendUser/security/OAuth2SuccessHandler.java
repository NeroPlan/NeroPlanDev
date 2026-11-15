package com.neroplan.backendUser.security;

import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final RefreshTokenCookie refreshTokenCookie;

    // 로그인 성공 후 이동할 프론트 주소 - 배포 환경마다 다르므로 환경변수로 주입
    @Value("${app.oauth2.redirect-url}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // 토큰을 URL에 붙이면 브라우저 기록·프록시 로그·Referer로 새어 나갈 수 있으므로
        // refresh token만 HttpOnly 쿠키로 내려주고, 프론트가 /api/v1/auth/refresh로 access token을 받아감
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getUserId());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(refreshToken).toString());

        // 로그인 과정에서 만든 세션은 더 쓰지 않음
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }

        response.sendRedirect(redirectUrl);
    }
}
