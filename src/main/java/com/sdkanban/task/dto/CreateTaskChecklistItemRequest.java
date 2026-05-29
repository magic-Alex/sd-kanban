package com.sdkanban.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskChecklistItemRequest(
    @NotBlank
    @Size(max = 200)
    String title
) {
}
