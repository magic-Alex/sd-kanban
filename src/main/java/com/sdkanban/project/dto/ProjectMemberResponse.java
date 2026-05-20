package com.sdkanban.project.dto;

import com.sdkanban.user.dto.UserSummary;

import java.time.LocalDateTime;

public record ProjectMemberResponse(
    UserSummary user,
    String role,
    LocalDateTime joinedAt
) {
}
