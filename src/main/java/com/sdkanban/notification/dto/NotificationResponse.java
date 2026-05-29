package com.sdkanban.notification.dto;

import com.sdkanban.notification.entity.Notification;
import com.sdkanban.user.dto.UserSummary;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    UserSummary actor,
    Long projectId,
    Long taskId,
    String type,
    String title,
    String content,
    boolean read,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {
    public static NotificationResponse from(Notification notification, UserSummary actor) {
        return new NotificationResponse(
            notification.getId(),
            actor,
            notification.getProjectId(),
            notification.getTaskId(),
            notification.getType(),
            notification.getTitle(),
            notification.getContent(),
            notification.isRead(),
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }
}
