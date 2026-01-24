package com.github.oldlabauth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.github.oldlabauth.entity.RefreshToken;
import com.github.oldlabauth.entity.User;
import com.github.oldlabauth.exception.InvalidTokenException;
import com.github.oldlabauth.exception.NoSuchAlgorithmException;
import com.github.oldlabauth.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    
    @Value("${jwt.refresh-ttl-days:30}")
    private int refreshExpiresDays;
    
    private static final String REFRESH_TOKEN_NOT_FOUND = "refresh token not found";
    private static final SecureRandom RANDOM = new SecureRandom();

    public record RotatedToken(User person, String token) {}

    @Transactional
    public String issue(User user) {
        String raw = generateToken();
        String tokenHash = hash(raw);

        RefreshToken refreshJWT = new RefreshToken();
        refreshJWT.setUser(user);
        refreshJWT.setTokenHash(tokenHash);
        refreshJWT.setExpiresAt(Instant.now().plus(refreshExpiresDays, ChronoUnit.DAYS));
        refreshJWT.setRevoked(false);
        
        repository.save(refreshJWT);
        return raw;
    }

    @Transactional
    public RotatedToken rotate(String token) {
        String currentHash = hash(token);

        RefreshToken currentToken = repository.findByTokenHash(currentHash)
            .orElseThrow(() -> new InvalidTokenException(REFRESH_TOKEN_NOT_FOUND));

        if (currentToken.isRevoked() || currentToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("refresh token invalid or expired");
        }

        String nextRaw = generateToken();
        String nextHash = hash(nextRaw);

        RefreshToken next = new RefreshToken();
        next.setUser(currentToken.getUser());
        next.setTokenHash(nextHash);
        next.setExpiresAt(Instant.now().plus(refreshExpiresDays, ChronoUnit.DAYS));
        next.setRevoked(false);
        next = repository.save(next);

        currentToken.setRevoked(true);
        currentToken.setReplacedByTokenId(next.getId());
        repository.save(currentToken);

        return new RotatedToken(next.getUser(), nextRaw);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void revoke(String token) {
        log.debug("revoking token");
        String hash = hash(token);
        RefreshToken rt = repository.findByTokenHash(hash)
            .orElseThrow(() -> new InvalidTokenException(REFRESH_TOKEN_NOT_FOUND));

        if(rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("refresh token already revoked or expired");
        }

        rt.setRevoked(true);
        repository.save(rt);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void revokeAllForPerson(String refreshToken) {
        UUID userId = getUserFromToken(refreshToken).getId();
        repository.findByUserId(userId).forEach(rt -> {
            rt.setRevoked(true);
            repository.save(rt);
        });
    }
    
    public User getUserFromToken(String rawToken) {
        String hash = hash(rawToken);
        RefreshToken rt = repository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException(REFRESH_TOKEN_NOT_FOUND));
        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now()))
            throw new InvalidTokenException("refresh token invalid or expired");
        return rt.getUser();
    }

    private String generateToken() {
        byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] refreshHash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(refreshHash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException("SHA-256 algorithm not found", e);
        }
    }

    @Transactional
    public void cleanupExpiredTokens() {
        repository.deleteOlderThan(Instant.now());
    }
}
