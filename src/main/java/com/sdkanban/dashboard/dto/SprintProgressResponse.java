package com.sdkanban.dashboard.dto;

public record SprintProgressResponse(
    Long sprintId,
    String name,
    int totalTaskCount,
    int doneTaskCount,
    int openTaskCount
) {
}
