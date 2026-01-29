package com.github.oldlabauth.entity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UserAdapter(
        UUID id,
        Role role,
        boolean isActive,
        boolean isNotBlocked,
        String password
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return id.toString();
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isNotBlocked;
    }

    @Override
    public String getPassword() {
        return password;
    }
    public static UserAdapter fromUser(User entity) {
        if (entity == null) {
            throw new IllegalArgumentException("User entity cannot be null");
        }

        return new UserAdapter(
                entity.getIdempotencyKey(),
                entity.getRoleEnum(),
                entity.isActive(),
                entity.isNotBlocked(),
                entity.getPassword()
        );
    }
}
