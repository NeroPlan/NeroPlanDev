package com.neroplan.backendUser.dto;

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
    private String profileImage;
    private LocalDate birth;
    private String gender;
    private String googleId;

}
