package com.sdkanban.task.repository;

import com.sdkanban.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("select coalesce(max(task.sortOrder), -1) from Task task where task.projectId = :projectId and task.columnId = :columnId")
    int maxSortOrderInColumn(@Param("projectId") Long projectId, @Param("columnId") Long columnId);
}
