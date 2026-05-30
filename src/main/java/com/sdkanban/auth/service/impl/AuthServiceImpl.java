package com.sdkanban.auth.service.impl;

import com.sdkanban.auth.dto.AuthResponse;
import com.sdkanban.auth.dto.LoginRequest;
import com.sdkanban.auth.dto.RegisterRequest;
import com.sdkanban.auth.service.AuthService;
import com.sdkanban.common.BusinessException;
import com.sdkanban.config.JwtService;
import com.sdkanban.user.dto.UserSummary;
import com.sdkanban.user.entity.User;
import com.sdkanban.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final boolean registrationEnabled;

    public AuthServiceImpl(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        @Value("${app.auth.registration-enabled:false}") boolean registrationEnabled
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.registrationEnabled = registrationEnabled;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!registrationEnabled) {
            throw BusinessException.forbidden("REGISTRATION_DISABLED", "Public registration is disabled");
        }
        String email = normalizeEmail(request.email());
        if (userRepository.existsByAccount(request.account())) {
            throw BusinessException.conflict("ACCOUNT_EXISTS", "Account already exists");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw BusinessException.conflict("EMAIL_EXISTS", "Email already exists");
        }

        User user = userRepository.save(new User(
            request.account(),
            request.nickname(),
            email,
            passwordEncoder.encode(request.password())
        ));

        return responseFor(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByAccount(request.account())
            .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
            .filter(candidate -> "ACTIVE".equals(candidate.getStatus()))
            .orElseThrow(() -> new BadCredentialsException("Invalid account or password"));

        return responseFor(user);
    }

    private AuthResponse responseFor(User user) {
        return new AuthResponse(jwtService.createToken(user), UserSummary.from(user));
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim();
    }
}
