package com.sdkanban.task.controller;

import com.sdkanban.common.ApiResponse;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.task.dto.AddTaskCommentRequest;
import com.sdkanban.task.dto.CreateTaskRequest;
import com.sdkanban.task.dto.CreateTaskTagRequest;
import com.sdkanban.task.dto.TaskActivityResponse;
import com.sdkanban.task.dto.TaskCommentResponse;
import com.sdkanban.task.dto.TaskResponse;
import com.sdkanban.task.dto.TaskTagResponse;
import com.sdkanban.task.dto.UpdateTaskPositionRequest;
import com.sdkanban.task.dto.UpdateTaskRequest;
import com.sdkanban.task.dto.UpdateTaskTagsRequest;
import com.sdkanban.task.service.TaskService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @PatchMapping("/tasks/{taskId}/position")
    ApiResponse<TaskResponse> updatePosition(
        @PathVariable Long taskId,
        @Valid @RequestBody UpdateTaskPositionRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskService.updatePosition(taskId, request, currentUserId(user)));
    }

    @PatchMapping("/tasks/{taskId}/archive")
    ApiResponse<TaskResponse> archive(
        @PathVariable Long taskId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskService.archive(taskId, currentUserId(user)));
    }

    @GetMapping("/projects/{projectId}/tasks/archived")
    ApiResponse<List<TaskResponse>> archivedTasks(
        @PathVariable Long projectId,
        @RequestParam(required = false) Long assigneeId,
        @RequestParam(required = false, name = "type") String type,
        @RequestParam(required = false) String priority,
        @RequestParam(required = false) String keyword,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskService.archivedTasks(projectId, assigneeId, type, priority, keyword, currentUserId(user)));
    }

    @PatchMapping("/tasks/{taskId}/restore")
    ApiResponse<TaskResponse> restore(
        @PathVariable Long taskId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskService.restore(taskId, currentUserId(user)));
    }

    @DeleteMapping("/tasks/{taskId}")
    ApiResponse<Void> delete(
        @PathVariable Long taskId,
        @AuthenticationPrincipal User user
    ) {
        taskService.delete(taskId, currentUserId(user));
        return ApiResponse.ok(null);
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

    @GetMapping("/tasks/{taskId}/comments")
    ApiResponse<List<TaskCommentResponse>> comments(
        @PathVariable Long taskId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskService.comments(taskId, currentUserId(user)));
    }

    @GetMapping("/tasks/{taskId}/activities")
    ApiResponse<List<TaskActivityResponse>> activities(
        @PathVariable Long taskId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskService.activities(taskId, currentUserId(user)));
    }

    private Long currentUserId(User user) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return user.getId();
    }
}
