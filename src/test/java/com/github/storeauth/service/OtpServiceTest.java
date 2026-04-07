package com.github.storeauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.github.storeauth.dto.request.ContactEmailRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.storeauth.dto.MessageChannelEnum;
import com.github.storeauth.dto.request.OtpRequest;
import com.github.storeauth.dto.request.OtpValidationRequest;
import com.github.storeauth.dto.response.AuthResponse;
import com.github.storeauth.entity.OtpType;
import com.github.storeauth.entity.Role;
import com.github.storeauth.entity.User;
import com.github.storeauth.exception.InvalidOtpException;
import com.github.storeauth.exception.UserNotFoundException;
import com.github.storeauth.repository.OtpRedisRepository;
import com.github.storeauth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock private OtpRedisRepository otpRepository;
    @Mock private UserRepository userRepository;
    @Mock private TokenService tokenService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private EventService eventService;

    @InjectMocks
    private OtpService otpService;

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        @DisplayName("saves OTP to redis and dispatches event")
        void savesAndSends() {
            var request = OtpRequest.email("u@test.com", OtpType.LOGIN);

            otpService.send(request);

            verify(otpRepository).save(eq(MessageChannelEnum.EMAIL), eq("u@test.com"), eq(OtpType.LOGIN), anyInt());
            verify(eventService).sendOtp(eq(MessageChannelEnum.EMAIL), eq("u@test.com"), anyInt());
        }
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        @DisplayName("passes when OTP matches and deletes entry")
        void passes_whenOtpMatches() {
            var request = OtpValidationRequest.forEmailActivation("u@test.com", 1234);
            when(otpRepository.find(MessageChannelEnum.EMAIL, "u@test.com", OtpType.ACTIVATE_ACCOUNT))
                    .thenReturn(Optional.of(1234));

            otpService.validate(request);

            verify(otpRepository).delete(MessageChannelEnum.EMAIL, "u@test.com", OtpType.ACTIVATE_ACCOUNT);
        }

        @Test
        @DisplayName("throws InvalidOtpException when OTP not found (expired)")
        void throws_whenOtpNotFound() {
            var request = OtpValidationRequest.forEmailActivation("u@test.com", 1234);
            when(otpRepository.find(any(), any(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpService.validate(request))
                    .isInstanceOf(InvalidOtpException.class)
                    .hasMessageContaining("not found or expired");
        }

        @Test
        @DisplayName("throws InvalidOtpException when OTP code is wrong")
        void throws_whenOtpMismatch() {
            var request = OtpValidationRequest.forEmailActivation("u@test.com", 9999);
            when(otpRepository.find(MessageChannelEnum.EMAIL, "u@test.com", OtpType.ACTIVATE_ACCOUNT))
                    .thenReturn(Optional.of(1234));

            assertThatThrownBy(() -> otpService.validate(request))
                    .isInstanceOf(InvalidOtpException.class)
                    .hasMessageContaining("Invalid OTP");
        }
    }

    @Nested
    @DisplayName("resend")
    class Resend {

        @Test
        @DisplayName("re-dispatches existing OTP without creating new one")
        void reusesExisting() {
            var request = OtpRequest.email("u@test.com", OtpType.LOGIN);
            when(otpRepository.find(MessageChannelEnum.EMAIL, "u@test.com", OtpType.LOGIN))
                    .thenReturn(Optional.of(5678));

            otpService.resend(request);

            verify(eventService).sendOtp(MessageChannelEnum.EMAIL, "u@test.com", 5678);
            verify(otpRepository, never()).save(any(), any(), any(), anyInt());
        }

        @Test
        @DisplayName("creates new OTP when no existing found")
        void createsNew_whenNoneExists() {
            var request = OtpRequest.email("u@test.com", OtpType.LOGIN);
            when(otpRepository.find(MessageChannelEnum.EMAIL, "u@test.com", OtpType.LOGIN))
                    .thenReturn(Optional.empty());

            otpService.resend(request);

            verify(otpRepository).save(eq(MessageChannelEnum.EMAIL), eq("u@test.com"), eq(OtpType.LOGIN), anyInt());
        }
    }

    @Nested
    @DisplayName("activateAccount")
    class ActivateAccount {

        @Test
        @DisplayName("validates OTP and activates the user")
        void activatesUser() {
            var email = "u@test.com";
            var user = User.builder().email(email).isActive(false).roleEnum(Role.USER).build();

            when(otpRepository.find(MessageChannelEnum.EMAIL, email, OtpType.ACTIVATE_ACCOUNT))
                    .thenReturn(Optional.of(1234));
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            otpService.activateAccount(email, 1234);

            assertThat(user.isActive()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user email missing after OTP validation")
        void throws_whenUserNotFound() {
            var email = "ghost@test.com";
            when(otpRepository.find(MessageChannelEnum.EMAIL, email, OtpType.ACTIVATE_ACCOUNT))
                    .thenReturn(Optional.of(1234));
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> otpService.activateAccount(email, 1234))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("loginWithOtp")
    class LoginWithOtp {

        @Test
        @DisplayName("validates OTP and returns tokens")
        void returnsTokens() {
            var email = "u@test.com";
            var user = User.builder().email(email).isActive(true).roleEnum(Role.USER).build();

            when(otpRepository.find(MessageChannelEnum.EMAIL, email, OtpType.LOGIN))
                    .thenReturn(Optional.of(4321));
            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(tokenService.generateToken(any())).thenReturn("access");
            when(refreshTokenService.issue(user)).thenReturn("refresh");

            AuthResponse response = otpService.loginWithOtp(email, 4321);

            assertThat(response.accessToken()).isEqualTo("access");
            assertThat(response.refreshToken()).isEqualTo("refresh");
        }
    }

    @Nested
    @DisplayName("sendActivationOtp / sendLoginOtp / sendPasswordResetOtp")
    class SendSpecific {

        @Test
        @DisplayName("sendActivationOtp throws when user not found")
        void activationOtp_throwsWhenNoUser() {
            when(userRepository.existsByEmail("x@t.com")).thenReturn(false);

            var req = new ContactEmailRequest("x@t.com");
            assertThatThrownBy(() -> otpService.sendActivationOtp(req))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found");

        }

        @Test
        @DisplayName("sendLoginOtp throws when user not active")
        void loginOtp_throwsWhenNotActive() {
            var user = User.builder().email("u@t.com").isActive(false).build();
            when(userRepository.findByEmail("u@t.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> otpService.sendLoginOtp("u@t.com"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("generateOtp")
    class GenerateOtp {

        @Test
        @DisplayName("generates 4-digit OTP in range [1000, 9999]")
        void generates4DigitOtp() {
            for (int i = 0; i < 100; i++) {
                int otp = otpService.generateOtp();
                assertThat(otp).isBetween(1000, 9999);
            }
        }
    }
}
