package com.github.storeauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.github.storeauth.dto.request.ResetPasswordRequest;
import com.github.storeauth.dto.request.UpdatePasswordRequest;
import com.github.storeauth.entity.User;
import com.github.storeauth.exception.UserNotFoundException;
import com.github.storeauth.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    private static final String USER_NOT_FOUND_BY_EMAIL = "User not found with email: ";

    public void updatePassword(UpdatePasswordRequest request, UUID userId) {
        log.debug("updating password for userId: {}", userId);
        User user = userRepository.findByIdempotencyKey(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        verifyPassword(request.oldPassword(), user.getPassword());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.debug("updated password for userId: {}", userId);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void resetPassword(ResetPasswordRequest request) {
        log.debug("Resetting password for: {}", request.contact());

        otpService.validatePasswordResetOtp(request.contact(), request.otpReset());

        if (!request.isEmail()) {
            throw new IllegalArgumentException("You must reset password via email, phone reset is not implemented yet");
        }

        User user = userRepository.findByEmail(request.contact())
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_BY_EMAIL + request.contact()));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        log.debug("Password reset successfully for: {}", request.contact());
    }

    private void verifyPassword(String raw, String encoded) {
        if (!passwordEncoder.matches(raw, encoded)) {
            throw new BadCredentialsException("invalid credentials");
        }
    }
}
