package com.sdkanban.board.service.impl;

import com.sdkanban.board.dto.BoardColumnResponse;
import com.sdkanban.board.dto.CreateBoardColumnRequest;
import com.sdkanban.board.dto.ReorderBoardColumnsRequest;
import com.sdkanban.board.dto.UpdateBoardColumnRequest;
import com.sdkanban.board.entity.BoardColumn;
import com.sdkanban.board.repository.BoardColumnRepository;
import com.sdkanban.board.service.BoardColumnService;
import com.sdkanban.common.BusinessException;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.project.service.ProjectService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Conditional(ProjectPersistenceAvailableCondition.class)
public class BoardColumnServiceImpl implements BoardColumnService {
    private static final String DEFAULT_COLOR = "#64748b";

    private final BoardColumnRepository boardColumnRepository;
    private final ProjectService projectService;

    public BoardColumnServiceImpl(BoardColumnRepository boardColumnRepository, ProjectService projectService) {
        this.boardColumnRepository = boardColumnRepository;
        this.projectService = projectService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardColumnResponse> list(Long projectId, Long currentUserId) {
        projectService.requireMember(projectId, currentUserId);
        return listResponses(projectId);
    }

    @Override
    @Transactional
    public BoardColumnResponse create(Long projectId, CreateBoardColumnRequest request, Long currentUserId) {
        projectService.requireOwner(projectId, currentUserId);
        BoardColumn column = boardColumnRepository.save(new BoardColumn(
            projectId,
            requiredName(request.name()),
            normalizeColor(request.color()),
            boardColumnRepository.maxSortOrderByProjectId(projectId) + 1,
            request.wipLimit(),
            Boolean.TRUE.equals(request.isDone())
        ));
        return BoardColumnResponse.from(column);
    }

    @Override
    @Transactional
    public BoardColumnResponse update(Long projectId, Long columnId, UpdateBoardColumnRequest request, Long currentUserId) {
        projectService.requireOwner(projectId, currentUserId);
        BoardColumn column = requireColumn(projectId, columnId);
        column.update(
            optionalName(request.name(), column.getName()),
            normalizeColorOrDefault(request.color(), column.getColor()),
            request.wipLimit() == null ? column.getWipLimit() : request.wipLimit(),
            request.isDone() == null ? column.isDone() : request.isDone()
        );
        return BoardColumnResponse.from(column);
    }

    @Override
    @Transactional
    public List<BoardColumnResponse> reorder(Long projectId, ReorderBoardColumnsRequest request, Long currentUserId) {
        projectService.requireOwner(projectId, currentUserId);
        List<BoardColumn> columns = boardColumnRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId);
        if (columns.size() != request.columnIds().size()
            || new HashSet<>(request.columnIds()).size() != request.columnIds().size()) {
            throw BusinessException.badRequest("COLUMN_REORDER_INVALID", "Column reorder payload must contain each project column once");
        }

        Map<Long, BoardColumn> columnsById = columns.stream()
            .collect(Collectors.toMap(BoardColumn::getId, Function.identity()));
        for (Long columnId : request.columnIds()) {
            if (!columnsById.containsKey(columnId)) {
                throw BusinessException.badRequest("COLUMN_REORDER_INVALID", "Column reorder payload must contain each project column once");
            }
        }

        for (int index = 0; index < request.columnIds().size(); index++) {
            columnsById.get(request.columnIds().get(index)).changeSortOrder(-(index + 1));
        }
        boardColumnRepository.flush();

        for (int index = 0; index < request.columnIds().size(); index++) {
            columnsById.get(request.columnIds().get(index)).changeSortOrder(index);
        }

        return listResponses(projectId);
    }

    @Override
    @Transactional
    public void delete(Long projectId, Long columnId, Long currentUserId) {
        projectService.requireOwner(projectId, currentUserId);
        BoardColumn column = requireColumn(projectId, columnId);
        if (boardColumnRepository.countTasksInColumn(projectId, columnId) > 0) {
            throw BusinessException.conflict("COLUMN_NOT_EMPTY", "Board column still contains tasks");
        }
        boardColumnRepository.delete(column);
    }

    private List<BoardColumnResponse> listResponses(Long projectId) {
        return boardColumnRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId).stream()
            .map(BoardColumnResponse::from)
            .toList();
    }

    private BoardColumn requireColumn(Long projectId, Long columnId) {
        return boardColumnRepository.findByIdAndProjectId(columnId, projectId)
            .orElseThrow(() -> BusinessException.notFound("BOARD_COLUMN_NOT_FOUND", "Board column not found"));
    }

    private String requiredName(String name) {
        if (!StringUtils.hasText(name)) {
            throw BusinessException.badRequest("COLUMN_NAME_REQUIRED", "Column name is required");
        }
        return name.trim();
    }

    private String optionalName(String name, String fallback) {
        if (name == null) {
            return fallback;
        }
        return requiredName(name);
    }

    private String normalizeColor(String color) {
        if (!StringUtils.hasText(color)) {
            return DEFAULT_COLOR;
        }
        return color.trim();
    }

    private String normalizeColorOrDefault(String color, String fallback) {
        if (color == null) {
            return fallback;
        }
        return normalizeColor(color);
    }
}
