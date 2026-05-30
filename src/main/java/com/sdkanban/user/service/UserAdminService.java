package com.sdkanban.user.service;

import com.sdkanban.user.dto.CreateUserRequest;
import com.sdkanban.user.dto.UpdateUserStatusRequest;
import com.sdkanban.user.dto.UserAdminResponse;

import java.util.List;

public interface UserAdminService {
    List<UserAdminResponse> list();

    UserAdminResponse create(CreateUserRequest request);

    UserAdminResponse updateStatus(String account, UpdateUserStatusRequest request);
}
