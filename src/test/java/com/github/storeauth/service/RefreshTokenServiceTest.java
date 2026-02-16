package com.github.storeauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.github.storeauth.entity.RefreshToken;
import com.github.storeauth.entity.User;
import com.github.storeauth.exception.InvalidTokenException;
import com.github.storeauth.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository repository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private static final String SECRET = "test-secret-key-for-hmac-signing-32chars!";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpiresDays", 30);
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenSecret", SECRET);
    }

    private User testUser() {
        return User.builder()
                .idempotencyKey(UUID.randomUUID())
                .email("user@test.com")
                .build();
    }

    @Nested
    @DisplayName("issue")
    class Issue {

        @Test
        @DisplayName("saves hashed token and returns raw token with dot-separated format")
        void issuesToken() {
            var user = testUser();
            when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            String raw = refreshTokenService.issue(user);

            assertThat(raw).contains(".");
            assertThat(raw.split("\\.")).hasSize(2);

            var captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(repository).save(captor.capture());

            RefreshToken saved = captor.getValue();
            assertThat(saved.getTokenHash()).isNotBlank();
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.isRevoked()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(Instant.now());
        }
    }

    @Nested
    @DisplayName("rotate")
    class Rotate {

        @Test
        @DisplayName("revokes current token and issues a new one")
        void rotatesToken() {
            var user = testUser();
            String rawToken = refreshTokenService.issue(user);

            // Capture the saved token to set up mock for rotate
            var captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(repository).save(captor.capture());
            RefreshToken savedToken = captor.getValue();
            savedToken.setId(1L);

            when(repository.findByTokenHash(savedToken.getTokenHash()))
                    .thenReturn(Optional.of(savedToken));
            when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> {
                RefreshToken rt = inv.getArgument(0);
                if (rt.getId() == null) rt.setId(2L); // new token
                return rt;
            });

            var rotated = refreshTokenService.rotate(rawToken);

            assertThat(rotated.person()).isEqualTo(user);
            assertThat(rotated.token()).contains(".");
            assertThat(rotated.token()).isNotEqualTo(rawToken);
            assertThat(savedToken.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("throws InvalidTokenException when token not found")
        void throws_whenTokenNotFound() {
            String rawToken = refreshTokenService.issue(testUser());
            // Clear the mock to set up not-found scenario
            when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("throws InvalidTokenException for expired token")
        void throws_whenTokenExpired() {
            var user = testUser();
            String rawToken = refreshTokenService.issue(user);

            var captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(repository).save(captor.capture());
            RefreshToken savedToken = captor.getValue();
            savedToken.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));

            when(repository.findByTokenHash(savedToken.getTokenHash()))
                    .thenReturn(Optional.of(savedToken));

            assertThatThrownBy(() -> refreshTokenService.rotate(rawToken))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    @Nested
    @DisplayName("revoke")
    class Revoke {

        @Test
        @DisplayName("marks token as revoked")
        void revokesToken() {
            var user = testUser();
            String rawToken = refreshTokenService.issue(user);

            var captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(repository).save(captor.capture());
            RefreshToken savedToken = captor.getValue();

            when(repository.findByTokenHash(savedToken.getTokenHash()))
                    .thenReturn(Optional.of(savedToken));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            refreshTokenService.revoke(rawToken);

            assertThat(savedToken.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("throws InvalidTokenException when revoking already revoked token")
        void throws_whenAlreadyRevoked() {
            var user = testUser();
            String rawToken = refreshTokenService.issue(user);

            var captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(repository).save(captor.capture());
            RefreshToken savedToken = captor.getValue();
            savedToken.setRevoked(true); // already revoked

            when(repository.findByTokenHash(savedToken.getTokenHash()))
                    .thenReturn(Optional.of(savedToken));

            assertThatThrownBy(() -> refreshTokenService.revoke(rawToken))
                    .isInstanceOf(InvalidTokenException.class);
        }
    }

    @Nested
    @DisplayName("revokeAllForPerson")
    class RevokeAllForPerson {

        @Test
        @DisplayName("revokes all tokens for the user")
        void revokesAll() {
            var user = testUser();
            String rawToken = refreshTokenService.issue(user);

            var captor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(repository).save(captor.capture());
            RefreshToken savedToken = captor.getValue();

            when(repository.findByTokenHash(savedToken.getTokenHash()))
                    .thenReturn(Optional.of(savedToken));

            RefreshToken rt1 = new RefreshToken();
            rt1.setUser(user);
            rt1.setRevoked(false);
            RefreshToken rt2 = new RefreshToken();
            rt2.setUser(user);
            rt2.setRevoked(false);

            when(repository.findByUserIdempotencyKey(user.getIdempotencyKey()))
                    .thenReturn(List.of(rt1, rt2));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            refreshTokenService.revokeAllForPerson(rawToken);

            assertThat(rt1.isRevoked()).isTrue();
            assertThat(rt2.isRevoked()).isTrue();
        }
    }

    @Nested
    @DisplayName("signature verification")
    class SignatureVerification {

        @Test
        @DisplayName("throws InvalidTokenException for tampered token")
        void throws_whenSignatureTampered() {
            String rawToken = refreshTokenService.issue(testUser());
            String tampered = rawToken.substring(0, rawToken.indexOf('.')) + ".tampered-signature";

            assertThatThrownBy(() -> refreshTokenService.revoke(tampered))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("signature");
        }

        @Test
        @DisplayName("throws InvalidTokenException for malformed token (no dot)")
        void throws_whenMalformed() {
            assertThatThrownBy(() -> refreshTokenService.revoke("no-dot-here"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("format");
        }
    }
}
