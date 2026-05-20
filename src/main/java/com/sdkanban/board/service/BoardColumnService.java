package com.sdkanban.board.service;

import com.sdkanban.board.dto.BoardColumnResponse;
import com.sdkanban.board.dto.CreateBoardColumnRequest;
import com.sdkanban.board.dto.ReorderBoardColumnsRequest;
import com.sdkanban.board.dto.UpdateBoardColumnRequest;

import java.util.List;

public interface BoardColumnService {
    List<BoardColumnResponse> list(Long projectId, Long currentUserId);

    BoardColumnResponse create(Long projectId, CreateBoardColumnRequest request, Long currentUserId);

    BoardColumnResponse update(Long projectId, Long columnId, UpdateBoardColumnRequest request, Long currentUserId);

    List<BoardColumnResponse> reorder(Long projectId, ReorderBoardColumnsRequest request, Long currentUserId);

    void delete(Long projectId, Long columnId, Long currentUserId);
}
