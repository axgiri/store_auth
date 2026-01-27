package com.github.oldlabauth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefreshRequest(
    @NotBlank(message = "refresh token cannot be blank")
    @NotNull(message = "refresh token cannot be null")
    String refreshToken) {}
