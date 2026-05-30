package com.sdkanban.settings.service;

import com.sdkanban.board.entity.BoardColumn;
import com.sdkanban.settings.dto.BoardColumnTemplateResponse;
import com.sdkanban.settings.dto.CreateBoardColumnTemplateRequest;
import com.sdkanban.settings.dto.ReorderBoardColumnTemplatesRequest;
import com.sdkanban.settings.dto.UpdateBoardColumnTemplateRequest;

import java.util.List;

public interface BoardTemplateService {
    List<BoardColumnTemplateResponse> list(Long currentUserId);

    BoardColumnTemplateResponse create(CreateBoardColumnTemplateRequest request, Long currentUserId);

    BoardColumnTemplateResponse update(String templateKey, UpdateBoardColumnTemplateRequest request, Long currentUserId);

    List<BoardColumnTemplateResponse> reorder(ReorderBoardColumnTemplatesRequest request, Long currentUserId);

    void delete(String templateKey, Long currentUserId);

    List<BoardColumn> createProjectColumns(Long projectId);
}
