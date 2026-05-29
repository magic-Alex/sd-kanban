package com.sdkanban.task.dto;

import com.sdkanban.user.dto.UserSummary;

import java.time.LocalDateTime;

public record TaskActivityResponse(
    Long id,
    Long taskId,
    UserSummary actor,
    String actionType,
    String fieldName,
    String oldValue,
    String newValue,
    String displayText,
    LocalDateTime createdAt
) {
}
