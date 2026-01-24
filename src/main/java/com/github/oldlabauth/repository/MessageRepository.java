package com.github.oldlabauth.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.github.oldlabauth.entity.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long>{

    Optional<Message> findTopByEmailOrderByCreatedAtDesc(String email);

    Optional<Message> findTopByEmailAndIsLoginOrderByCreatedAtDesc(String email, boolean isLogin);
    
    @Transactional
    Optional<Message> findByEmailAndOtpResetAndIsActive(String email, int otp, boolean isActive);

    boolean existsByEmail(String email);

    @Modifying
    @Query("DELETE FROM Message a WHERE a.createdAt < :cutoffDate")
    void deleteOlderThan(@Param("cutoffDate") Instant cutoffDate);

}

