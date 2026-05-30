package com.sdkanban.user.dto;

import com.sdkanban.user.entity.User;

import java.time.LocalDateTime;

public record UserAdminResponse(
    Long id,
    String account,
    String nickname,
    String email,
    String avatarUrl,
    String status,
    String role,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static UserAdminResponse from(User user) {
        return new UserAdminResponse(
            user.getId(),
            user.getAccount(),
            user.getNickname(),
            user.getEmail(),
            user.getAvatarUrl(),
            user.getStatus(),
            user.getRole(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
