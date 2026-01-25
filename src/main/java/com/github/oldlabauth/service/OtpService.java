package com.github.oldlabauth.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Service;

import com.github.oldlabauth.dto.request.ContactEmailRequest;
import com.github.oldlabauth.dto.request.OtpRequest;
import com.github.oldlabauth.dto.request.OtpValidationRequest;
import com.github.oldlabauth.dto.response.AuthResponse;
import com.github.oldlabauth.entity.OtpType;
import com.github.oldlabauth.entity.UserAdapter;
import com.github.oldlabauth.exception.InvalidOtpException;
import com.github.oldlabauth.exception.UserNotFoundException;
import com.github.oldlabauth.repository.OtpRedisRepository;
import com.github.oldlabauth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRedisRepository otpRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final EventService eventService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public void send(OtpRequest request) {
        log.debug("Sending OTP: type={}, channel={}, contact={}", request.otpType(), request.channel(), maskContactForLogging(request.contact()));
        int otp = generateOtp();
        otpRepository.save(request.channel(), request.contact(), request.otpType(), otp);
        eventService.sendOtp(request.channel(), request.contact(), otp);
        log.debug("OTP sent successfully");
    }

    public void validate(OtpValidationRequest request) {
        log.debug("Validating OTP: type={}, channel={}, contact={}", request.otpType(), request.channel(), maskContactForLogging(request.contact()));
        int storedOtp = otpRepository.find(request.channel(), request.contact(), request.otpType())
                .orElseThrow(() -> new InvalidOtpException("OTP not found or expired for: " + maskContactForLogging(request.contact())));

        if (storedOtp != request.otp()) {
            log.warn("Invalid OTP attempt for: {}", maskContactForLogging(request.contact()));
            throw new InvalidOtpException("Invalid OTP code");
        }

        otpRepository.delete(request.channel(), request.contact(), request.otpType());
        log.debug("OTP validated and deleted successfully");
    }

    public void resend(OtpRequest request) {
        log.debug("Resend OTP request: type={}, channel={}, contact={}", request.otpType(), request.channel(), maskContactForLogging(request.contact()));

        var existingOtp = otpRepository.find(request.channel(), request.contact(), request.otpType());
        if (existingOtp.isPresent()) {
            eventService.sendOtp(request.channel(), request.contact(), existingOtp.get());
        } else {
            send(request);
        }
    }

    public void sendActivationOtp(ContactEmailRequest contactEmailRequest) {
        validateUserExists(contactEmailRequest.email());
        send(OtpRequest.email(contactEmailRequest.email(), OtpType.ACTIVATE_ACCOUNT));
    }

    public void resendActivationOtp(ContactEmailRequest contactEmailRequest) {
        validateUserExists(contactEmailRequest.email());
        resend(OtpRequest.email(contactEmailRequest.email(), OtpType.ACTIVATE_ACCOUNT));
    }

    public void activateAccount(String email, int otp) {
        log.info("Activating account for: {}", maskContactForLogging(email));
        
        validate(OtpValidationRequest.forEmailActivation(email, otp));
        userService.activateUser(email);
        
        log.info("Account activated successfully: {}", maskContactForLogging(email));
    }

    public void sendLoginOtp(String email) {
        validateUserExistsAndActive(email);
        send(OtpRequest.email(email, OtpType.LOGIN));
    }

    public AuthResponse loginWithOtp(String email, int otp) {
        log.info("OTP login attempt for: {}", maskContactForLogging(email));
        validate(OtpValidationRequest.forEmailLogin(email, otp));

        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + email));

        var userDetails = UserAdapter.fromUser(user);
        String accessToken = tokenService.generateToken(userDetails);
        String refreshToken = refreshTokenService.issue(user);

        log.info("OTP login successful for: {}", maskContactForLogging(email));
        return new AuthResponse(accessToken, refreshToken);
    }

    public void sendPasswordResetOtp(String email) {
        validateUserExistsAndActive(email);
        send(OtpRequest.email(email, OtpType.RESET_PASSWORD));
    }

    public void validatePasswordResetOtp(String email, int otp) {
        validate(OtpValidationRequest.forEmailActivation(email, otp));
    }

    public int generateOtp() {
        log.debug("generating otp");
        return 1000 + SECURE_RANDOM.nextInt(9000);
    }

    private void validateUserExists(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new UserNotFoundException("User not found: " + email);
        }
    }

    private void validateUserExistsAndActive(String email) {
        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + email));
        
        if (!user.isActive()) {
            throw new UserNotFoundException("User account is not activated: " + email);
        }
    }

    private String maskContactForLogging(String contact) {
        if (contact == null || contact.length() < 4) {
            return "***";
        }
        if (contact.contains("@")) {
            int atIndex = contact.indexOf('@');
            return contact.substring(0, Math.min(3, atIndex)) + "***" + contact.substring(atIndex);
        }
        return contact.substring(0, 3) + "***" + contact.substring(contact.length() - 2);
    }
}
