package com.github.oldlabauth.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.github.oldlabauth.entity.Role;
import com.github.oldlabauth.entity.Users;

public record UsersSecurity(
        UUID id,
        String email,
        Role role,
        Boolean isActive,
        Boolean isNotBlocked,
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
        return email;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive);
    }

    @Override
    public boolean isAccountNonLocked() {
        return Boolean.TRUE.equals(isNotBlocked);
    }

    @Override
    public String getPassword() {
        return password;
    }
    public static UsersSecurity fromUser(Users entity) {
        if (entity == null) {
            throw new IllegalArgumentException("User entity cannot be null");
        }

        return new UsersSecurity(
                entity.getId(),
                entity.getEmail(),
                entity.getRoleEnum(),
                entity.getIsActive(),
                entity.getIsNotBlocked(),
                entity.getPassword()
        );
    }
}
