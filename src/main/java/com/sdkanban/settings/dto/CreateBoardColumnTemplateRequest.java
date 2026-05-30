package com.sdkanban.settings.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBoardColumnTemplateRequest(
    @NotBlank
    @Pattern(regexp = "^[A-Z0-9_]{2,60}$")
    String templateKey,

    @NotBlank
    @Size(max = 80)
    String nameZh,

    @NotBlank
    @Size(max = 80)
    String nameEn,

    @NotBlank
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$")
    String color,

    @Min(1)
    Integer wipLimit,

    Boolean isDone
) {
}
