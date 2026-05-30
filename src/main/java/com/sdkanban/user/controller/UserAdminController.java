package com.sdkanban.user.controller;

import com.sdkanban.common.ApiResponse;
import com.sdkanban.common.BusinessException;
import com.sdkanban.user.dto.CreateUserRequest;
import com.sdkanban.user.dto.UpdateUserStatusRequest;
import com.sdkanban.user.dto.UserAdminResponse;
import com.sdkanban.user.entity.User;
import com.sdkanban.user.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {
    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    ApiResponse<List<UserAdminResponse>> list(@AuthenticationPrincipal User user) {
        requireAdmin(user);
        return ApiResponse.ok(userAdminService.list());
    }

    @PostMapping
    ApiResponse<UserAdminResponse> create(
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateUserRequest request
    ) {
        requireAdmin(user);
        return ApiResponse.ok(userAdminService.create(request));
    }

    @PatchMapping("/{account}/status")
    ApiResponse<UserAdminResponse> updateStatus(
        @AuthenticationPrincipal User user,
        @PathVariable String account,
        @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        requireAdmin(user);
        return ApiResponse.ok(userAdminService.updateStatus(account, request));
    }

    private void requireAdmin(User user) {
        if (user == null || !user.isAdmin()) {
            throw BusinessException.forbidden("FORBIDDEN", "Access denied");
        }
    }
}
