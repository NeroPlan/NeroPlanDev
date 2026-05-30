package com.neroplan.backendUser.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserLoginResponseDto {

    private String accessToken;
    private String refreshToken;
    private boolean isNewUser;

}
