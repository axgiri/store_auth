package com.github.oldlabauth.entity;

import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users", 
    uniqueConstraints = @UniqueConstraint(columnNames = "email"),
    indexes = @Index(name = "idx_user_email", columnList = "email")
)
public class User{

    @Id
    private UUID idempotencyKey;

    @NotNull(message = "email cannot be null")
    @Email(message = "email should be valid")
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @NotNull(message = "password cannot be null")
    @NotBlank(message = "password cannot be blank")
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_enum")
    private Role roleEnum;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "is_not_blocked", nullable = false)
    private boolean isNotBlocked;

    @Version
    private Long version;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
