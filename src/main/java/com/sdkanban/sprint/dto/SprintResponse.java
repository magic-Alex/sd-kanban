package com.sdkanban.sprint.dto;

import com.sdkanban.sprint.entity.Sprint;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SprintResponse(
    Long id,
    Long projectId,
    String name,
    String goal,
    LocalDate startDate,
    LocalDate endDate,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static SprintResponse from(Sprint sprint) {
        return new SprintResponse(
            sprint.getId(),
            sprint.getProjectId(),
            sprint.getName(),
            sprint.getGoal(),
            sprint.getStartDate(),
            sprint.getEndDate(),
            sprint.getStatus(),
            sprint.getCreatedAt(),
            sprint.getUpdatedAt()
        );
    }
}
