package com.github.oldlabauth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ActivateRequest(
    @NotNull(message = "email cannot be null")
    @Email(message = "email must be valid")
    String email,

    @NotNull(message = "otp cannot be null")
    @Pattern(regexp = "\\d{4}")
    int otp
) {}
