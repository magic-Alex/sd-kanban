package com.sdkanban.task.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskPositionRequest(
    @NotNull
    Long columnId,

    @NotNull
    @Min(0)
    Integer sortOrder
) {
}
