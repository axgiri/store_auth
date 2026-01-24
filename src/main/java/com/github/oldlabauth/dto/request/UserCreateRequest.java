package com.github.oldlabauth.dto.request;

import java.util.UUID;
import com.github.oldlabauth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
    @NotNull(message = "id cannot be null")
    UUID id,
    
    @NotNull(message = "email cannot be null")
    @Email(message = "email should be valid")
    String email,
    
    @NotBlank(message = "password cannot be blank")
    String password,
    
    Role role
) {}
