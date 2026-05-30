package com.sdkanban.board.dto;

import com.sdkanban.board.entity.BoardColumn;
import com.sdkanban.project.entity.Project;
import com.sdkanban.task.entity.Task;
import com.sdkanban.user.dto.UserSummary;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaskCardResponse(
    Long id,
    Long projectId,
    String projectCode,
    String projectName,
    String projectColor,
    Long sprintId,
    Long columnId,
    String columnTemplateKey,
    Long assigneeId,
    UserSummary assignee,
    String title,
    String taskType,
    String priority,
    BigDecimal storyPoints,
    LocalDate dueDate,
    Integer sortOrder,
    long checklistDoneCount,
    long checklistTotalCount
) {
    public static TaskCardResponse from(
        Task task,
        Project project,
        BoardColumn column,
        UserSummary assignee,
        long checklistDoneCount,
        long checklistTotalCount
    ) {
        return new TaskCardResponse(
            task.getId(),
            task.getProjectId(),
            project == null ? null : project.getProjectCode(),
            project == null ? null : project.getName(),
            project == null ? null : project.getProjectColor(),
            task.getSprintId(),
            task.getColumnId(),
            column == null ? null : column.getTemplateKey(),
            task.getAssigneeId(),
            assignee,
            task.getTitle(),
            task.getTaskType(),
            task.getPriority(),
            task.getStoryPoints(),
            task.getDueDate(),
            task.getSortOrder(),
            checklistDoneCount,
            checklistTotalCount
        );
    }
}
