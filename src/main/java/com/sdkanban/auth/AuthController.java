package com.sdkanban.auth;

import com.sdkanban.common.ApiResponse;
import com.sdkanban.user.User;
import com.sdkanban.user.UserSummary;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final ObjectProvider<AuthService> authServiceProvider;

    public AuthController(ObjectProvider<AuthService> authServiceProvider) {
        this.authServiceProvider = authServiceProvider;
    }

    @PostMapping("/register")
    ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService().register(request));
    }

    @PostMapping("/login")
    ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService().login(request));
    }

    @GetMapping("/me")
    ApiResponse<UserSummary> me(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(UserSummary.from(user));
    }

    private AuthService authService() {
        return authServiceProvider.getObject();
    }
}
