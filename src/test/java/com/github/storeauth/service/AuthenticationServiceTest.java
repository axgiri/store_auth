package com.github.storeauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.github.storeauth.dto.request.LoginRequest;
import com.github.storeauth.dto.response.AuthResponse;
import com.github.storeauth.entity.Role;
import com.github.storeauth.entity.User;
import com.github.storeauth.entity.UserAdapter;
import com.github.storeauth.exception.AccountNotActivatedException;
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

    @Test
    @DisplayName("Should authenticate successfully and return tokens")
    void shouldAuthenticateSuccessfully() {
        String email = "active@example.com";
        String password = "password";
        LoginRequest request = new LoginRequest(email, password);
        
        User user = User.builder()
                .email(email)
                .isActive(true)
                .isNotBlocked(true)
                .roleEnum(Role.USER)
                .build();
        
        String accessToken = "access.token.jwt";
        String refreshToken = "refresh.token";

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(authenticationManagerProvider.getObject()).thenReturn(authenticationManager);
        when(tokenService.generateToken(any(UserAdapter.class))).thenReturn(accessToken);
        when(refreshTokenService.issue(user)).thenReturn(refreshToken);

        AuthResponse response = authenticationService.authenticate(request);

        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(refreshToken);
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should throw exception when authenticating inactive user")
    void shouldThrowException_WhenUserNotActive() {
        String email = "inactive@example.com";
        LoginRequest request = new LoginRequest(email, "pass");
        
        User user = User.builder()
                .email(email)
                .isActive(false)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(AccountNotActivatedException.class);
    }
}
