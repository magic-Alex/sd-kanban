package com.sdkanban.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank @Size(max = 64) String account,
    @NotBlank @Size(max = 100) String nickname,
    @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 1, max = 100) String password,
    @Size(max = 32) String role
) {
}
