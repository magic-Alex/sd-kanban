package com.sdkanban.task.repository;

import com.sdkanban.task.entity.TaskActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskActivityRepository extends JpaRepository<TaskActivity, Long> {
}
