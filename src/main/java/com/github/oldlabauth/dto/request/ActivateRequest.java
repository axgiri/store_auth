package com.github.oldlabauth.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record ActivateRequest(
    @NotNull(message = "email cannot be null")
    @Email(message = "email must be valid")
    String email,

    @NotNull(message = "otp cannot be null")
    @Digits(integer = 4, fraction = 0, message = "otp must be 4 digits")
    int otp
) {}
