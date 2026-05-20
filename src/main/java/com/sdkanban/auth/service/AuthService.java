package com.sdkanban.auth.service;

import com.sdkanban.auth.dto.AuthResponse;
import com.sdkanban.auth.dto.LoginRequest;
import com.sdkanban.auth.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
