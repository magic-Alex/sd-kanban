package com.sdkanban.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBoardColumnTemplateRequest(
    @NotBlank
    @Size(max = 80)
    String nameZh,

    @NotBlank
    @Size(max = 80)
    String nameEn,

    @NotBlank
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$")
    String color,

    Integer wipLimit,

    Boolean isDone
) {
}
