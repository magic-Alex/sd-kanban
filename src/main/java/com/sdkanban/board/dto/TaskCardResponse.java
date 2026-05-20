package com.sdkanban.board.dto;

import com.sdkanban.task.entity.Task;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaskCardResponse(
    Long id,
    Long projectId,
    Long sprintId,
    Long columnId,
    Long assigneeId,
    String title,
    String taskType,
    String priority,
    BigDecimal storyPoints,
    LocalDate dueDate,
    Integer sortOrder
) {
    public static TaskCardResponse from(Task task) {
        return new TaskCardResponse(
            task.getId(),
            task.getProjectId(),
            task.getSprintId(),
            task.getColumnId(),
            task.getAssigneeId(),
            task.getTitle(),
            task.getTaskType(),
            task.getPriority(),
            task.getStoryPoints(),
            task.getDueDate(),
            task.getSortOrder()
        );
    }
}
