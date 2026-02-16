package com.github.storeauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.github.storeauth.dto.request.UserCreateRequest;
import com.github.storeauth.entity.Role;
import com.github.storeauth.entity.User;
import com.github.storeauth.exception.UserAlreadyExistsException;
import com.github.storeauth.exception.UserNotFoundException;
import com.github.storeauth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationService registrationService;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("saves user with encoded password and USER role")
        void savesUser_whenEmailUnique() {
            var key = UUID.randomUUID();
            var request = new UserCreateRequest(key, "test@example.com", "password", Role.USER);

            when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password")).thenReturn("encoded");

            registrationService.create(request);

            verify(userRepository).save(argThat(user ->
                    user.getIdempotencyKey().equals(key)
                    && user.getEmail().equals("test@example.com")
                    && user.getPassword().equals("encoded")
                    && user.getRoleEnum() == Role.USER
                    && !user.isActive()
                    && user.isNotBlocked()
            ));
        }

        @Test
        @DisplayName("throws UserAlreadyExistsException when email taken")
        void throws_whenEmailExists() {
            var request = new UserCreateRequest(UUID.randomUUID(), "existing@example.com", "pass", Role.USER);
            when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(new User()));

            assertThatThrownBy(() -> registrationService.create(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("already exists");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("validateEmail")
    class ValidateEmail {

        @Test
        @DisplayName("returns true when email not taken")
        void returnsTrue_whenEmailFree() {
            when(userRepository.existsByEmail("free@test.com")).thenReturn(false);
            assertThat(registrationService.validateEmail("free@test.com")).isTrue();
        }

        @Test
        @DisplayName("returns false when email already exists")
        void returnsFalse_whenEmailTaken() {
            when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);
            assertThat(registrationService.validateEmail("taken@test.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("activateUser")
    class ActivateUser {

        @Test
        @DisplayName("sets isActive to true and saves")
        void activatesUser() {
            var user = User.builder().email("u@test.com").isActive(false).build();
            when(userRepository.findByEmail("u@test.com")).thenReturn(Optional.of(user));

            registrationService.activateUser("u@test.com");

            assertThat(user.isActive()).isTrue();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("throws UserNotFoundException when email not found")
        void throws_whenUserMissing() {
            when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> registrationService.activateUser("missing@test.com"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deletes user by idempotency key")
        void deletesUser() {
            var key = UUID.randomUUID();
            var user = User.builder().idempotencyKey(key).build();
            when(userRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(user));

            registrationService.delete(key);

            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("throws UserNotFoundException when key not found")
        void throws_whenKeyMissing() {
            var key = UUID.randomUUID();
            when(userRepository.findByIdempotencyKey(key)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> registrationService.delete(key))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
