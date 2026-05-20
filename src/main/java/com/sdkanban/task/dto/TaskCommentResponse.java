package com.sdkanban.task.dto;

import com.sdkanban.task.entity.TaskComment;
import com.sdkanban.user.dto.UserSummary;

import java.time.LocalDateTime;

public record TaskCommentResponse(
    Long id,
    Long taskId,
    UserSummary author,
    String content,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TaskCommentResponse from(TaskComment comment, UserSummary author) {
        return new TaskCommentResponse(
            comment.getId(),
            comment.getTaskId(),
            author,
            comment.getContent(),
            comment.getCreatedAt(),
            comment.getUpdatedAt()
        );
    }
}
