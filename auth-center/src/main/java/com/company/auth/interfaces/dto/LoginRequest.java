package com.company.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "Login name must not be blank")
    @Size(max = 64, message = "Login name must not exceed 64 characters")
    String loginName,
    @NotBlank(message = "Password must not be blank")
    @Size(max = 255, message = "Password must not exceed 255 characters")
    String password
) {
}
