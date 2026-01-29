package com.github.oldlabauth.web;

import com.github.oldlabauth.dto.request.LoginRequest;
import com.github.oldlabauth.dto.request.RefreshRequest;
import com.github.oldlabauth.dto.request.ResetPasswordRequest;
import com.github.oldlabauth.dto.request.UpdatePasswordRequest;
import com.github.oldlabauth.dto.request.UserCreateRequest;
import com.github.oldlabauth.dto.response.AuthResponse;
import com.github.oldlabauth.service.AuthenticationService;
import com.github.oldlabauth.service.PasswordService;
import com.github.oldlabauth.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;
    private final PasswordService passwordService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> create(@RequestBody @Valid UserCreateRequest request) {
        registrationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
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
    
    //TODO: @PreAuthorize("@accessControlService.isSelfByEmail(authentication, #request.email)")
    @PutMapping("/update/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        log.debug("Password update attempt for email: {}", request.email());
        passwordService.updatePassword(request);
        return ResponseEntity.noContent().build();
    }

    // @PreAuthorize("@accessControlService.isSelf(authentication, #id) or @accessControlService.isAdmin(authentication)")
    @DeleteMapping("/delete/{idempotencyKey}")
    public ResponseEntity<Void> delete(@PathVariable UUID idempotencyKey) {
        log.debug("Delete attempt for userId: {}", idempotencyKey);
        registrationService.delete(idempotencyKey);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        log.debug("Password reset attempt for email: {}", request.contact());
        passwordService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}