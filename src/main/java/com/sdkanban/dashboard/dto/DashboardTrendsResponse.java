package com.sdkanban.dashboard.dto;

import java.util.List;

public record DashboardTrendsResponse(
    List<CompletionTrendBucket> buckets
) {
}
