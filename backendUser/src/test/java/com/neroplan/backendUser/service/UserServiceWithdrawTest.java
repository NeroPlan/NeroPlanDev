package com.neroplan.backendUser.service;

import com.neroplan.backendUser.entity.Role;
import com.neroplan.backendUser.entity.User;
import com.neroplan.backendUser.global.apiPayload.exception.GeneralException;
import com.neroplan.backendUser.repository.UserProfileImageRepository;
import com.neroplan.backendUser.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceWithdrawTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserProfileImageRepository imageRepository = mock(UserProfileImageRepository.class);
    private final UserService userService = new UserService(userRepository, imageRepository);

    @Test
    void 탈퇴하면_프로필_사진과_계정을_삭제한다() {
        User user = User.builder().user_id(1L).nickname("테스트").role(Role.values()[0]).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.withdraw(1L);

        verify(imageRepository).deleteById(1L);
        verify(userRepository).delete(user);
    }

    @Test
    void 없는_사용자나_토큰_없는_요청은_거절한다() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.withdraw(2L)).isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> userService.withdraw(null)).isInstanceOf(GeneralException.class);
        verify(userRepository, never()).delete(any());
    }
}
