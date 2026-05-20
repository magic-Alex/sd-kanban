package com.sdkanban.dashboard.dto;

import java.util.List;

public record ProjectStatsResponse(
    Long projectId,
    int totalTaskCount,
    int doneTaskCount,
    int openTaskCount,
    List<SprintProgressResponse> sprintProgress,
    List<TaskTypeDistributionResponse> taskTypeDistribution,
    List<MemberWorkloadResponse> memberWorkload
) {
}
