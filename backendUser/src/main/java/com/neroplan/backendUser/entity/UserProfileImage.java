package com.neroplan.backendUser.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사용자가 직접 올린 프로필 사진 - users 조회 때마다 이미지를 읽지 않도록 테이블을 분리
@Entity
@Table(name = "user_profile_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfileImage {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private byte[] data;

    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    public UserProfileImage(Long userId, byte[] data, String contentType) {
        this.userId = userId;
        this.data = data;
        this.contentType = contentType;
    }

    public void replace(byte[] data, String contentType) {
        this.data = data;
        this.contentType = contentType;
    }
}
