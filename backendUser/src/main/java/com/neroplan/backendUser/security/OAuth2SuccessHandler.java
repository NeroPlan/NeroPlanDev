package com.neroplan.backendUser.security;

import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler
        implements AuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;

    // 로그인 성공 후 이동할 프론트 주소 - 배포 환경마다 다르므로 환경변수로 주입
    @Value("${app.oauth2.redirect-url}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // JWT 생성
        String accessToken =
                tokenProvider.generateAccessToken(
                        authentication
                );

        // 프론트로 JWT 전달
        String targetUrl =
                UriComponentsBuilder
                        .fromUriString(redirectUrl)
                        .queryParam(
                                "accessToken",
                                accessToken
                        )
                        .build()
                        .toUriString();

        response.sendRedirect(targetUrl);
    }
}