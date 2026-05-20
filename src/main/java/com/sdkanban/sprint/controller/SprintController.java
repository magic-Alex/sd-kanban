package com.sdkanban.sprint.controller;

import com.sdkanban.common.ApiResponse;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.sprint.dto.CreateSprintRequest;
import com.sdkanban.sprint.dto.SprintResponse;
import com.sdkanban.sprint.dto.UpdateSprintRequest;
import com.sdkanban.sprint.service.SprintService;
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
@RequestMapping("/api/projects/{projectId}/sprints")
@Conditional(ProjectPersistenceAvailableCondition.class)
public class SprintController {
    private final SprintService sprintService;

    public SprintController(SprintService sprintService) {
        this.sprintService = sprintService;
    }

    @GetMapping
    ApiResponse<List<SprintResponse>> list(
        @PathVariable Long projectId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(sprintService.list(projectId, currentUserId(user)));
    }

    @PostMapping
    ApiResponse<SprintResponse> create(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateSprintRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(sprintService.create(projectId, request, currentUserId(user)));
    }

    @PatchMapping("/{sprintId}")
    ApiResponse<SprintResponse> update(
        @PathVariable Long projectId,
        @PathVariable Long sprintId,
        @Valid @RequestBody UpdateSprintRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(sprintService.update(projectId, sprintId, request, currentUserId(user)));
    }

    @DeleteMapping("/{sprintId}")
    ApiResponse<Void> delete(
        @PathVariable Long projectId,
        @PathVariable Long sprintId,
        @AuthenticationPrincipal User user
    ) {
        sprintService.delete(projectId, sprintId, currentUserId(user));
        return ApiResponse.ok(null);
    }

    private Long currentUserId(User user) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return user.getId();
    }
}
