package com.github.storeauth.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.github.storeauth.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserIdempotencyKey(UUID userIdempotencyKey);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :cutoffDate")
    void deleteOlderThan(Instant cutoffDate);
}
