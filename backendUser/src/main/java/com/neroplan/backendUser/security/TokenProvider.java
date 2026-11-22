package com.neroplan.backendUser.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TokenProvider {

    public static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(1);
    public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

    private static final String ROLE = "role";
    // 토큰 종류 - refresh token을 API 호출(Authorization 헤더)에 쓰지 못하도록 구분 (backendPlan도 같은 값을 검사)
    private static final String TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    @Value("${jwt.key}")
    private String key;
    private SecretKey secretKey;

    @PostConstruct
    private void init() {
        secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
    }

    // OAuth 로그인 성공 시
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        return generateAccessToken(userPrincipal.getUserId(), role);
    }

    // refresh 시 - role은 DB의 최신 값을 사용
    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(ROLE, role)
                .claim(TYPE, TYPE_ACCESS)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_TTL.toMillis()))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // HttpOnly 쿠키로만 전달 - 사용자 id만 담고 권한은 refresh 시 DB에서 다시 읽음
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(TYPE, TYPE_REFRESH)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_TTL.toMillis()))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(claims.get(ROLE).toString()));
        Long userId = Long.valueOf(claims.getSubject());
        return new UsernamePasswordAuthenticationToken(userId, token, authorities);
    }

    // API 호출용 access token인지 검사
    public boolean validateToken(String token) {
        return parseValid(token, TYPE_ACCESS).isPresent();
    }

    // 유효한 refresh token이면 사용자 id
    public Optional<Long> getUserIdFromRefreshToken(String token) {
        return parseValid(token, TYPE_REFRESH).map(claims -> Long.valueOf(claims.getSubject()));
    }

    private Optional<Claims> parseValid(String token, String expectedType) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        try {
            Claims claims = parseClaims(token);
            if (claims.getExpiration() == null || !claims.getExpiration().after(new Date())) {
                return Optional.empty();
            }
            if (!expectedType.equals(claims.get(TYPE, String.class))) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
