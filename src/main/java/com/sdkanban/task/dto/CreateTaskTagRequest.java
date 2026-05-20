package com.sdkanban.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskTagRequest(
    @NotBlank
    @Size(max = 60)
    String name,

    @Size(max = 20)
    String color
) {
}
