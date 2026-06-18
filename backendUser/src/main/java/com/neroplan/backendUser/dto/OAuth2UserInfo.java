package com.neroplan.backendUser.dto;

public interface OAuth2UserInfo {
    String getProviderId();
    String getEmail();
    String getNickname();
    String getProfileImage();
}