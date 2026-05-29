package com.sdkanban.notification.service.impl;

import com.sdkanban.common.BusinessException;
import com.sdkanban.notification.dto.NotificationResponse;
import com.sdkanban.notification.dto.UnreadNotificationCountResponse;
import com.sdkanban.notification.entity.Notification;
import com.sdkanban.notification.repository.NotificationRepository;
import com.sdkanban.notification.service.NotificationService;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.user.dto.UserSummary;
import com.sdkanban.user.entity.User;
import com.sdkanban.user.repository.UserRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Conditional(ProjectPersistenceAvailableCondition.class)
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String status, Long currentUserId) {
        List<Notification> notifications = "unread".equalsIgnoreCase(status)
            ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDescIdDesc(currentUserId)
            : notificationRepository.findByRecipientIdOrderByCreatedAtDescIdDesc(currentUserId);
        return notifications.stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse unreadCount(Long currentUserId) {
        return new UnreadNotificationCountResponse(notificationRepository.countByRecipientIdAndReadFalse(currentUserId));
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long notificationId, Long currentUserId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, currentUserId)
            .orElseThrow(() -> BusinessException.notFound("NOTIFICATION_NOT_FOUND", "Notification not found"));
        notification.markRead();
        return toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllRead(Long currentUserId) {
        List<Notification> notifications = notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDescIdDesc(currentUserId);
        notifications.forEach(Notification::markRead);
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void notifyUsers(
        Collection<Long> recipientIds,
        Long actorId,
        Long projectId,
        Long taskId,
        String type,
        String title,
        String content
    ) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return;
        }
        Set<Long> recipients = recipientIds.stream()
            .filter(Objects::nonNull)
            .filter(recipientId -> !Objects.equals(recipientId, actorId))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (recipients.isEmpty()) {
            return;
        }
        notificationRepository.saveAll(recipients.stream()
            .map(recipientId -> new Notification(recipientId, actorId, projectId, taskId, type, title, content))
            .toList());
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.from(notification, actorSummary(notification.getActorId()));
    }

    private UserSummary actorSummary(Long actorId) {
        if (actorId == null) {
            return null;
        }
        User user = userRepository.findById(actorId)
            .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "User not found"));
        return UserSummary.from(user);
    }
}
