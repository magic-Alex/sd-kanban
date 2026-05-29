package com.sdkanban.task.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderTaskChecklistItemsRequest(
    @NotEmpty
    List<Long> itemIds
) {
}
