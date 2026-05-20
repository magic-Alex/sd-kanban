package com.sdkanban.board.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateBoardColumnRequest(
    @Size(max = 80)
    String name,

    @Size(max = 20)
    String color,

    @Min(1)
    Integer wipLimit,

    Boolean isDone
) {
}
