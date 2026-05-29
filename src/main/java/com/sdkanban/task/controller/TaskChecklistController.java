package com.sdkanban.task.controller;

import com.sdkanban.common.ApiResponse;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.task.dto.CreateTaskChecklistItemRequest;
import com.sdkanban.task.dto.ReorderTaskChecklistItemsRequest;
import com.sdkanban.task.dto.TaskChecklistItemResponse;
import com.sdkanban.task.dto.UpdateTaskChecklistItemRequest;
import com.sdkanban.task.service.TaskChecklistService;
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
@RequestMapping("/api/tasks/{taskId}/checklist")
@Conditional(ProjectPersistenceAvailableCondition.class)
public class TaskChecklistController {
    private final TaskChecklistService taskChecklistService;

    public TaskChecklistController(TaskChecklistService taskChecklistService) {
        this.taskChecklistService = taskChecklistService;
    }

    @GetMapping
    ApiResponse<List<TaskChecklistItemResponse>> list(
        @PathVariable Long taskId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskChecklistService.list(taskId, currentUserId(user)));
    }

    @PostMapping
    ApiResponse<TaskChecklistItemResponse> create(
        @PathVariable Long taskId,
        @Valid @RequestBody CreateTaskChecklistItemRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskChecklistService.create(taskId, request, currentUserId(user)));
    }

    @PatchMapping("/{itemId}")
    ApiResponse<TaskChecklistItemResponse> update(
        @PathVariable Long taskId,
        @PathVariable Long itemId,
        @Valid @RequestBody UpdateTaskChecklistItemRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskChecklistService.update(taskId, itemId, request, currentUserId(user)));
    }

    @PatchMapping("/{itemId}/toggle")
    ApiResponse<TaskChecklistItemResponse> toggle(
        @PathVariable Long taskId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskChecklistService.toggle(taskId, itemId, currentUserId(user)));
    }

    @PatchMapping("/reorder")
    ApiResponse<List<TaskChecklistItemResponse>> reorder(
        @PathVariable Long taskId,
        @Valid @RequestBody ReorderTaskChecklistItemsRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskChecklistService.reorder(taskId, request, currentUserId(user)));
    }

    @DeleteMapping("/{itemId}")
    ApiResponse<Void> delete(
        @PathVariable Long taskId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal User user
    ) {
        taskChecklistService.delete(taskId, itemId, currentUserId(user));
        return ApiResponse.ok(null);
    }

    private Long currentUserId(User user) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return user.getId();
    }
}
