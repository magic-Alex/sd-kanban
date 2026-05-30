package com.sdkanban.settings.service.impl;

import com.sdkanban.board.entity.BoardColumn;
import com.sdkanban.board.repository.BoardColumnRepository;
import com.sdkanban.common.BusinessException;
import com.sdkanban.project.repository.ProjectRepository;
import com.sdkanban.settings.dto.BoardColumnTemplateResponse;
import com.sdkanban.settings.dto.CreateBoardColumnTemplateRequest;
import com.sdkanban.settings.dto.ReorderBoardColumnTemplatesRequest;
import com.sdkanban.settings.dto.UpdateBoardColumnTemplateRequest;
import com.sdkanban.settings.entity.BoardColumnTemplate;
import com.sdkanban.settings.repository.BoardColumnTemplateRepository;
import com.sdkanban.settings.service.BoardTemplateService;
import com.sdkanban.user.entity.User;
import com.sdkanban.user.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BoardTemplateServiceImpl implements BoardTemplateService {
    private final BoardColumnTemplateRepository boardColumnTemplateRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final Validator validator;

    public BoardTemplateServiceImpl(
        BoardColumnTemplateRepository boardColumnTemplateRepository,
        BoardColumnRepository boardColumnRepository,
        ProjectRepository projectRepository,
        UserRepository userRepository,
        Validator validator
    ) {
        this.boardColumnTemplateRepository = boardColumnTemplateRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.validator = validator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardColumnTemplateResponse> list(Long currentUserId) {
        requireAdmin(currentUserId);
        return listResponses();
    }

    @Override
    @Transactional
    public BoardColumnTemplateResponse create(CreateBoardColumnTemplateRequest request, Long currentUserId) {
        requireAdmin(currentUserId);
        validate(request);
        String templateKey = request.templateKey().trim();
        if (boardColumnTemplateRepository.existsByTemplateKey(templateKey)) {
            throw templateKeyExists();
        }

        try {
            BoardColumnTemplate template = boardColumnTemplateRepository.saveAndFlush(new BoardColumnTemplate(
                templateKey,
                request.nameZh().trim(),
                request.nameEn().trim(),
                request.color().trim(),
                nextSortOrder(),
                request.wipLimit(),
                Boolean.TRUE.equals(request.isDone())
            ));
            backfillProjectColumns(template);
            return BoardColumnTemplateResponse.from(template);
        } catch (DataIntegrityViolationException exception) {
            throw templateKeyExists();
        }
    }

    @Override
    @Transactional
    public BoardColumnTemplateResponse update(String templateKey, UpdateBoardColumnTemplateRequest request, Long currentUserId) {
        requireAdmin(currentUserId);
        validate(request);
        BoardColumnTemplate template = requireTemplate(templateKey);
        template.update(
            request.nameZh().trim(),
            request.nameEn().trim(),
            request.color().trim(),
            request.wipLimit(),
            Boolean.TRUE.equals(request.isDone())
        );
        syncProjectColumns(template);
        return BoardColumnTemplateResponse.from(template);
    }

    @Override
    @Transactional
    public List<BoardColumnTemplateResponse> reorder(ReorderBoardColumnTemplatesRequest request, Long currentUserId) {
        requireAdmin(currentUserId);
        validate(request);
        List<BoardColumnTemplate> templates = boardColumnTemplateRepository.findByOrderBySortOrderAscIdAsc();
        List<String> templateKeys = request.templateKeys();
        if (templates.size() != templateKeys.size() || new HashSet<>(templateKeys).size() != templateKeys.size()) {
            throw invalidReorder();
        }

        Map<String, BoardColumnTemplate> templatesByKey = templates.stream()
            .collect(Collectors.toMap(BoardColumnTemplate::getTemplateKey, Function.identity()));
        for (String templateKey : templateKeys) {
            if (!templatesByKey.containsKey(templateKey)) {
                throw invalidReorder();
            }
        }

        for (int index = 0; index < templateKeys.size(); index++) {
            templatesByKey.get(templateKeys.get(index)).changeSortOrder(-(index + 1));
        }
        boardColumnTemplateRepository.flush();
        stageProjectColumns(templateKeys);

        for (int index = 0; index < templateKeys.size(); index++) {
            BoardColumnTemplate template = templatesByKey.get(templateKeys.get(index));
            template.changeSortOrder(index);
            syncProjectColumns(template);
        }

        return listResponses();
    }

    @Override
    @Transactional
    public void delete(String templateKey, Long currentUserId) {
        requireAdmin(currentUserId);
        BoardColumnTemplate template = requireTemplate(templateKey);
        if (boardColumnRepository.countTasksByTemplateKey(template.getTemplateKey()) > 0) {
            throw BusinessException.conflict("TEMPLATE_COLUMN_NOT_EMPTY", "Template column still contains tasks");
        }

        boardColumnRepository.deleteByTemplateKey(template.getTemplateKey());
        boardColumnTemplateRepository.delete(template);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardColumn> createProjectColumns(Long projectId) {
        return boardColumnTemplateRepository.findByOrderBySortOrderAscIdAsc().stream()
            .map(template -> new BoardColumn(
                projectId,
                template.getTemplateKey(),
                template.getDisplayName(),
                template.getColor(),
                template.getSortOrder(),
                template.getWipLimit(),
                template.isDone()
            ))
            .toList();
    }

    private void requireAdmin(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> BusinessException.forbidden("FORBIDDEN", "Access denied"));
        if (!user.isAdmin()) {
            throw BusinessException.forbidden("FORBIDDEN", "Access denied");
        }
    }

    private <T> void validate(T request) {
        if (request == null) {
            throw BusinessException.badRequest("VALIDATION_FAILED", "Validation failed");
        }
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw BusinessException.badRequest("VALIDATION_FAILED", "Validation failed");
        }
    }

    private BoardColumnTemplate requireTemplate(String templateKey) {
        return boardColumnTemplateRepository.findByTemplateKey(templateKey)
            .orElseThrow(() -> BusinessException.notFound("BOARD_TEMPLATE_NOT_FOUND", "Board column template not found"));
    }

    private List<BoardColumnTemplateResponse> listResponses() {
        return boardColumnTemplateRepository.findByOrderBySortOrderAscIdAsc().stream()
            .map(BoardColumnTemplateResponse::from)
            .toList();
    }

    private int nextSortOrder() {
        return boardColumnTemplateRepository.findByOrderBySortOrderAscIdAsc().stream()
            .map(BoardColumnTemplate::getSortOrder)
            .max(Integer::compareTo)
            .orElse(-1) + 1;
    }

    private void syncProjectColumns(BoardColumnTemplate template) {
        boardColumnRepository.findByTemplateKey(template.getTemplateKey())
            .forEach(column -> column.syncFromTemplate(
                template.getDisplayName(),
                template.getColor(),
                template.getSortOrder(),
                template.getWipLimit(),
                template.isDone()
            ));
    }

    private void stageProjectColumns(List<String> templateKeys) {
        for (int index = 0; index < templateKeys.size(); index++) {
            int stagedSortOrder = -(index + 100_000);
            boardColumnRepository.findByTemplateKey(templateKeys.get(index))
                .forEach(column -> column.changeSortOrder(stagedSortOrder));
        }
        boardColumnRepository.flush();
    }

    private void backfillProjectColumns(BoardColumnTemplate template) {
        List<BoardColumn> columns = projectRepository.findAll().stream()
            .filter(project -> boardColumnRepository.findByProjectIdAndTemplateKey(
                project.getId(),
                template.getTemplateKey()
            ).isEmpty())
            .map(project -> new BoardColumn(
                project.getId(),
                template.getTemplateKey(),
                template.getDisplayName(),
                template.getColor(),
                template.getSortOrder(),
                template.getWipLimit(),
                template.isDone()
            ))
            .toList();
        boardColumnRepository.saveAll(columns);
    }

    private BusinessException templateKeyExists() {
        return BusinessException.conflict("TEMPLATE_KEY_EXISTS", "Board column template key already exists");
    }

    private BusinessException invalidReorder() {
        return BusinessException.badRequest(
            "TEMPLATE_REORDER_INVALID",
            "Template reorder payload must contain each board column template once"
        );
    }
}
