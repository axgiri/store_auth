package com.github.storeauth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.github.storeauth.dto.request.LoginRequest;
import com.github.storeauth.dto.request.RefreshRequest;
import com.github.storeauth.dto.response.AuthResponse;
import com.github.storeauth.entity.User;
import com.github.storeauth.entity.UserAdapter;
import com.github.storeauth.exception.AccountBlockedException;
import com.github.storeauth.exception.AccountNotActivatedException;
import com.github.storeauth.exception.UserNotFoundException;
import com.github.storeauth.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final ObjectProvider<AuthenticationManager> authenticationManagerProvider;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;

    private static final String USER_NOT_FOUND_BY_EMAIL = "User not found with email: ";

    public AuthResponse authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_BY_EMAIL + request.email()));

        if (!user.isActive()) {
            throw new AccountNotActivatedException("Account is not activated");
        }
        if (!user.isNotBlocked()) {
            throw new AccountBlockedException("Account is blocked");
        }

        authenticationManagerProvider.getObject()
                .authenticate(new UsernamePasswordAuthenticationToken(user.getIdempotencyKey(), request.password()));

        var userDetails = UserAdapter.fromUser(user);
        String token = tokenService.generateToken(userDetails);
        String refreshToken = refreshTokenService.issue(user);
        log.info("user {} authenticated successfully", user.getIdempotencyKey());
        return new AuthResponse(token, refreshToken);
    }

    public AuthResponse refreshAccessToken(RefreshRequest refreshToken) {
        var rotated = refreshTokenService.rotate(refreshToken.refreshToken());
        var user = rotated.person();
        var userDetails = UserAdapter.fromUser(user);

        String access = tokenService.generateToken(userDetails);

        return new AuthResponse(access, rotated.token());
    }

    public void revoke(RefreshRequest refreshToken) {
        refreshTokenService.revoke(refreshToken.refreshToken());
    }

    public void revokeAll(RefreshRequest refreshToken) {
        refreshTokenService.revokeAllForPerson(refreshToken.refreshToken());
    }
}
