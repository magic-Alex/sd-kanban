package com.sdkanban.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank String account,
    @NotBlank String password
) {
}
