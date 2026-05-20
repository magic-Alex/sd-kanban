package com.sdkanban.dashboard.dto;

import com.sdkanban.user.dto.UserSummary;

import java.time.LocalDateTime;

public record DashboardActivityResponse(
    Long id,
    Long taskId,
    Long projectId,
    String projectName,
    String taskTitle,
    UserSummary actor,
    String actionType,
    String fieldName,
    String oldValue,
    String newValue,
    LocalDateTime createdAt
) {
}
