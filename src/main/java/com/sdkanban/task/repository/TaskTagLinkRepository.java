package com.sdkanban.task.repository;

import com.sdkanban.task.entity.TaskTagLink;
import com.sdkanban.task.entity.TaskTagLinkId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskTagLinkRepository extends JpaRepository<TaskTagLink, TaskTagLinkId> {
    void deleteByIdTaskIdAndIdProjectId(Long taskId, Long projectId);

    List<TaskTagLink> findByIdTaskIdAndIdProjectId(Long taskId, Long projectId);
}
