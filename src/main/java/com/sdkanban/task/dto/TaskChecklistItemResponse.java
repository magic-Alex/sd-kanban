package com.sdkanban.task.dto;

import com.sdkanban.task.entity.TaskChecklistItem;
import com.sdkanban.user.dto.UserSummary;

import java.time.LocalDateTime;

public record TaskChecklistItemResponse(
    Long id,
    Long taskId,
    Long projectId,
    String title,
    boolean done,
    Integer sortOrder,
    UserSummary createdBy,
    UserSummary completedBy,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TaskChecklistItemResponse from(
        TaskChecklistItem item,
        UserSummary createdBy,
        UserSummary completedBy
    ) {
        return new TaskChecklistItemResponse(
            item.getId(),
            item.getTaskId(),
            item.getProjectId(),
            item.getTitle(),
            item.isDone(),
            item.getSortOrder(),
            createdBy,
            completedBy,
            item.getCompletedAt(),
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }
}
