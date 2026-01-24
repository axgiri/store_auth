package com.github.oldlabauth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ContactRequest(
    @NotBlank(message = "email cannot be blank")
    @Email(message = "email must be valid")
    String email
) {}
