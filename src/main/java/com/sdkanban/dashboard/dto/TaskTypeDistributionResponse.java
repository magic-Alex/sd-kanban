package com.sdkanban.dashboard.dto;

public record TaskTypeDistributionResponse(
    String type,
    int count
) {
}
