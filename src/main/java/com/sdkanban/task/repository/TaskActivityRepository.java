package com.sdkanban.task.repository;

import com.sdkanban.task.entity.TaskActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {
    List<TaskActivity> findByTaskIdAndProjectIdOrderByCreatedAtDescIdDesc(Long taskId, Long projectId);
}
