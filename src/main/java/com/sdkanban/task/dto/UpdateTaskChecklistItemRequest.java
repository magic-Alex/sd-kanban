package com.sdkanban.task.dto;

import jakarta.validation.constraints.Size;

public record UpdateTaskChecklistItemRequest(
    @Size(max = 200)
    String title
) {
}
