package com.github.oldlabauth.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.github.oldlabauth.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);


    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.isActive = :isActive WHERE u.email = :email")
    int setActiveByEmail(@Param("email") String email, @Param("isActive") boolean isActive);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM User u WHERE u.isActive = false AND u.createdAt < :cutoffDate")
    int deleteByIsActiveFalseAndCreatedAtBefore(@Param("cutoffDate") Instant cutoffDate);
}
