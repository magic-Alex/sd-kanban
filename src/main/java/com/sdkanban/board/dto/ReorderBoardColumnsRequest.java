package com.sdkanban.board.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderBoardColumnsRequest(
    @NotEmpty
    List<Long> columnIds
) {
}
