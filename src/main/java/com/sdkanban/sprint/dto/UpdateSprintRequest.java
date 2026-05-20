package com.sdkanban.sprint.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateSprintRequest(
    @Size(max = 120)
    String name,

    @Size(max = 1000)
    String goal,

    LocalDate startDate,
    LocalDate endDate,

    @Size(max = 32)
    String status
) {
}
