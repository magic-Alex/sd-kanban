package com.sdkanban.auth.controller;

import com.sdkanban.auth.dto.AuthResponse;
import com.sdkanban.auth.dto.LoginRequest;
import com.sdkanban.auth.dto.RegisterRequest;
import com.sdkanban.auth.service.AuthService;
import com.sdkanban.common.ApiResponse;
import com.sdkanban.user.dto.UserSummary;
import com.sdkanban.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    ApiResponse<UserSummary> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return ApiResponse.ok(UserSummary.from(user));
    }
}
