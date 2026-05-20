package com.sdkanban.auth;

import com.sdkanban.user.UserSummary;

public record AuthResponse(String token, UserSummary user) {
}
