package com.github.storeauth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.github.storeauth.entity.RefreshToken;
import com.github.storeauth.entity.User;
import com.github.storeauth.exception.InvalidTokenException;
import com.github.storeauth.exception.NoSuchAlgorithmException;
import com.github.storeauth.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    
    @Value("${jwt.refresh-ttl-days:30}")
    private int refreshExpiresDays;

    @Value("${jwt.refresh-token-secret}")
    private String refreshTokenSecret;
    
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
        verifySignature(token);
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
        verifySignature(token);
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
        UUID userId = getUserFromToken(refreshToken).getIdempotencyKey();
        repository.findByUserIdempotencyKey(userId).forEach(rt -> {
            rt.setRevoked(true);
            repository.save(rt);
        });
    }
    
    public User getUserFromToken(String rawToken) {
        verifySignature(rawToken);
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
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String signature = sign(payload);
        return payload + "." + signature;
    }

    private String sign(String payload) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(refreshTokenSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] signatureBytes = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Failed to sign refresh token", e);
        }
    }

    private void verifySignature(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            throw new InvalidTokenException("invalid token format");
        }
        String payload = parts[0];
        String signature = parts[1];
        String expectedSignature = sign(payload);
        
        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidTokenException("invalid refresh token signature");
        }
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
