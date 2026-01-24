package com.github.oldlabauth.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.oldlabauth.dto.request.ActivateRequest;
import com.github.oldlabauth.dto.response.AuthResponse;
import com.github.oldlabauth.service.MessageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/email")
public class MessageController {

    private final MessageService service;

    @PostMapping("/activate")
    public ResponseEntity<Void> activate(@Valid @RequestBody ActivateRequest request) {
        log.debug("activating account with email: {}", request.email());
        service.setActive(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send/email/{email}")
    public ResponseEntity<Void> sendOtp(@PathVariable String email) {
        log.debug("sending OTP to email: {}", email);
        service.sendOtp(email);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/resend/email/{email}")
    public ResponseEntity<Void> resendOtp(@PathVariable String email) {
        log.debug("resending OTP to email: {}", email);
        service.resendOtp(email);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody ActivateRequest request) {
        log.debug("logging in user with email: {}", request.email());
        AuthResponse response = service.login(request.email(), request.otp());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send/login/{email}")
    public ResponseEntity<Void> sendLoginOtp(@PathVariable String email) {
        log.debug("sending login OTP to email: {}", email);
        service.sendLoginOtp(email);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
