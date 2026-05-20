package com.sdkanban.dashboard.service;

import com.sdkanban.dashboard.dto.DashboardSummaryResponse;
import com.sdkanban.dashboard.dto.DashboardTrendsResponse;
import com.sdkanban.dashboard.dto.ProjectStatsResponse;
import com.sdkanban.dashboard.dto.SprintProgressResponse;

public interface DashboardService {
    DashboardSummaryResponse summary(Long currentUserId);

    DashboardTrendsResponse trends(Long currentUserId);

    ProjectStatsResponse projectStats(Long projectId, Long currentUserId);

    SprintProgressResponse sprintStats(Long projectId, Long sprintId, Long currentUserId);
}
