package com.github.oldlabauth.service;

import com.github.oldlabauth.dto.request.ContactRequest;
import com.github.oldlabauth.dto.request.LoginRequest;
import com.github.oldlabauth.dto.request.RefreshRequest;
import com.github.oldlabauth.dto.request.ResetPasswordRequest;
import com.github.oldlabauth.dto.request.UpdatePasswordRequest;
import com.github.oldlabauth.dto.request.UserCreateRequest;
import com.github.oldlabauth.dto.response.AuthResponse;
import com.github.oldlabauth.entity.Role;
import com.github.oldlabauth.entity.User;
import com.github.oldlabauth.exception.UserAlreadyExistsException;
import com.github.oldlabauth.exception.UserNotFoundException;
import com.github.oldlabauth.mapper.UserMapper;
import com.github.oldlabauth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ObjectProvider<AuthenticationManager> authenticationManagerProvider;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final MessageService messageService;

    private static final String USER_NOT_FOUND_BY_EMAIL = "User not found with email: ";

    @Transactional
    public void create(UserCreateRequest request) {
        log.debug("Creating user with email: {} and id: {}", request.email(), request.id());

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        var user = User.builder()
                .id(request.id())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roleEnum(Role.USER)
                .isActive(false)
                .isNotBlocked(true)
                .build();

        userRepository.save(user);
    }

    public AuthResponse authenticate(LoginRequest request) {
        authenticationManagerProvider.getObject()
                .authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_BY_EMAIL + request.email()));

        var userDetails = userMapper.toAdapter(user);
        String token = tokenService.generateToken(userDetails);
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(token, refreshToken);
    }

    public AuthResponse refreshAccessToken(RefreshRequest refreshToken) {
        var rotated = refreshTokenService.rotate(refreshToken.refreshToken());
        var user = rotated.person();
        var userDetails = userMapper.toAdapter(user);

        String access = tokenService.generateToken(userDetails);

        return new AuthResponse(access, rotated.token());
    }

    public void revoke(RefreshRequest refreshToken) {
        refreshTokenService.revoke(refreshToken.refreshToken());
    }

    public void revokeAll(RefreshRequest refreshToken) {
        refreshTokenService.revokeAllForPerson(refreshToken.refreshToken());
    }

    public void updatePassword(UpdatePasswordRequest request) {
        log.debug("updating password for email: {}", request.email());
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_BY_EMAIL + request.email()));
        verifyPassword(request.oldPassword(), user.getPassword());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.debug("updated password for email: {}", request.email());
    }

    public void requestPasswordReset(ContactRequest contactRequest) {
        User user = userRepository.findByEmail(contactRequest.email())
                .orElseThrow(
                        () -> new UserNotFoundException(USER_NOT_FOUND_BY_EMAIL + contactRequest.email()));

        int otp = messageService.setOtp();
        messageService.saveOtpReset(user.getEmail(), otp, false);
        messageService.sendOtpReset(user.getEmail());
        log.debug("OTP sent to {}: {}", contactRequest.email(), otp);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void resetPassword(ResetPasswordRequest request) {
        log.debug("Resetting password for: {}", request.contact());

        messageService.validateOtpReset(request.contact(), request.otpReset());

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
    
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void delete(Long userId) {
        User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        userRepository.delete(user);
        log.debug("Deleted user with id: {}", userId);
    }

    @Transactional
    public void cleanupUnactivatedUsers() {
        Instant cutoffDate = Instant.now().minusSeconds(60L * 60 * 24 * 7); // 7 days
        int count = userRepository.deleteByIsActiveFalseAndCreatedAtBefore(cutoffDate);
        //TODO: call ol_client to delete related data
        log.info("Deleted {} unactivated users created before {}", count, cutoffDate);
    }
    
    private void verifyPassword(String raw, String encoded) {
        if (!passwordEncoder.matches(raw, encoded)) {
            throw new BadCredentialsException("invalid credentials");
        }
    }
} //TODO: migrations
