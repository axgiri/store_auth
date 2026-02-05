package com.github.storeauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.storeauth.dto.request.UserCreateRequest;
import com.github.storeauth.entity.Role;
import com.github.storeauth.entity.User;
import com.github.storeauth.exception.UserAlreadyExistsException;
import com.github.storeauth.exception.UserNotFoundException;
import com.github.storeauth.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String USER_NOT_FOUND_BY_EMAIL = "User not found with email: ";

    @Transactional
    public void create(UserCreateRequest request) {
        log.debug("Creating user with email: {} and idempotencyKey: {}", request.email(), request.idempotencyKey());

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        var user = User.builder()
                .idempotencyKey(request.idempotencyKey())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roleEnum(Role.USER)
                .isActive(false)
                .isNotBlocked(true)
                .build();

        userRepository.save(user);
    }

    public void activateUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_BY_EMAIL + email));
        user.setActive(true);
        userRepository.save(user);
        log.debug("Activated user with email: {}", email);
    }

    public void delete(UUID idempotencyKey) {
        User user = userRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + idempotencyKey));
        userRepository.delete(user);
        log.debug("Deleted user with id: {}", idempotencyKey);
    }

    @Transactional
    public void cleanupUnactivatedUsers() {
        Instant cutoffDate = Instant.now().minusSeconds(60L * 60 * 24 * 7); // 7 days
        int count = userRepository.deleteByIsActiveFalseAndCreatedAtBefore(cutoffDate);
        //TODO: call ol_client to delete related data
        log.info("Deleted {} unactivated users created before {}", count, cutoffDate);
    }
}
