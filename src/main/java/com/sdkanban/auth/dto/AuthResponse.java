package com.sdkanban.auth.dto;

import com.sdkanban.user.dto.UserSummary;

public record AuthResponse(String token, UserSummary user) {
}
