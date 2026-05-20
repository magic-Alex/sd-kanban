package com.sdkanban.task.controller;

import com.sdkanban.common.ApiResponse;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.task.dto.AddTaskCommentRequest;
import com.sdkanban.task.dto.CreateTaskRequest;
import com.sdkanban.task.dto.CreateTaskTagRequest;
import com.sdkanban.task.dto.TaskCommentResponse;
import com.sdkanban.task.dto.TaskResponse;
import com.sdkanban.task.dto.TaskTagResponse;
import com.sdkanban.task.dto.UpdateTaskRequest;
import com.sdkanban.task.dto.UpdateTaskTagsRequest;
import com.sdkanban.task.service.TaskService;
import com.sdkanban.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Conditional(ProjectPersistenceAvailableCondition.class)
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    ApiResponse<TaskResponse> create(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateTaskRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskService.create(projectId, request, currentUserId(user)));
    }

    @GetMapping("/tasks/{taskId}")
    ApiResponse<TaskResponse> detail(
        @PathVariable Long taskId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskService.detail(taskId, currentUserId(user)));
    }

    @PatchMapping("/tasks/{taskId}")
    ApiResponse<TaskResponse> update(
        @PathVariable Long taskId,
        @Valid @RequestBody UpdateTaskRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskService.update(taskId, request, currentUserId(user)));
    }

    @PostMapping("/projects/{projectId}/tags")
    ApiResponse<TaskTagResponse> createTag(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateTaskTagRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskService.createTag(projectId, request, currentUserId(user)));
    }

    @PatchMapping("/tasks/{taskId}/tags")
    ApiResponse<TaskResponse> updateTags(
        @PathVariable Long taskId,
        @Valid @RequestBody UpdateTaskTagsRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskService.updateTags(taskId, request, currentUserId(user)));
    }

    @PostMapping("/tasks/{taskId}/comments")
    ApiResponse<TaskCommentResponse> addComment(
        @PathVariable Long taskId,
        @Valid @RequestBody AddTaskCommentRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskService.addComment(taskId, request, currentUserId(user)));
    }

    private Long currentUserId(User user) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return user.getId();
    }
}
