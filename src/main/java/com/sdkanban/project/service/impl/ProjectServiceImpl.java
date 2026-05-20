package com.sdkanban.project.service.impl;

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

    public ProjectServiceImpl(
        ProjectRepository projectRepository,
        ProjectMemberRepository projectMemberRepository,
        UserRepository userRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
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

        ProjectMember member = projectMemberRepository.save(
            new ProjectMember(projectId, user.getId(), ProjectMember.ROLE_MEMBER)
        );
        return toMemberResponse(member);
    }

    @Override
    @Transactional
    public void removeMember(Long projectId, Long userId, Long currentUserId) {
        requireOwner(projectId, currentUserId);
        Project project = requireProject(projectId);
        if (project.getOwnerId().equals(userId)) {
            throw BusinessException.badRequest("CANNOT_REMOVE_PROJECT_OWNER", "Project owner cannot be removed");
        }

        ProjectMember member = requireMember(projectId, userId);
        projectMemberRepository.delete(member);
    }

    @Override
    @Transactional
    public ProjectResponse transferOwner(Long projectId, TransferProjectOwnerRequest request, Long currentUserId) {
        ProjectMember oldOwnerMember = requireOwner(projectId, currentUserId);
        ProjectMember newOwnerMember = requireMember(projectId, request.userId());
        Project project = requireProject(projectId);

        oldOwnerMember.changeRole(ProjectMember.ROLE_MEMBER);
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
        ProjectMember member = requireMember(projectId, userId);
        if (!project.getOwnerId().equals(userId) || !ProjectMember.ROLE_OWNER.equals(member.getRole())) {
            throw BusinessException.forbidden("PROJECT_OWNER_REQUIRED", "Project owner permission is required");
        }
        return member;
    }

    private Project requireProject(Long projectId) {
        return projectRepository.findById(projectId)
            .orElseThrow(() -> BusinessException.notFound("PROJECT_NOT_FOUND", "Project not found"));
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "User not found"));
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
