package com.sdkanban.task.dto;

import java.util.List;

public record ReorderTaskChecklistItemsRequest(
    List<Long> itemIds
) {
}
