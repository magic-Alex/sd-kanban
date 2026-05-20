package com.sdkanban.project.controller;

import com.sdkanban.common.ApiResponse;
import com.sdkanban.project.dto.AddProjectMemberRequest;
import com.sdkanban.project.dto.CreateProjectRequest;
import com.sdkanban.project.dto.ProjectMemberResponse;
import com.sdkanban.project.dto.ProjectResponse;
import com.sdkanban.project.dto.TransferProjectOwnerRequest;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.project.service.ProjectService;
import com.sdkanban.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@Conditional(ProjectPersistenceAvailableCondition.class)
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    ApiResponse<ProjectResponse> create(
        @Valid @RequestBody CreateProjectRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(projectService.create(request, currentUserId(user)));
    }

    @GetMapping
    ApiResponse<List<ProjectResponse>> list(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(projectService.listForUser(currentUserId(user)));
    }

    @GetMapping("/{projectId}")
    ApiResponse<ProjectResponse> detail(
        @PathVariable Long projectId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(projectService.detail(projectId, currentUserId(user)));
    }

    @GetMapping("/{projectId}/members")
    ApiResponse<List<ProjectMemberResponse>> listMembers(
        @PathVariable Long projectId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(projectService.listMembers(projectId, currentUserId(user)));
    }

    @PostMapping("/{projectId}/members")
    ApiResponse<ProjectMemberResponse> addMember(
        @PathVariable Long projectId,
        @Valid @RequestBody AddProjectMemberRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(projectService.addMember(projectId, request, currentUserId(user)));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    ApiResponse<Void> removeMember(
        @PathVariable Long projectId,
        @PathVariable Long userId,
        @AuthenticationPrincipal User user
    ) {
        projectService.removeMember(projectId, userId, currentUserId(user));
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{projectId}/owner")
    ApiResponse<ProjectResponse> transferOwner(
        @PathVariable Long projectId,
        @Valid @RequestBody TransferProjectOwnerRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(projectService.transferOwner(projectId, request, currentUserId(user)));
    }

    private Long currentUserId(User user) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return user.getId();
    }
}
