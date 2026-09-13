package com.neroplan.backendPlan.global.apiPayload.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
public class TokenProvider {

    private static final String ROLE = "role";

    @Value("${jwt.key}")
    private String key;

    private SecretKey secretKey;

    @PostConstruct
    private void init() {
        secretKey = Keys.hmacShaKeyFor(
                key.getBytes(StandardCharsets.UTF_8)
        );
    }

    public Authentication getAuthentication(
            String token
    ) {
        Claims claims = parseClaims(token);

        String role = claims.get(
                ROLE,
                String.class
        );

        List<SimpleGrantedAuthority> authorities =
                Collections.singletonList(
                        new SimpleGrantedAuthority(role)
                );

        Long userId = Long.valueOf(
                claims.getSubject()
        );

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
            Claims claims = parseClaims(token);

            return claims.getExpiration() != null
                    && claims.getExpiration()
                    .after(new Date());

        } catch (JwtException
                 | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(
            String token
    ) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}