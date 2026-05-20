package com.sdkanban.task.dto;

import jakarta.validation.constraints.NotBlank;

public record AddTaskCommentRequest(
    @NotBlank
    String content
) {
}
