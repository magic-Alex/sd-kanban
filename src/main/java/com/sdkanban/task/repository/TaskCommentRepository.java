package com.sdkanban.task.repository;

import com.sdkanban.task.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
    List<TaskComment> findByTaskIdOrderByCreatedAtDescIdDesc(Long taskId);
}
