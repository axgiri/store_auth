package com.github.oldlabauth.service;

import com.github.oldlabauth.dto.request.UserCreateRequest;
import com.github.oldlabauth.entity.Role;
import com.github.oldlabauth.entity.User;
import com.github.oldlabauth.exception.UserAlreadyExistsException;
import com.github.oldlabauth.exception.UserNotFoundException;
import com.github.oldlabauth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
