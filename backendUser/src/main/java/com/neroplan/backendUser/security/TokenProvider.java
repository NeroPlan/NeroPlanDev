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
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TokenProvider {

    @Value("${jwt.key}")
    private String key;
    private SecretKey secretKey;
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 1000 * 60 * 60;
    private static final String ROLE = "role";

    @PostConstruct
    private void init() {
        secretKey =
                Keys.hmacShaKeyFor(
                        key.getBytes()
                );
    }

    public String generateAccessToken(
            Authentication authentication
    ) {
        Date now = new Date();

        Date expiredDate =
                new Date(
                        now.getTime()
                                + ACCESS_TOKEN_EXPIRE_TIME
                );
        UserPrincipal userPrincipal =
                (UserPrincipal) authentication.getPrincipal();

        Long userId =
                userPrincipal.getUserId();
        String role =
                authentication.getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority();

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(
                        ROLE,
                        role
                )
                .setIssuedAt(now)
                .setExpiration(expiredDate)
                .signWith(
                        secretKey,
                        SignatureAlgorithm.HS512
                )
                .compact();
    }

    public Authentication getAuthentication(
            String token
    ) {

        Claims claims =
                parseClaims(token);

        List<SimpleGrantedAuthority>
                authorities =
                Collections.singletonList(
                        new SimpleGrantedAuthority(
                                claims.get(
                                        ROLE
                                ).toString()
                        )
                );
        Long userId =
                Long.valueOf(claims.getSubject());

        return new UsernamePasswordAuthenticationToken(
                userId,
                token,
                authorities
        );
    }

    public boolean validateToken(
            String token
    ) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            Claims claims =
                    parseClaims(token);
            return claims.getExpiration()
                    .after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(
            String token
    ) {

        return Jwts.parserBuilder()
                .setSigningKey(
                        secretKey
                )
                .build()
                .parseClaimsJws(
                        token
                )
                .getBody();
    }
}