package com.sdkanban.notification.service;

import com.sdkanban.notification.dto.NotificationResponse;
import com.sdkanban.notification.dto.UnreadNotificationCountResponse;

import java.util.Collection;
import java.util.List;

public interface NotificationService {
    List<NotificationResponse> list(String status, Long currentUserId);

    UnreadNotificationCountResponse unreadCount(Long currentUserId);

    NotificationResponse markRead(Long notificationId, Long currentUserId);

    void markAllRead(Long currentUserId);

    void notifyUsers(Collection<Long> recipientIds, Long actorId, Long projectId, Long taskId, String type, String title, String content);
}
