package com.sdkanban.task.dto;

import jakarta.validation.constraints.Size;

public record CreateTaskChecklistItemRequest(
    @Size(max = 200)
    String title
) {
}
