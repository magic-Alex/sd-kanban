package com.sdkanban.user.service.impl;

import com.sdkanban.common.BusinessException;
import com.sdkanban.user.dto.CreateUserRequest;
import com.sdkanban.user.dto.UpdateUserStatusRequest;
import com.sdkanban.user.dto.UserAdminResponse;
import com.sdkanban.user.entity.User;
import com.sdkanban.user.repository.UserRepository;
import com.sdkanban.user.service.UserAdminService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserAdminServiceImpl implements UserAdminService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAdminResponse> list() {
        return userRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
            .map(UserAdminResponse::from)
            .toList();
    }

    @Override
    @Transactional
    public UserAdminResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByAccount(request.account())) {
            throw BusinessException.conflict("ACCOUNT_EXISTS", "Account already exists");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw BusinessException.conflict("EMAIL_EXISTS", "Email already exists");
        }

        User user = userRepository.save(new User(
            request.account().trim(),
            request.nickname().trim(),
            email,
            passwordEncoder.encode(request.password()),
            normalizeRole(request.role())
        ));
        return UserAdminResponse.from(user);
    }

    @Override
    @Transactional
    public UserAdminResponse updateStatus(String account, UpdateUserStatusRequest request) {
        User user = userRepository.findByAccount(account)
            .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "User not found"));
        user.changeStatus(normalizeStatus(request.status()));
        return UserAdminResponse.from(user);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim();
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "MEMBER";
        }
        String normalized = role.trim().toUpperCase();
        if (!List.of("ADMIN", "MEMBER").contains(normalized)) {
            throw BusinessException.badRequest("INVALID_ROLE", "Role must be ADMIN or MEMBER");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!List.of("ACTIVE", "DISABLED").contains(normalized)) {
            throw BusinessException.badRequest("INVALID_STATUS", "Status must be ACTIVE or DISABLED");
        }
        return normalized;
    }
}
