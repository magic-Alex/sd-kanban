package com.sdkanban.board.dto;

import java.util.List;

public record BoardResponse(
    Long projectId,
    List<BoardColumnTasks> columns
) {
}
