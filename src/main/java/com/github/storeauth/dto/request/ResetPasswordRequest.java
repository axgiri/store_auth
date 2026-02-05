package com.github.storeauth.dto.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    String contact,

    boolean isEmail,

    @NotNull(message = "otp cannot be null")
    @Digits(integer = 4, fraction = 0, message = "otp must be 4 digits")
    int otpReset,

    @NotNull(message = "new password cannot be null")
    @Size(min = 6, max = 32, message = "password must be between 6 and 32 characters")
    String newPassword) {}
