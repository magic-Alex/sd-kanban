package com.sdkanban.sprint.service.impl;

import com.sdkanban.common.BusinessException;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.project.service.ProjectService;
import com.sdkanban.sprint.dto.CreateSprintRequest;
import com.sdkanban.sprint.dto.SprintResponse;
import com.sdkanban.sprint.dto.UpdateSprintRequest;
import com.sdkanban.sprint.entity.Sprint;
import com.sdkanban.sprint.repository.SprintRepository;
import com.sdkanban.sprint.service.SprintService;
import org.springframework.context.annotation.Conditional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@Conditional(ProjectPersistenceAvailableCondition.class)
public class SprintServiceImpl implements SprintService {
    private static final String DEFAULT_STATUS = "PLANNED";

    private final SprintRepository sprintRepository;
    private final ProjectService projectService;

    public SprintServiceImpl(SprintRepository sprintRepository, ProjectService projectService) {
        this.sprintRepository = sprintRepository;
        this.projectService = projectService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SprintResponse> list(Long projectId, Long currentUserId) {
        projectService.requireMember(projectId, currentUserId);
        return sprintRepository.findByProjectIdOrderByCreatedAtDescIdDesc(projectId).stream()
            .map(SprintResponse::from)
            .toList();
    }

    @Override
    @Transactional
    public SprintResponse create(Long projectId, CreateSprintRequest request, Long currentUserId) {
        projectService.requireMember(projectId, currentUserId);
        validateDateRange(request.startDate(), request.endDate());
        Sprint sprint = sprintRepository.save(new Sprint(
            projectId,
            requiredName(request.name()),
            normalizeGoal(request.goal()),
            request.startDate(),
            request.endDate(),
            normalizeStatus(request.status())
        ));
        return SprintResponse.from(sprint);
    }

    @Override
    @Transactional
    public SprintResponse update(Long projectId, Long sprintId, UpdateSprintRequest request, Long currentUserId) {
        projectService.requireMember(projectId, currentUserId);
        Sprint sprint = sprintRepository.findByIdAndProjectId(sprintId, projectId)
            .orElseThrow(() -> BusinessException.notFound("SPRINT_NOT_FOUND", "Sprint not found"));

        LocalDate startDate = request.startDate() == null ? sprint.getStartDate() : request.startDate();
        LocalDate endDate = request.endDate() == null ? sprint.getEndDate() : request.endDate();
        validateDateRange(startDate, endDate);
        sprint.update(
            optionalName(request.name(), sprint.getName()),
            request.goal() == null ? sprint.getGoal() : normalizeGoal(request.goal()),
            startDate,
            endDate,
            request.status() == null ? sprint.getStatus() : normalizeStatus(request.status())
        );
        return SprintResponse.from(sprint);
    }

    @Override
    @Transactional
    public void delete(Long projectId, Long sprintId, Long currentUserId) {
        projectService.requireMember(projectId, currentUserId);
        Sprint sprint = sprintRepository.findByIdAndProjectId(sprintId, projectId)
            .orElseThrow(() -> BusinessException.notFound("SPRINT_NOT_FOUND", "Sprint not found"));
        try {
            sprintRepository.delete(sprint);
            sprintRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("SPRINT_IN_USE", "Sprint is still referenced by tasks");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw BusinessException.badRequest("SPRINT_DATE_RANGE_INVALID", "Sprint end date cannot be before start date");
        }
    }

    private String requiredName(String name) {
        if (!StringUtils.hasText(name)) {
            throw BusinessException.badRequest("SPRINT_NAME_REQUIRED", "Sprint name is required");
        }
        return name.trim();
    }

    private String optionalName(String name, String fallback) {
        if (name == null) {
            return fallback;
        }
        return requiredName(name);
    }

    private String normalizeGoal(String goal) {
        if (!StringUtils.hasText(goal)) {
            return null;
        }
        return goal.trim();
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return DEFAULT_STATUS;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }
}
