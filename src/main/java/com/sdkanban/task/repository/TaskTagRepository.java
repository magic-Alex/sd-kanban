package com.sdkanban.task.repository;

import com.sdkanban.task.entity.TaskTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskTagRepository extends JpaRepository<TaskTag, Long> {
    Optional<TaskTag> findByIdAndProjectId(Long id, Long projectId);

    List<TaskTag> findByProjectIdAndIdIn(Long projectId, Collection<Long> ids);
}
