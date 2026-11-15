package com.neroplan.backendUser.controller;

import com.neroplan.backendUser.dto.TokenResponseDto;
import com.neroplan.backendUser.entity.User;
import com.neroplan.backendUser.global.apiPayload.ApiResponse;
import com.neroplan.backendUser.global.apiPayload.code.status.ErrorStatus;
import com.neroplan.backendUser.global.apiPayload.exception.GeneralException;
import com.neroplan.backendUser.repository.UserRepository;
import com.neroplan.backendUser.security.RefreshTokenCookie;
import com.neroplan.backendUser.security.TokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// access token 재발급과 로그아웃 - refresh token은 HttpOnly 쿠키로만 주고받음
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final TokenProvider tokenProvider;
    private final RefreshTokenCookie refreshTokenCookie;
    private final UserRepository userRepository;

    // access token 재발급 - 로그인 직후, 새로고침 후, access token 만료 시 프론트가 호출
    // refresh token도 새로 발급해 쿠키 만료일을 연장 (계속 쓰는 동안은 다시 로그인하지 않음)
    @PostMapping("/refresh")
    public ApiResponse<TokenResponseDto> refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        Long userId = tokenProvider.getUserIdFromRefreshToken(refreshToken)
                .orElseThrow(() -> new GeneralException(ErrorStatus._TOKEN_INVALID));
        // 탈퇴 등으로 사라진 사용자의 토큰은 거부
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._TOKEN_INVALID));

        response.addHeader(HttpHeaders.SET_COOKIE,
                refreshTokenCookie.create(tokenProvider.generateRefreshToken(userId)).toString());
        return ApiResponse.onSuccess(
                new TokenResponseDto(tokenProvider.generateAccessToken(userId, user.getRole().getKey())));
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.expire().toString());
        return ApiResponse.onSuccess("로그아웃되었습니다.");
    }
}
