package com.sdkanban.task.dto;

import com.sdkanban.task.entity.TaskTag;

public record TaskTagResponse(
    Long id,
    Long projectId,
    String name,
    String color
) {
    public static TaskTagResponse from(TaskTag tag) {
        return new TaskTagResponse(tag.getId(), tag.getProjectId(), tag.getName(), tag.getColor());
    }
}
