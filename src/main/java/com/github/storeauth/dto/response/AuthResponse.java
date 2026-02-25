package com.github.storeauth.dto.response;

public record AuthResponse(
    String accessToken,
    String refreshToken
) {}
