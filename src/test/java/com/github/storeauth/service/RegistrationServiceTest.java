package com.github.storeauth.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.github.storeauth.dto.request.UserCreateRequest;
import com.github.storeauth.entity.Role;
import com.github.storeauth.entity.User;
import com.github.storeauth.exception.UserAlreadyExistsException;
import com.github.storeauth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService registrationService;

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
        registrationService.create(request);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when creating user with existing email")
    void shouldThrowException_WhenEmailExists() {
        String email = "existing@example.com";
        var request = new UserCreateRequest(UUID.randomUUID(), email, "pass", Role.USER);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User()));
        assertThatThrownBy(() -> registrationService.create(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("already exists");
    }
}
