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

    public AuthServiceImpl(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByAccount(request.account())) {
            throw BusinessException.conflict("ACCOUNT_EXISTS", "Account already exists");
        }
        if (StringUtils.hasText(request.email()) && userRepository.existsByEmail(request.email())) {
            throw BusinessException.conflict("EMAIL_EXISTS", "Email already exists");
        }

        User user = userRepository.save(new User(
            request.account(),
            request.nickname(),
            request.email(),
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
}
