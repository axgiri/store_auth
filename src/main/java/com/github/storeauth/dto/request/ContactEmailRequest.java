package com.github.storeauth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ContactEmailRequest(
    @NotBlank(message = "email cannot be blank")
    @Email(message = "email must be valid")
    String email
) {}
