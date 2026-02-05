package com.github.storeauth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
    @NotNull(message = "email cannot be null")
    @Email(message = "email must be a valid email address")
    String email,

    @NotNull(message = "old_password cannot be null")
    @Size(min = 6, max = 32, message = "old_password must be between 6 and 32 characters")
    String oldPassword,

    @NotNull(message = "new_password cannot be null")
    @Size(min = 6, max = 32, message = "new_password must be between 6 and 32 characters")
    String newPassword) {}
