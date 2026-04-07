package com.github.storeauth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.github.storeauth.dto.request.ResetPasswordRequest;
import com.github.storeauth.dto.request.UpdatePasswordRequest;
import com.github.storeauth.entity.Role;
import com.github.storeauth.entity.User;
import com.github.storeauth.exception.UserNotFoundException;
import com.github.storeauth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private OtpService otpService;

    @InjectMocks
    private PasswordService passwordService;

    @Nested
    @DisplayName("updatePassword")
    class UpdatePassword {

        @Test
        @DisplayName("encodes and saves new password when old password matches")
        void updatesPassword() {
            var userId = UUID.randomUUID();
            var user = User.builder()
                    .idempotencyKey(userId)
                    .email("u@test.com")
                    .password("old-encoded")
                    .roleEnum(Role.USER)
                    .build();

            when(userRepository.findByIdempotencyKey(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("old-raw", "old-encoded")).thenReturn(true);
            when(passwordEncoder.encode("new-raw")).thenReturn("new-encoded");

            passwordService.updatePassword(new UpdatePasswordRequest("old-raw", "new-raw"), userId);

            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("throws BadCredentialsException when old password wrong")
        void throws_whenOldPasswordWrong() {
            var userId = UUID.randomUUID();
            var user = User.builder().idempotencyKey(userId).email("u@test.com").password("encoded").build();
            when(userRepository.findByIdempotencyKey(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

            var request = new UpdatePasswordRequest("wrong", "new");
            assertThatThrownBy(() ->
                    passwordService.updatePassword(request, userId))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user missing")
        void throws_whenUserMissing() {
            var userId = UUID.randomUUID();
            when(userRepository.findByIdempotencyKey(userId)).thenReturn(Optional.empty());

            var request = new UpdatePasswordRequest("old", "new");
            assertThatThrownBy(() ->
                    passwordService.updatePassword(request, userId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("validates OTP and resets password for email-based reset")
        void resetsPassword() {
            var request = new ResetPasswordRequest("u@test.com", true, 1234, "newPass");
            var user = User.builder().email("u@test.com").password("old").build();

            when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newPass")).thenReturn("newEncoded");

            passwordService.resetPassword(request);

            verify(otpService).validatePasswordResetOtp("u@test.com", 1234);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for non-email reset")
        void throws_whenNotEmail() {
            var request = new ResetPasswordRequest("+1234567890", false, 1234, "newPass");

            assertThatThrownBy(() -> passwordService.resetPassword(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("email");
        }
    }
}
