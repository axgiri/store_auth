package com.github.storeauth.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.github.storeauth.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserRepository userRepository;

    public boolean isSelfByEmail(String email) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return false;
            }
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String userId = jwt.getClaimAsString("sub");
            UUID userIdUuid = UUID.fromString(userId);

            return userRepository.findById(userIdUuid)
                .map(user -> user.getEmail().equals(email))
                .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isModerator() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                return false;
            }
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String userId = jwt.getClaimAsString("sub");
            UUID userIdUuid = UUID.fromString(userId);

            return userRepository.findById(userIdUuid)
                .map(user -> {
                    String role = user.getRoleEnum().name();
                    return role.equals("MODERATOR") || role.equals("ADMIN");
                })
                .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }
}
