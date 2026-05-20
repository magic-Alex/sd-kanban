package com.sdkanban.user;

public record UserSummary(
    Long id,
    String account,
    String nickname,
    String email,
    String avatarUrl
) {
    public static UserSummary from(User user) {
        return new UserSummary(
            user.getId(),
            user.getAccount(),
            user.getNickname(),
            user.getEmail(),
            user.getAvatarUrl()
        );
    }
}
