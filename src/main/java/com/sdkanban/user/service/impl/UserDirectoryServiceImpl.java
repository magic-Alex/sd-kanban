package com.sdkanban.user.service.impl;

import com.sdkanban.user.dto.UserSummary;
import com.sdkanban.user.repository.UserRepository;
import com.sdkanban.user.service.UserDirectoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class UserDirectoryServiceImpl implements UserDirectoryService {
    private final UserRepository userRepository;

    public UserDirectoryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> search(String keyword) {
        return userRepository.searchActiveUsers(normalizedKeyword(keyword)).stream()
            .map(UserSummary::from)
            .toList();
    }

    private String normalizedKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim();
    }
}
