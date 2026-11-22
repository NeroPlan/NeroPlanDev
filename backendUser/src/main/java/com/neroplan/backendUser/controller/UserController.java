package com.neroplan.backendUser.controller;

import com.neroplan.backendUser.dto.UpdateNicknameRequestDto;
import com.neroplan.backendUser.dto.UserResponseDto;
import com.neroplan.backendUser.entity.UserProfileImage;
import com.neroplan.backendUser.global.apiPayload.ApiResponse;
import com.neroplan.backendUser.security.RefreshTokenCookie;
import com.neroplan.backendUser.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final RefreshTokenCookie refreshTokenCookie;

    // 내 정보 조회
    @GetMapping("/me")
    public ApiResponse<UserResponseDto> getMe(@AuthenticationPrincipal Long userId) {
        return ApiResponse.onSuccess(userService.getMe(userId));
    }

    // 이름 변경
    @PatchMapping("/me")
    public ApiResponse<UserResponseDto> updateNickname(
            @AuthenticationPrincipal Long userId,
            @RequestBody UpdateNicknameRequestDto requestDto
    ) {
        return ApiResponse.onSuccess(userService.updateNickname(userId, requestDto.getNickname()));
    }

    // 회원 탈퇴 - 계정 삭제 후 refresh token 쿠키도 만료
    @DeleteMapping("/me")
    public ApiResponse<String> withdraw(@AuthenticationPrincipal Long userId, HttpServletResponse response) {
        userService.withdraw(userId);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.expire().toString());
        return ApiResponse.onSuccess("회원 탈퇴가 완료되었습니다.");
    }

    // 프로필 사진 변경 - multipart의 image 필드
    @PutMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponseDto> updateProfileImage(
            @AuthenticationPrincipal Long userId,
            @RequestPart("image") MultipartFile image
    ) {
        return ApiResponse.onSuccess(userService.updateProfileImage(userId, image));
    }

    // 프로필 사진 조회 - <img>는 토큰을 못 보내므로 인증 없이 허용(SecurityConfig)
    // 사진이 바뀌면 주소의 ?v= 값이 바뀌므로 오래 캐시해도 됨
    @GetMapping("/{userId}/profile-image")
    public ResponseEntity<byte[]> getProfileImage(@PathVariable Long userId) {
        UserProfileImage image = userService.getProfileImage(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.getData());
    }
}
