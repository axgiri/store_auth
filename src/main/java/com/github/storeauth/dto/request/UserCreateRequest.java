package com.github.storeauth.dto.request;

import java.util.UUID;

import com.github.storeauth.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
    @NotNull(message = "idempotencyKey cannot be null")
    UUID idempotencyKey,
    
    @NotNull(message = "email cannot be null")
    @Email(message = "email should be valid")
    String email,
    
    @Size(min = 6, message = "password must be at least 6 characters long")
    @NotBlank(message = "password cannot be blank")
    String password,
    
    Role role
) {}
