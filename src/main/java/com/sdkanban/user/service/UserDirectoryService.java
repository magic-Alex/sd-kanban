package com.sdkanban.user.service;

import com.sdkanban.user.dto.UserDirectoryResponse;

import java.util.List;

public interface UserDirectoryService {
    List<UserDirectoryResponse> search(String keyword);
}
