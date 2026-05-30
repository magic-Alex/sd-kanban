package com.sdkanban.user.dto;

import com.sdkanban.user.entity.User;

public record UserDirectoryResponse(
    Long id,
    String account,
    String nickname,
    String avatarUrl
) {
    public static UserDirectoryResponse from(User user) {
        return new UserDirectoryResponse(
            user.getId(),
            user.getAccount(),
            user.getNickname(),
            user.getAvatarUrl()
        );
    }
}
