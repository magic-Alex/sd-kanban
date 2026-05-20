package com.sdkanban.sprint.service;

import com.sdkanban.sprint.dto.CreateSprintRequest;
import com.sdkanban.sprint.dto.SprintResponse;
import com.sdkanban.sprint.dto.UpdateSprintRequest;

import java.util.List;

public interface SprintService {
    List<SprintResponse> list(Long projectId, Long currentUserId);

    SprintResponse create(Long projectId, CreateSprintRequest request, Long currentUserId);

    SprintResponse update(Long projectId, Long sprintId, UpdateSprintRequest request, Long currentUserId);

    void delete(Long projectId, Long sprintId, Long currentUserId);
}
