package com.sdkanban.dashboard.dto;

import com.sdkanban.user.dto.UserSummary;

public record MemberWorkloadResponse(
    UserSummary user,
    int openTaskCount,
    int doneTaskCount,
    int totalTaskCount
) {
}
