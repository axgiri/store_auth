package com.github.storeauth.service;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.github.storeauth.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserRepository userRepository;

    public boolean isSelfByEmail(Authentication authentication, String email) {
        try {
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
}
