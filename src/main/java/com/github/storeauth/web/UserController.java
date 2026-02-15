package com.github.storeauth.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.storeauth.dto.request.LoginRequest;
import com.github.storeauth.dto.request.RefreshRequest;
import com.github.storeauth.dto.request.ResetPasswordRequest;
import com.github.storeauth.dto.request.UpdatePasswordRequest;
import com.github.storeauth.dto.response.AuthResponse;
import com.github.storeauth.service.AuthenticationService;
import com.github.storeauth.service.PasswordService;
import com.github.storeauth.service.RegistrationService;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;
    private final PasswordService passwordService;

    @GetMapping("/is-email-available")
    public ResponseEntity<Boolean> validateEmail(@RequestParam String email) {
        return ResponseEntity.ok(registrationService.validateEmail(email));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Login attempt for email: {}", request.email());
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PutMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        log.debug("Token refresh attempt");
        return ResponseEntity.ok(authenticationService.refreshAccessToken(request));
    }

    @PutMapping("/revoke")
    public ResponseEntity<Void> revoke(@Valid @RequestBody RefreshRequest request) {
        log.debug("Token revoke attempt");
        authenticationService.revoke(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/revoke/all")
    public ResponseEntity<Void> revokeAll(@Valid @RequestBody RefreshRequest request) {
        log.debug("Revoke all tokens attempt");
        authenticationService.revokeAll(request);
        return ResponseEntity.noContent().build();
    }
    
    @PreAuthorize("@accessControlService.isSelfByEmail(authentication, #request.email)")
    @PutMapping("/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        log.debug("Password update attempt for email: {}", request.email());
        passwordService.updatePassword(request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal(expression = "claims['sub']") String userId) {
        log.debug("Delete attempt for userId: {}", userId);
        registrationService.delete(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        log.debug("Password reset attempt for email: {}", request.contact());
        passwordService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}