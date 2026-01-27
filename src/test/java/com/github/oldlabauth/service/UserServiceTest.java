package com.github.oldlabauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.github.oldlabauth.dto.request.LoginRequest;
import com.github.oldlabauth.dto.request.UserCreateRequest;
import com.github.oldlabauth.dto.response.AuthResponse;
import com.github.oldlabauth.entity.Role;
import com.github.oldlabauth.entity.User;
import com.github.oldlabauth.entity.UserAdapter;
import com.github.oldlabauth.exception.AccountNotActivatedException;
import com.github.oldlabauth.exception.UserAlreadyExistsException;
import com.github.oldlabauth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ObjectProvider<AuthenticationManager> authenticationManagerProvider;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenService tokenService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private OtpService otpService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Should create user successfully when email is unique")
    void shouldCreateUserSuccessfully() {
        String email = "test@example.com";
        String password = "password";
        String encodedPassword = "encodedPassword";
        UUID idempotencyKey = UUID.randomUUID();
        var request = new UserCreateRequest(idempotencyKey, email, password, Role.USER);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        userService.create(request);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when creating user with existing email")
    void shouldThrowException_WhenEmailExists() {
        String email = "existing@example.com";
        var request = new UserCreateRequest(UUID.randomUUID(), email, "pass", Role.USER);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User()));
        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("already exists");
    }

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

        AuthResponse response = userService.authenticate(request);

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
        assertThatThrownBy(() -> userService.authenticate(request))
                .isInstanceOf(AccountNotActivatedException.class);
    }
}
