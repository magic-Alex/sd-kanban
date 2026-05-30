package com.sdkanban.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
    @NotBlank
    @Size(max = 120)
    String name,

    @Size(max = 1000)
    String description,

    @NotBlank
    @Size(max = 40)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-]{1,39}$")
    String projectCode,

    @NotBlank
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$")
    String projectColor
) {
}
