package com.sdkanban.board.dto;

import java.util.List;

public record BoardColumnTasks(
    Long id,
    String name,
    String color,
    Integer sortOrder,
    boolean isDone,
    List<TaskCardResponse> tasks
) {
}
