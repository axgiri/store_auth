package com.github.storeauth.service;

import com.github.storeauth.entity.Role;
import com.github.storeauth.entity.User;
import com.github.storeauth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @InjectMocks
    private AccessControlService service;

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void isSelfByEmail_returnsTrue_whenTokenAndUserValid() {
        var email = "noreply@axgiri.tech";
        var userId = UUID.randomUUID();

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("sub")).thenReturn(userId.toString());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = mock(User.class);
        when(user.getEmail()).thenReturn(email);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));


        boolean result = service.isSelfByEmail(email);
        Assertions.assertTrue(result);
    }

    @Test
    void isSelfByEmail_returnsFalse_whenTokenEmpty() {
        var email = "noreply@axgiri.tech";
        SecurityContextHolder.getContext().setAuthentication(null);

        boolean result = service.isSelfByEmail(email);
        Assertions.assertFalse(result);
    }

    @Test
    void isSelfByEmail_returnsFalse_whenUserNotFound() {
        var email = "noreply@axgiri.tech";
        var userId = UUID.randomUUID();

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("sub")).thenReturn(userId.toString());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Assertions.assertFalse(service.isSelfByEmail(email));
    }

    @Test
    void isModerator_returnsTrue_whenRoleIsAdmin() {
        var userId = UUID.randomUUID();
        var role = Role.ADMIN;

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("sub")).thenReturn(userId.toString());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = mock(User.class);
        when(user.getRoleEnum()).thenReturn(role);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Assertions.assertTrue(service.isModerator());
    }

    @Test
    void isModerator_returnsTrue_whenRoleIsModerator() {
        var role = Role.MODERATOR;
        var userId = UUID.randomUUID();

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("sub")).thenReturn(userId.toString());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = mock(User.class);
        when(user.getRoleEnum()).thenReturn(role);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Assertions.assertTrue(service.isModerator());
    }

    @Test
    void isModerator_returnsFalse_whenTokenEmpty() {
        SecurityContextHolder.getContext().setAuthentication(null);
        Assertions.assertFalse(service.isModerator());
    }

    @Test
    void isModerator_returnsFalse_whenUserNotFount() {
        var userId = UUID.randomUUID();

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("sub")).thenReturn(userId.toString());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Assertions.assertFalse(service.isModerator());
    }

    @Test
    void isModerator_returnsFalse_whenRoleIsNotModeratorOrAdmin() {
        var userId = UUID.randomUUID();

        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaimAsString("sub")).thenReturn(userId.toString());
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = mock(User.class);
        when(user.getRoleEnum()).thenReturn(Role.USER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Assertions.assertFalse(service.isModerator());
    }
}