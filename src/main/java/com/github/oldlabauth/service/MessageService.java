package com.github.oldlabauth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.oldlabauth.dto.MessageChannelEnum;
import com.github.oldlabauth.dto.request.ActivateRequest;
import com.github.oldlabauth.dto.response.AuthResponse;
import com.github.oldlabauth.entity.Message;
import com.github.oldlabauth.exception.InvalidOtpException;
import com.github.oldlabauth.exception.UserNotFoundException;
import com.github.oldlabauth.mapper.UserMapper;
import com.github.oldlabauth.repository.MessageRepository;
import com.github.oldlabauth.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class MessageService {
    private final MessageRepository repository;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final EventService eventService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final Random random = new Random();

    @Value("${app.activation-ttl-minutes}")
    private int otpExpirationMinutes;

    private static final String OTP_NOT_FOUND_BY_EMAIL = "no OTP found for email: ";

    @Transactional
    public void setActive(ActivateRequest request) {
        log.debug("saving to activate with email: {}", request.email());
        Message message = repository.findTopByEmailOrderByCreatedAtDesc(request.email())
            .orElseThrow(() -> new UserNotFoundException(OTP_NOT_FOUND_BY_EMAIL + request.email()));
        Instant createdAt = message.getCreatedAt();
        Instant expiration = createdAt.plus(Duration.ofMinutes(otpExpirationMinutes));
        if (Instant.now().isAfter(expiration)) {
            throw new UserNotFoundException(
                    "OTP expired for email: " + request.email()
            );
        }

        if (message.getOtp() != request.otp()) {
            throw new UserNotFoundException("invalid OTP for email: " + request.email());
        }

        if (message.isActive()) {
            throw new UserNotFoundException("user with email: " + request.email() + " already activated");
        }

        delete(request.email());

        userRepository.setActiveByEmail(request.email(), true);
    }

    public int setOtp() {
        log.debug("generating otp");
        return 1000 + random.nextInt(9000);
    }
    
    public void sendOtp(String email) {
        eventService.sendOtp(MessageChannelEnum.EMAIL, email, getOtp(email));
    }

    public void sendOtpReset(String email) {
        eventService.sendOtp(MessageChannelEnum.EMAIL, email, getOtpReset(email));
    }

    public void save(String email, Optional<Boolean> isLogin) {
        log.debug("saving to activate with email: {}, loginAttempted={}", email, isLogin);
        int otp = setOtp();
        Instant createdAt = Instant.now();
        Message message = Message.builder()
            .email(email)
            .otp(otp)
            .isActive(false)
            .isLogin(isLogin.orElse(false))
            .createdAt(createdAt)
            .build();
        repository.save(message);
    }

    public void saveForRegister(String email) {
        save(email, Optional.ofNullable(null));
    }

    public void saveForLogin(String email) {
        save(email, Optional.of(true));
    }

    public int getOtp(String email) {
        log.debug("getting otp");
        return repository.findTopByEmailOrderByCreatedAtDesc(email)
            .orElseThrow(() -> new UserNotFoundException(OTP_NOT_FOUND_BY_EMAIL + email))
            .getOtp();
    }

    public int getOtpReset(String email) {
        log.debug("getting otp for reset");
        return repository.findTopByEmailOrderByCreatedAtDesc(email)
            .orElseThrow(() -> new UserNotFoundException(OTP_NOT_FOUND_BY_EMAIL + email))
            .getOtpReset();
    }

    public void resendOtp(String email) {
        log.debug("resending OTP to email: {}", email);
        Message message = repository.findTopByEmailOrderByCreatedAtDesc(email)
            .orElseThrow(() -> new UserNotFoundException("user with email: " + email + " not found, please register first"));

        Instant createdAt = message.getCreatedAt();
        Instant expiration = createdAt.plus(Duration.ofMinutes(otpExpirationMinutes));
        if (Instant.now().isAfter(expiration)) {
            saveForRegister(email);
        }
        eventService.sendOtp(MessageChannelEnum.EMAIL,email, getOtp(email));
    }

    public AuthResponse login(ActivateRequest request) {
        log.debug("logging by otp in user with email: {}", request.email());
        Message message = repository.findTopByEmailAndIsLoginOrderByCreatedAtDesc(request.email(), true)
            .orElseThrow(() -> new UserNotFoundException("users with email: " + request.email() + " not found, please send OTP first"));
        Instant createdAt = message.getCreatedAt();
        Instant expiration = createdAt.plus(Duration.ofMinutes(otpExpirationMinutes));
        if (Instant.now().isAfter(expiration)) {
            throw new UserNotFoundException("OTP expired for email: " + request.email());
        }
        if (message.getOtp() != request.otp()) {
            throw new UserNotFoundException("invalid OTP for email: " + request.email());
        }

        delete(request.email());

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UserNotFoundException("person not found with email: " + request.email()));
        String token = tokenService.generateToken(userMapper.toAdapter(user));
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(token, refreshToken);
    }

    public void delete(String email) {
        log.debug("deleting activation with email: {}", email);
        Message message = repository.findTopByEmailOrderByCreatedAtDesc(email)
            .orElseThrow(() -> new UserNotFoundException("activation with email: " + email + " not found"));
        repository.delete(message);
    }

    public void sendLoginOtp(String email) {
        log.debug("sending login OTP to email: {}", email);
        saveForLogin(email);
        eventService.sendOtp(MessageChannelEnum.EMAIL,email, getOtp(email));
    }

    public AuthResponse login(String email, int otp) {
        log.debug("logging by otp in user with email: {}", email);
        Message message = repository.findTopByEmailAndIsLoginOrderByCreatedAtDesc(email, true)
            .orElseThrow(() -> new UserNotFoundException("users with email: " + email + " not found, please send OTP first"));
        Instant createdAt = message.getCreatedAt();
        Instant expiration = createdAt.plus(Duration.ofMinutes(otpExpirationMinutes));
        if (Instant.now().isAfter(expiration)) {
            throw new UserNotFoundException("OTP expired for email: " + email);
        }
        if (message.getOtp() != otp) {
            throw new UserNotFoundException("invalid OTP for email: " + email);
        }

        delete(email);

        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("person not found with email: " + email));
        var userDetails = userMapper.toAdapter(user);
        String token = tokenService.generateToken(userDetails);
        String refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(token, refreshToken);
    }

    public void saveOtpReset(String email, int otp, boolean isForLogin) {
    Message message = repository.findTopByEmailOrderByCreatedAtDesc(email)
        .orElse(new Message());
        message.setEmail(email);
        message.setOtpReset(otp);
        message.setActive(true);
        message.setLogin(isForLogin);
        message.setCreatedAt(Instant.now());

        repository.save(message);
    }

    @Transactional
    public void validateOtpReset(String email, int otp) {
        Message message = repository.findByEmailAndOtpResetAndIsActive(email, otp, true)
                .orElseThrow(() -> new InvalidOtpException("Invalid OTP"));

        Instant createdAt = message.getCreatedAt();
        Instant expiration = createdAt.plus(Duration.ofMinutes(otpExpirationMinutes));
        if (Instant.now().isAfter(expiration)) {
            throw new InvalidOtpException("OTP expired");
        }
        message.setActive(false);
        repository.save(message);
    }
    
    @Transactional
    public void cleanupOldRecords() {
        Instant cutoffDate = Instant.now().minusSeconds(60L * 60 * 24);
        repository.deleteOlderThan(cutoffDate);
    }
}
