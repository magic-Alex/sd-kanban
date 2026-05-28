package com.sdkanban.board.dto;

import com.sdkanban.task.entity.Task;
import com.sdkanban.user.dto.UserSummary;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaskCardResponse(
    Long id,
    Long projectId,
    Long sprintId,
    Long columnId,
    Long assigneeId,
    UserSummary assignee,
    String title,
    String taskType,
    String priority,
    BigDecimal storyPoints,
    LocalDate dueDate,
    Integer sortOrder
) {
    public static TaskCardResponse from(Task task, UserSummary assignee) {
        return new TaskCardResponse(
            task.getId(),
            task.getProjectId(),
            task.getSprintId(),
            task.getColumnId(),
            task.getAssigneeId(),
            assignee,
            task.getTitle(),
            task.getTaskType(),
            task.getPriority(),
            task.getStoryPoints(),
            task.getDueDate(),
            task.getSortOrder()
        );
    }
}
