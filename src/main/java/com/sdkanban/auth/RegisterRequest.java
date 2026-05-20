package com.sdkanban.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 64) String account,
    @NotBlank @Size(max = 100) String nickname,
    @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 6, max = 100) String password
) {
}
