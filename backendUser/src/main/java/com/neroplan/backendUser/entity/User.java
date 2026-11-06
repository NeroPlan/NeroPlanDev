package com.neroplan.backendUser.entity;

import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Id;

import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long user_id;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(nullable = true)
    private LocalDate birth;

    @Column(length = 10)
    private String gender;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 구글 사진 주소 또는 직접 올린 사진의 조회 주소
    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}
