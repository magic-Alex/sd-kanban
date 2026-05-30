package com.sdkanban.user.service;

import com.sdkanban.user.dto.UserSummary;

import java.util.List;

public interface UserDirectoryService {
    List<UserSummary> search(String keyword);
}
