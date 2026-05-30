package com.sdkanban.task.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdatePersonalTaskPositionRequest(
    @NotBlank
    @Pattern(regexp = "^[A-Z0-9_]{2,60}$")
    String targetTemplateKey,

    @NotNull
    @Min(0)
    Integer sortOrder
) {
}
