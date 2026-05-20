package com.sdkanban.project.dto;

import com.sdkanban.user.dto.UserSummary;

import java.time.LocalDateTime;

public record ProjectResponse(
    Long id,
    String name,
    String description,
    UserSummary owner,
    UserSummary creator,
    String status,
    long memberCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
