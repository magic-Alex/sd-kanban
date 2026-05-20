package com.sdkanban.dashboard.dto;

import java.time.LocalDate;

public record CompletionTrendBucket(
    LocalDate date,
    int completedCount
) {
}
