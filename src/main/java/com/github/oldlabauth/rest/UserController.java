package com.github.oldlabauth.rest;

import com.github.oldlabauth.dto.request.ContactRequest;
import com.github.oldlabauth.dto.request.LoginRequest;
import com.github.oldlabauth.dto.request.RefreshRequest;
import com.github.oldlabauth.dto.request.ResetPasswordRequest;
import com.github.oldlabauth.dto.request.UpdatePasswordRequest;
import com.github.oldlabauth.dto.request.UserCreateRequest;
import com.github.oldlabauth.dto.response.AuthResponse;
import com.github.oldlabauth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> create(@RequestBody @Valid UserCreateRequest request) {
        userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Login attempt for email: {}", request.email());
        return ResponseEntity.ok(userService.authenticate(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        log.debug("Token refresh attempt");
        return ResponseEntity.ok(userService.refreshAccessToken(request));
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(@Valid @RequestBody RefreshRequest request) {
        log.debug("Token revoke attempt");
        userService.revoke(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/revoke/all")
    public ResponseEntity<Void> revokeAll(@Valid @RequestBody RefreshRequest request) {
        log.debug("Revoke all tokens attempt");
        userService.revokeAll(request);
        return ResponseEntity.noContent().build();
    }
    
    //TODO: @PreAuthorize("@accessControlService.isSelfByEmail(authentication, #request.email)")
    @PutMapping("/update/password")
    public ResponseEntity<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        log.debug("Password update attempt for email: {}", request.email());
        userService.updatePassword(request);
        return ResponseEntity.noContent().build();
    }

    // @PreAuthorize("@accessControlService.isSelf(authentication, #id) or @accessControlService.isAdmin(authentication)")
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<Void> delete(@PathVariable Long userId) {
        log.debug("Delete attempt for userId: {}", userId);
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody ContactRequest request){
        log.debug("Password reset request for email: {}", request.email());
        userService.requestPasswordReset(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        log.debug("Password reset attempt for email: {}", request.contact());
        userService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}