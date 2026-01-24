package com.github.oldlabauth.dto.response;

public record AuthResponse(
    String accessToken,
    String refreshToken
) {}
