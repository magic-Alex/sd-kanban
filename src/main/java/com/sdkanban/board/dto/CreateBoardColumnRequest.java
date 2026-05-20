package com.sdkanban.board.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardColumnRequest(
    @NotBlank
    @Size(max = 80)
    String name,

    @Size(max = 20)
    String color,

    @Min(1)
    Integer wipLimit,

    Boolean isDone
) {
}
