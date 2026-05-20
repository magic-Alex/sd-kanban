package com.sdkanban.board.service;

import com.sdkanban.board.dto.BoardResponse;
import com.sdkanban.board.dto.MyTaskBoardResponse;

public interface BoardService {
    BoardResponse projectBoard(
        Long projectId,
        Long sprintId,
        Long assigneeId,
        String type,
        String priority,
        String keyword,
        Long currentUserId
    );

    MyTaskBoardResponse myTaskBoard(String groupBy, Long currentUserId);
}
