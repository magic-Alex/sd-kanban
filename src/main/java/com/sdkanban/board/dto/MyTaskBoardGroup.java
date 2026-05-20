package com.sdkanban.board.dto;

import java.util.List;

public record MyTaskBoardGroup(
    Long id,
    String name,
    List<TaskCardResponse> tasks
) {
}
