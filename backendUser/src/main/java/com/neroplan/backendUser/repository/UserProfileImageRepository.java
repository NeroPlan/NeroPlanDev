package com.neroplan.backendUser.repository;

import com.neroplan.backendUser.entity.UserProfileImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileImageRepository extends JpaRepository<UserProfileImage, Long> {
}
