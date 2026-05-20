package com.sdkanban.board.dto;

import java.util.List;

public record MyTaskBoardResponse(
    String groupBy,
    List<MyTaskBoardGroup> groups
) {
}
