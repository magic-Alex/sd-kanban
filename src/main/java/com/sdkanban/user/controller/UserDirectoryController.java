package com.sdkanban.user.controller;

import com.sdkanban.common.ApiResponse;
import com.sdkanban.user.dto.UserDirectoryResponse;
import com.sdkanban.user.entity.User;
import com.sdkanban.user.service.UserDirectoryService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/directory")
public class UserDirectoryController {
    private final UserDirectoryService userDirectoryService;

    public UserDirectoryController(UserDirectoryService userDirectoryService) {
        this.userDirectoryService = userDirectoryService;
    }

    @GetMapping
    ApiResponse<List<UserDirectoryResponse>> search(
        @RequestParam(required = false) String keyword,
        @AuthenticationPrincipal User user
    ) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return ApiResponse.ok(userDirectoryService.search(keyword));
    }
}
