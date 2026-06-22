package com.neroplan.backendUser.service;

import com.neroplan.backendUser.dto.GoogleOAuth2UserInfo;
//import com.neroplan.backendUser.dto.KakaoOAuth2UserInfo;
//import com.neroplan.backendUser.dto.NaverOAuth2UserInfo;
import com.neroplan.backendUser.dto.OAuth2UserInfo;
import com.neroplan.backendUser.entity.Role;
import com.neroplan.backendUser.entity.User;
import com.neroplan.backendUser.repository.UserRepository;
import com.neroplan.backendUser.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(
            OAuth2UserRequest userRequest
    ) throws OAuth2AuthenticationException {

        OAuth2User oauth2User =
                super.loadUser(userRequest);

        Map<String, Object> attributes =
                oauth2User.getAttributes();

        String registrationId =
                userRequest.getClientRegistration()
                        .getRegistrationId();

        OAuth2UserInfo userInfo;

        if ("google".equals(registrationId)) {

            userInfo =
                    new GoogleOAuth2UserInfo(attributes);

//        } else if ("kakao".equals(registrationId)) {

//            userInfo =
//                    new KakaoOAuth2UserInfo(attributes);
//
//        } else if ("naver".equals(registrationId)) {
//
//            userInfo =
//                    new NaverOAuth2UserInfo(attributes);

        } else {

            throw new OAuth2AuthenticationException(
                    "지원하지 않는 OAuth Provider 입니다."
            );
        }

        User user =
                userRepository.findByEmail(
                                userInfo.getEmail()
                        )
                        .orElseGet(() ->
                                userRepository.save(
                                        User.builder()
                                                .googleId(
                                                        userInfo.getProviderId()
                                                )
                                                .email(
                                                        userInfo.getEmail()
                                                )
                                                .nickname(
                                                        userInfo.getNickname()
                                                )
                                                .profileImage(
                                                        userInfo.getProfileImage()
                                                )
                                                .role(Role.USER)
                                                .build()
                                )
                        );

        return new UserPrincipal(
                user,
                attributes
        );
    }
}