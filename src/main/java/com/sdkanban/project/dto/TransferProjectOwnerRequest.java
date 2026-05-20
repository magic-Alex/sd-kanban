package com.sdkanban.project.dto;

import jakarta.validation.constraints.NotNull;

public record TransferProjectOwnerRequest(
    @NotNull
    Long userId
) {
}
