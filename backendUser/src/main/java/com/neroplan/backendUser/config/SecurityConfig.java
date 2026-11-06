package com.neroplan.backendUser.config;

import com.neroplan.backendUser.security.OAuth2SuccessHandler;
import com.neroplan.backendUser.security.TokenAuthenticationFilter;
import com.neroplan.backendUser.security.TokenExceptionFilter;
import com.neroplan.backendUser.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    private final OAuth2SuccessHandler successHandler;

    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http
    ) throws Exception {

        http
                // csrf 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // form 로그인 비활성화
                .formLogin(FormLoginConfigurer::disable)
                // http basic 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                // cors 허용
                .cors(Customizer.withDefaults())
                // 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 로그인 관련 API는 허용
                        .requestMatchers(
                                "/",
                                "/error",
                                "/api/v1/auth/login/google",
                                "/oauth2/**"
                        )
                        .permitAll()
                        // <img>로 불러오는 프로필 사진은 토큰 없이 조회
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/profile-image")
                        .permitAll()
                        // 나머지는 인증 필요
                        .anyRequest()
                        .authenticated()
                )
                // API는 로그인 페이지로 리다이렉트(302)하지 않고 401을 돌려줌 - 프론트가 401을 받으면 로그인 화면으로 이동
                .exceptionHandling(exception -> exception
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                PathPatternRequestMatcher.withDefaults().matcher("/api/**")
                        )
                )
                // OAuth2 로그인 설정
                .oauth2Login(oauth -> oauth
                        // 구글 유저 정보 조회
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(
                                        customOAuth2UserService
                                )
                        )
                        // 로그인 성공 시 JWT 발급
                        .successHandler(successHandler)
                )
                // JWT 인증 필터
                .addFilterBefore(
                        tokenAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                // JWT 예외 처리
                .addFilterBefore(
                        new TokenExceptionFilter(),
                        TokenAuthenticationFilter.class
                );
        return http.build();
    }
}