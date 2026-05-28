package com.sdkanban.task.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateTaskRequest(
    @Size(max = 200)
    String title,
    String description,
    @Size(max = 32)
    String taskType,
    @Size(max = 32)
    String priority,
    @DecimalMin("0.0")
    BigDecimal storyPoints,
    @DecimalMin("0.0")
    BigDecimal estimatedHours,
    LocalDate dueDate,
    String acceptanceCriteria,
    Long assigneeId,
    Long sprintId,
    Long columnId,
    List<String> clearFields
) {
}
