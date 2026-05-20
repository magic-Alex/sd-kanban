package com.sdkanban.task.dto;

import java.util.List;

public record UpdateTaskTagsRequest(
    List<Long> tagIds
) {
}
