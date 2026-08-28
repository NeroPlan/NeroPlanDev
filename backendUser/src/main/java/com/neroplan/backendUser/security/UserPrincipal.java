package com.neroplan.backendUser.security;

import com.neroplan.backendUser.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class UserPrincipal implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    public UserPrincipal(
            User user,
            Map<String, Object> attributes
    ) {
        this.user = user;
        this.attributes = attributes;
    }
    public Long getUserId() {
        return user.getUser_id();
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority(
                        user.getRole().getKey()
                )
        );
    }

    @Override
    public String getName() {
        return user.getEmail();
    }
}