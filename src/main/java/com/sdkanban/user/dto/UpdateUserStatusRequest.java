package com.sdkanban.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserStatusRequest(
    @NotBlank @Size(max = 32) String status
) {
}
