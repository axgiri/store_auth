package com.github.storeauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.github.storeauth.dto.request.LoginRequest;
import com.github.storeauth.dto.request.RefreshRequest;
import com.github.storeauth.dto.response.AuthResponse;
import com.github.storeauth.entity.Role;
import com.github.storeauth.entity.User;
import com.github.storeauth.entity.UserAdapter;
import com.github.storeauth.exception.AccountBlockedException;
import com.github.storeauth.exception.AccountNotActivatedException;
import com.github.storeauth.exception.UserNotFoundException;
import com.github.storeauth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ObjectProvider<AuthenticationManager> authenticationManagerProvider;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenService tokenService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User activeUser(String email) {
        return User.builder()
                .idempotencyKey(UUID.randomUUID())
                .email(email)
                .password("encoded")
                .isActive(true)
                .isNotBlocked(true)
                .roleEnum(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("authenticate")
    class Authenticate {

        @Test
        @DisplayName("returns access and refresh tokens for valid active user")
        void returnsTokens_whenUserActiveAndCredentialsValid() {
            var email = "active@example.com";
            var request = new LoginRequest(email, "password");
            var user = activeUser(email);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(authenticationManagerProvider.getObject()).thenReturn(authenticationManager);
            when(tokenService.generateToken(any(UserAdapter.class))).thenReturn("access-jwt");
            when(refreshTokenService.issue(user)).thenReturn("refresh-tok");

            AuthResponse response = authenticationService.authenticate(request);

            assertThat(response.accessToken()).isEqualTo("access-jwt");
            assertThat(response.refreshToken()).isEqualTo("refresh-tok");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("throws UserNotFoundException when email not found")
        void throws_whenEmailNotFound() {
            var request = new LoginRequest("ghost@example.com", "pass");
            when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("throws AccountNotActivatedException when user inactive")
        void throws_whenUserNotActivated() {
            var email = "inactive@example.com";
            var request = new LoginRequest(email, "pass");
            var user = User.builder().email(email).isActive(false).isNotBlocked(true).build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(AccountNotActivatedException.class);
        }

        @Test
        @DisplayName("throws AccountBlockedException when user is blocked")
        void throws_whenUserBlocked() {
            var email = "blocked@example.com";
            var request = new LoginRequest(email, "pass");
            var user = User.builder().email(email).isActive(true).isNotBlocked(false).build();

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(AccountBlockedException.class);
        }

        @Test
        @DisplayName("propagates BadCredentialsException from AuthenticationManager")
        void propagates_whenBadCredentials() {
            var email = "user@example.com";
            var request = new LoginRequest(email, "wrong");
            var user = activeUser(email);

            when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            when(authenticationManagerProvider.getObject()).thenReturn(authenticationManager);
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("bad credentials"));

            assertThatThrownBy(() -> authenticationService.authenticate(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("refreshAccessToken")
    class RefreshAccessToken {

        @Test
        @DisplayName("returns new access token after rotating refresh token")
        void returnsNewAccessToken() {
            var rawRefresh = "payload.signature";
            var user = activeUser("user@test.com");
            var rotatedRaw = "new-payload.new-sig";

            when(refreshTokenService.rotate(rawRefresh))
                    .thenReturn(new RefreshTokenService.RotatedToken(user, rotatedRaw));
            when(tokenService.generateToken(any(UserAdapter.class))).thenReturn("new-access");

            AuthResponse response = authenticationService.refreshAccessToken(new RefreshRequest(rawRefresh));

            assertThat(response.accessToken()).isEqualTo("new-access");
            assertThat(response.refreshToken()).isEqualTo(rotatedRaw);
        }
    }

    @Nested
    @DisplayName("revoke / revokeAll")
    class Revocation {

        @Test
        @DisplayName("delegates single revocation to RefreshTokenService")
        void revokeDelegates() {
            var token = "tok.sig";
            authenticationService.revoke(new RefreshRequest(token));
            verify(refreshTokenService).revoke(token);
        }

        @Test
        @DisplayName("delegates revokeAll to RefreshTokenService")
        void revokeAllDelegates() {
            var token = "tok.sig";
            authenticationService.revokeAll(new RefreshRequest(token));
            verify(refreshTokenService).revokeAllForPerson(token);
        }
    }
}
