package com.sdkanban.task.service;

import com.sdkanban.task.dto.AddTaskCommentRequest;
import com.sdkanban.task.dto.CreateTaskRequest;
import com.sdkanban.task.dto.CreateTaskTagRequest;
import com.sdkanban.task.dto.TaskActivityResponse;
import com.sdkanban.task.dto.TaskCommentResponse;
import com.sdkanban.task.dto.TaskResponse;
import com.sdkanban.task.dto.TaskTagResponse;
import com.sdkanban.task.dto.UpdateTaskRequest;
import com.sdkanban.task.dto.UpdateTaskPositionRequest;
import com.sdkanban.task.dto.UpdateTaskTagsRequest;

import java.util.List;

public interface TaskService {
    TaskResponse create(Long projectId, CreateTaskRequest request, Long currentUserId);

    TaskResponse detail(Long taskId, Long currentUserId);

    TaskResponse update(Long taskId, UpdateTaskRequest request, Long currentUserId);

    TaskResponse updatePosition(Long taskId, UpdateTaskPositionRequest request, Long currentUserId);

    TaskResponse archive(Long taskId, Long currentUserId);

    List<TaskResponse> archivedTasks(Long projectId, Long assigneeId, String type, String priority, String keyword, Long currentUserId);

    TaskResponse restore(Long taskId, Long currentUserId);

    void delete(Long taskId, Long currentUserId);

    TaskTagResponse createTag(Long projectId, CreateTaskTagRequest request, Long currentUserId);

    TaskResponse updateTags(Long taskId, UpdateTaskTagsRequest request, Long currentUserId);

    TaskCommentResponse addComment(Long taskId, AddTaskCommentRequest request, Long currentUserId);

    List<TaskCommentResponse> comments(Long taskId, Long currentUserId);

    List<TaskActivityResponse> activities(Long taskId, Long currentUserId);
}
