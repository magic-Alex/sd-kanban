package com.sdkanban.dashboard.dto;

import java.util.List;

public record DashboardSummaryResponse(
    int pendingTaskCount,
    int overdueTaskCount,
    int ownedProjectCount,
    int joinedProjectCount,
    List<DashboardActivityResponse> recentActivities
) {
}
