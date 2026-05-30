package com.sdkanban.user.service.impl;

import com.sdkanban.user.dto.UserDirectoryResponse;
import com.sdkanban.user.repository.UserRepository;
import com.sdkanban.user.service.UserDirectoryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserDirectoryServiceImpl implements UserDirectoryService {
    private static final int RESULT_LIMIT = 20;

    private final UserRepository userRepository;

    public UserDirectoryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDirectoryResponse> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        return userRepository.searchActiveUsers(escapedKeyword(keyword.trim()), PageRequest.of(0, RESULT_LIMIT)).stream()
            .map(UserDirectoryResponse::from)
            .toList();
    }

    private String escapedKeyword(String keyword) {
        return keyword
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
