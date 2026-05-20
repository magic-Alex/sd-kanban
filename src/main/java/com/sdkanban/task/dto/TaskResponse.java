package com.sdkanban.task.dto;

import com.sdkanban.task.entity.Task;
import com.sdkanban.user.dto.UserSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskResponse(
    Long id,
    Long projectId,
    Long sprintId,
    Long columnId,
    UserSummary assignee,
    UserSummary creator,
    String title,
    String description,
    String taskType,
    String priority,
    BigDecimal storyPoints,
    BigDecimal estimatedHours,
    LocalDate dueDate,
    String acceptanceCriteria,
    Integer sortOrder,
    List<TaskTagResponse> tags,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TaskResponse from(
        Task task,
        UserSummary assignee,
        UserSummary creator,
        List<TaskTagResponse> tags
    ) {
        return new TaskResponse(
            task.getId(),
            task.getProjectId(),
            task.getSprintId(),
            task.getColumnId(),
            assignee,
            creator,
            task.getTitle(),
            task.getDescription(),
            task.getTaskType(),
            task.getPriority(),
            task.getStoryPoints(),
            task.getEstimatedHours(),
            task.getDueDate(),
            task.getAcceptanceCriteria(),
            task.getSortOrder(),
            tags,
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
