package com.neroplan.backendUser.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UserUpdateRequestDto {

    private String nickname;
    private String profileImage;
    private LocalDate birth;
    private String gender;
}
