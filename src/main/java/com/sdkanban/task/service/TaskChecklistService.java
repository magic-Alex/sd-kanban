package com.sdkanban.task.service;

import com.sdkanban.task.dto.CreateTaskChecklistItemRequest;
import com.sdkanban.task.dto.ReorderTaskChecklistItemsRequest;
import com.sdkanban.task.dto.TaskChecklistItemResponse;
import com.sdkanban.task.dto.UpdateTaskChecklistItemRequest;

import java.util.List;

public interface TaskChecklistService {
    List<TaskChecklistItemResponse> list(Long taskId, Long currentUserId);

    TaskChecklistItemResponse create(Long taskId, CreateTaskChecklistItemRequest request, Long currentUserId);

    TaskChecklistItemResponse update(Long taskId, Long itemId, UpdateTaskChecklistItemRequest request, Long currentUserId);

    TaskChecklistItemResponse toggle(Long taskId, Long itemId, Long currentUserId);

    List<TaskChecklistItemResponse> reorder(Long taskId, ReorderTaskChecklistItemsRequest request, Long currentUserId);

    void delete(Long taskId, Long itemId, Long currentUserId);
}
