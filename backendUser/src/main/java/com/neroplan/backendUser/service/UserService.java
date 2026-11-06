package com.neroplan.backendUser.service;

import com.neroplan.backendUser.dto.UserResponseDto;
import com.neroplan.backendUser.entity.User;
import com.neroplan.backendUser.entity.UserProfileImage;
import com.neroplan.backendUser.global.apiPayload.code.status.ErrorStatus;
import com.neroplan.backendUser.global.apiPayload.exception.GeneralException;
import com.neroplan.backendUser.repository.UserProfileImageRepository;
import com.neroplan.backendUser.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int MAX_NICKNAME_LENGTH = 30; // users.nickname 컬럼 길이
    private static final long MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    private final UserRepository userRepository;
    private final UserProfileImageRepository userProfileImageRepository;

    @Transactional(readOnly = true)
    public UserResponseDto getMe(Long userId) {
        return UserResponseDto.from(findUser(userId));
    }

    @Transactional
    public UserResponseDto updateNickname(Long userId, String nickname) {
        String trimmed = nickname == null ? "" : nickname.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NICKNAME_LENGTH) {
            throw new GeneralException(ErrorStatus._INVALID_NICKNAME);
        }

        User user = findUser(userId);
        user.updateNickname(trimmed);
        return UserResponseDto.from(user);
    }

    @Transactional
    public UserResponseDto updateProfileImage(Long userId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new GeneralException(ErrorStatus._IMAGE_UPLOAD_FAIL);
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new GeneralException(ErrorStatus._IMAGE_TOO_LARGE);
        }

        byte[] data;
        try {
            data = image.getBytes();
        } catch (IOException e) {
            throw new GeneralException(ErrorStatus._IMAGE_UPLOAD_FAIL);
        }

        // 클라이언트가 보낸 Content-Type은 믿지 않고 파일 앞부분으로 형식을 판단
        String contentType = detectImageType(data);
        if (contentType == null) {
            throw new GeneralException(ErrorStatus._PROFILE_IMAGE_NOT_SUPPORTED);
        }

        User user = findUser(userId);
        userProfileImageRepository.findById(userId).ifPresentOrElse(
                saved -> saved.replace(data, contentType),
                () -> userProfileImageRepository.save(new UserProfileImage(userId, data, contentType))
        );

        // 주소가 바뀌어야 브라우저 캐시 대신 새 사진을 받음
        user.updateProfileImage("/api/v1/users/" + userId + "/profile-image?v=" + System.currentTimeMillis());
        return UserResponseDto.from(user);
    }

    @Transactional(readOnly = true)
    public UserProfileImage getProfileImage(Long userId) {
        return userProfileImageRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._PROFILE_IMAGE_NOT_FOUND));
    }

    private User findUser(Long userId) {
        if (userId == null) {
            throw new GeneralException(ErrorStatus._UNAUTHORIZED);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
    }

    private static String detectImageType(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') {
            return "image/png";
        }
        if (d.length >= 6 && d[0] == 'G' && d[1] == 'I' && d[2] == 'F' && d[3] == '8') {
            return "image/gif";
        }
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }
}
