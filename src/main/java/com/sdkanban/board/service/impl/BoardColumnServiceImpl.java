package com.sdkanban.board.service.impl;

import com.sdkanban.board.dto.BoardColumnResponse;
import com.sdkanban.board.dto.CreateBoardColumnRequest;
import com.sdkanban.board.dto.ReorderBoardColumnsRequest;
import com.sdkanban.board.dto.UpdateBoardColumnRequest;
import com.sdkanban.board.repository.BoardColumnRepository;
import com.sdkanban.board.service.BoardColumnService;
import com.sdkanban.common.BusinessException;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.project.service.ProjectService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Conditional(ProjectPersistenceAvailableCondition.class)
public class BoardColumnServiceImpl implements BoardColumnService {
    private static final String GLOBAL_TEMPLATE_REQUIRED_MESSAGE = "看板列由系统设置中的统一模板管理";

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
        return globalTemplateRequired();
    }

    @Override
    @Transactional
    public BoardColumnResponse update(Long projectId, Long columnId, UpdateBoardColumnRequest request, Long currentUserId) {
        projectService.requireOwner(projectId, currentUserId);
        return globalTemplateRequired();
    }

    @Override
    @Transactional
    public List<BoardColumnResponse> reorder(Long projectId, ReorderBoardColumnsRequest request, Long currentUserId) {
        projectService.requireOwner(projectId, currentUserId);
        return globalTemplateRequired();
    }

    @Override
    @Transactional
    public void delete(Long projectId, Long columnId, Long currentUserId) {
        projectService.requireOwner(projectId, currentUserId);
        globalTemplateRequired();
    }

    private List<BoardColumnResponse> listResponses(Long projectId) {
        return boardColumnRepository.findByProjectIdOrderBySortOrderAscIdAsc(projectId).stream()
            .map(BoardColumnResponse::from)
            .toList();
    }

    private <T> T globalTemplateRequired() {
        throw BusinessException.badRequest("GLOBAL_TEMPLATE_REQUIRED", GLOBAL_TEMPLATE_REQUIRED_MESSAGE);
    }
}
