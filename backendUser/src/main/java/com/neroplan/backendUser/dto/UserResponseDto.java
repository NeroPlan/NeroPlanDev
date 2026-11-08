package com.neroplan.backendUser.dto;

import com.neroplan.backendUser.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class UserResponseDto {

    private Long userId;
    private String nickname;
    private String profileImage; // 구글 사진 주소 또는 /api/v1/users/{id}/profile-image
    private LocalDate birth;
    private String gender;

    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .userId(user.getUser_id())
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .birth(user.getBirth())
                .gender(user.getGender())
                .build();
    }
}
