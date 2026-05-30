package com.sdkanban.board.dto;

import java.util.List;

public record MyTaskBoardGroup(
    String templateKey,
    String name,
    String color,
    Integer sortOrder,
    boolean isDone,
    List<TaskCardResponse> tasks
) {
}
