package com.sdkanban.project.service.impl;

import com.sdkanban.board.entity.BoardColumn;
import com.sdkanban.board.repository.BoardColumnRepository;
import com.sdkanban.common.BusinessException;
import com.sdkanban.project.dto.AddProjectMemberRequest;
import com.sdkanban.project.dto.CreateProjectRequest;
import com.sdkanban.project.dto.ProjectMemberResponse;
import com.sdkanban.project.dto.ProjectResponse;
import com.sdkanban.project.dto.TransferProjectOwnerRequest;
import com.sdkanban.project.entity.Project;
import com.sdkanban.project.entity.ProjectMember;
import com.sdkanban.project.entity.ProjectMemberId;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.project.repository.ProjectMemberRepository;
import com.sdkanban.project.repository.ProjectRepository;
import com.sdkanban.project.service.ProjectService;
import com.sdkanban.user.dto.UserSummary;
import com.sdkanban.user.entity.User;
import com.sdkanban.user.repository.UserRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Conditional(ProjectPersistenceAvailableCondition.class)
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final BoardColumnRepository boardColumnRepository;

    public ProjectServiceImpl(
        ProjectRepository projectRepository,
        ProjectMemberRepository projectMemberRepository,
        UserRepository userRepository,
        BoardColumnRepository boardColumnRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.boardColumnRepository = boardColumnRepository;
    }

    @Override
    @Transactional
    public ProjectResponse create(CreateProjectRequest request, Long currentUserId) {
        User creator = requireUser(currentUserId);
        Project project = projectRepository.save(new Project(
            creator.getId(),
            creator.getId(),
            request.name().trim(),
            normalizeDescription(request.description())
        ));
        projectMemberRepository.save(new ProjectMember(project.getId(), creator.getId(), ProjectMember.ROLE_OWNER));
        initializeDefaultColumns(project.getId());

        return toProjectResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> listForUser(Long currentUserId) {
        requireUser(currentUserId);
        return projectRepository.findVisibleToUser(currentUserId).stream()
            .map(this::toProjectResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse detail(Long projectId, Long currentUserId) {
        requireMember(projectId, currentUserId);
        return toProjectResponse(requireProject(projectId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(Long projectId, Long currentUserId) {
        requireMember(projectId, currentUserId);
        return projectMemberRepository.findByIdProjectIdOrderByCreatedAtAsc(projectId).stream()
            .map(this::toMemberResponse)
            .toList();
    }

    @Override
    @Transactional
    public ProjectMemberResponse addMember(Long projectId, AddProjectMemberRequest request, Long currentUserId) {
        requireOwner(projectId, currentUserId);
        User user = requireUser(request.userId());
        ProjectMemberId memberId = new ProjectMemberId(projectId, user.getId());
        if (projectMemberRepository.existsById(memberId)) {
            throw BusinessException.conflict("PROJECT_MEMBER_EXISTS", "User is already a project member");
        }

        ProjectMember member;
        try {
            member = projectMemberRepository.saveAndFlush(
                new ProjectMember(projectId, user.getId(), ProjectMember.ROLE_MEMBER)
            );
        } catch (DataIntegrityViolationException exception) {
            throw BusinessException.conflict("PROJECT_MEMBER_EXISTS", "User is already a project member");
        }
        return toMemberResponse(member);
    }

    @Override
    @Transactional
    public void removeMember(Long projectId, Long userId, Long currentUserId) {
        Project project = requireProjectForUpdate(projectId);
        requireOwner(project, currentUserId);
        if (project.getOwnerId().equals(userId)) {
            throw BusinessException.badRequest("CANNOT_REMOVE_PROJECT_OWNER", "Project owner cannot be removed");
        }

        ProjectMember member = projectMemberRepository.findById(new ProjectMemberId(projectId, userId))
            .orElseThrow(() -> BusinessException.forbidden(
                "PROJECT_MEMBER_REQUIRED",
                "Project membership is required"
            ));
        projectMemberRepository.delete(member);
    }

    @Override
    @Transactional
    public ProjectResponse transferOwner(Long projectId, TransferProjectOwnerRequest request, Long currentUserId) {
        Project project = requireProjectForUpdate(projectId);
        ProjectMember currentOwnerMember = projectMemberRepository.findById(new ProjectMemberId(projectId, currentUserId))
            .orElseThrow(() -> BusinessException.forbidden(
                "PROJECT_MEMBER_REQUIRED",
                "Project membership is required"
            ));
        if (!project.getOwnerId().equals(currentUserId)
            || !ProjectMember.ROLE_OWNER.equals(currentOwnerMember.getRole())) {
            throw BusinessException.forbidden("PROJECT_OWNER_REQUIRED", "Project owner permission is required");
        }
        ProjectMember newOwnerMember = projectMemberRepository.findById(new ProjectMemberId(projectId, request.userId()))
            .orElseThrow(() -> BusinessException.forbidden(
                "PROJECT_MEMBER_REQUIRED",
                "Project membership is required"
            ));

        projectMemberRepository.findByIdProjectIdAndRole(projectId, ProjectMember.ROLE_OWNER)
            .forEach(ownerMember -> ownerMember.changeRole(ProjectMember.ROLE_MEMBER));
        newOwnerMember.changeRole(ProjectMember.ROLE_OWNER);
        project.transferOwner(request.userId());

        return toProjectResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectMember requireMember(Long projectId, Long userId) {
        requireProject(projectId);
        return projectMemberRepository.findById(new ProjectMemberId(projectId, userId))
            .orElseThrow(() -> BusinessException.forbidden(
                "PROJECT_MEMBER_REQUIRED",
                "Project membership is required"
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectMember requireOwner(Long projectId, Long userId) {
        Project project = requireProject(projectId);
        return requireOwner(project, userId);
    }

    private ProjectMember requireOwner(Project project, Long userId) {
        ProjectMember member = projectMemberRepository.findById(new ProjectMemberId(project.getId(), userId))
            .orElseThrow(() -> BusinessException.forbidden(
                "PROJECT_MEMBER_REQUIRED",
                "Project membership is required"
            ));
        if (!project.getOwnerId().equals(userId) || !ProjectMember.ROLE_OWNER.equals(member.getRole())) {
            throw BusinessException.forbidden("PROJECT_OWNER_REQUIRED", "Project owner permission is required");
        }
        return member;
    }

    private Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
            .orElseThrow(() -> BusinessException.notFound("PROJECT_NOT_FOUND", "Project not found"));
    }

    private Project requireProjectForUpdate(Long projectId) {
        return projectRepository.findByIdForUpdate(projectId)
            .orElseThrow(() -> BusinessException.notFound("PROJECT_NOT_FOUND", "Project not found"));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "User not found"));
    }

    private void initializeDefaultColumns(Long projectId) {
        boardColumnRepository.saveAll(List.of(
            new BoardColumn(projectId, "Backlog", "#64748b", 0, null, false),
            new BoardColumn(projectId, "Ready", "#0ea5e9", 1, null, false),
            new BoardColumn(projectId, "In Progress", "#f59e0b", 2, null, false),
            new BoardColumn(projectId, "Testing", "#8b5cf6", 3, null, false),
            new BoardColumn(projectId, "Done", "#22c55e", 4, null, true)
        ));
    }

    private ProjectResponse toProjectResponse(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getName(),
            project.getDescription(),
            UserSummary.from(requireUser(project.getOwnerId())),
            UserSummary.from(requireUser(project.getCreatorId())),
            project.getStatus(),
            projectMemberRepository.countByIdProjectId(project.getId()),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember member) {
        return new ProjectMemberResponse(
            UserSummary.from(requireUser(member.getUserId())),
            member.getRole(),
            member.getCreatedAt()
        );
    }

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.trim();
    }
}
