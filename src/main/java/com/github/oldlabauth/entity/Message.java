package com.github.oldlabauth.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "activates")
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "email", unique = true, nullable = false)
    @Email(message = "email must be valid")
    private String email;

    @Column(name = "otp", nullable = false)
    private int otp;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "is_login_attempted", nullable = true)
    private boolean isLogin;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "otp_reset", nullable = true)
    private int otpReset;

}
