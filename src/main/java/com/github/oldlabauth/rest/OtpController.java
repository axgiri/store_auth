package com.github.oldlabauth.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.oldlabauth.dto.request.ContactEmailRequest;
import com.github.oldlabauth.dto.request.OtpValidationRequest;
import com.github.oldlabauth.dto.response.AuthResponse;
import com.github.oldlabauth.service.OtpService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/otp")
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/email/activation/send")
    public ResponseEntity<Void> sendActivationOtp(@Valid @RequestBody ContactEmailRequest request) {
        log.debug("Sending activation OTP to: {}", request);
        otpService.sendActivationOtp(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/email/activation/resend")
    public ResponseEntity<Void> resendActivationOtp(@Valid @RequestBody ContactEmailRequest request) {
        log.debug("Resending activation OTP to: {}", request);
        otpService.resendActivationOtp(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/email/activate")
    public ResponseEntity<Void> activateAccount(@Valid @RequestBody OtpValidationRequest request) {
        log.debug("Activating account for: {}", request.contact());
        otpService.activateAccount(request.contact(), request.otp());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/login/send/{email}")
    public ResponseEntity<Void> sendLoginOtp(@PathVariable @Email(message = "Invalid email format") String email) {
        log.debug("Sending login OTP to: {}", email);
        otpService.sendLoginOtp(email);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/email/login")
    public ResponseEntity<AuthResponse> loginWithOtp(@Valid @RequestBody OtpValidationRequest request) {
        log.debug("OTP login for: {}", request.contact());
        AuthResponse response = otpService.loginWithOtp(request.contact(), request.otp());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/email/password-reset/send/{email}")
    public ResponseEntity<Void> sendPasswordResetOtp(@PathVariable @Email(message = "Invalid email format") String email) {
        log.debug("Sending password reset OTP to: {}", email);
        otpService.sendPasswordResetOtp(email);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
