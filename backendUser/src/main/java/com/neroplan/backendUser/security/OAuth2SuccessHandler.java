package com.neroplan.backendUser.security;

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

    private static final String REDIRECT_URL =
            "http://localhost:8080/plan";

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
        String redirectUrl =
                UriComponentsBuilder
                        .fromUriString(REDIRECT_URL)
                        .queryParam(
                                "accessToken",
                                accessToken
                        )
                        .build()
                        .toUriString();

        response.sendRedirect(redirectUrl);
    }
}