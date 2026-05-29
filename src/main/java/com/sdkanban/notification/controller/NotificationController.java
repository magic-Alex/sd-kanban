package com.sdkanban.notification.controller;

import com.sdkanban.common.ApiResponse;
import com.sdkanban.notification.dto.NotificationResponse;
import com.sdkanban.notification.dto.UnreadNotificationCountResponse;
import com.sdkanban.notification.service.NotificationService;
import com.sdkanban.project.repository.ProjectPersistenceAvailableCondition;
import com.sdkanban.user.entity.User;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Conditional(ProjectPersistenceAvailableCondition.class)
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    ApiResponse<List<NotificationResponse>> list(
        @RequestParam(defaultValue = "all") String status,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(notificationService.list(status, currentUserId(user)));
    }

    @GetMapping("/unread-count")
    ApiResponse<UnreadNotificationCountResponse> unreadCount(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(notificationService.unreadCount(currentUserId(user)));
    }

    @PatchMapping("/{notificationId}/read")
    ApiResponse<NotificationResponse> markRead(@PathVariable Long notificationId, @AuthenticationPrincipal User user) {
        return ApiResponse.ok(notificationService.markRead(notificationId, currentUserId(user)));
    }

    @PatchMapping("/read-all")
    ApiResponse<Void> markAllRead(@AuthenticationPrincipal User user) {
        notificationService.markAllRead(currentUserId(user));
        return ApiResponse.ok(null);
    }

    private Long currentUserId(User user) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return user.getId();
    }
}
