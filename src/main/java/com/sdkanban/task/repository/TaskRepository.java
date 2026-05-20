package com.sdkanban.task.repository;

import com.sdkanban.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @Query("select coalesce(max(task.sortOrder), -1) from Task task where task.projectId = :projectId and task.columnId = :columnId")
    int maxSortOrderInColumn(@Param("projectId") Long projectId, @Param("columnId") Long columnId);

    @Query("""
        select task
        from Task task
        where task.projectId = :projectId
          and task.deleted = false
          and (:sprintId is null or task.sprintId = :sprintId)
          and (:assigneeId is null or task.assigneeId = :assigneeId)
          and (:taskType is null or task.taskType = :taskType)
          and (:priority is null or task.priority = :priority)
          and (
              :keyword is null
              or lower(task.title) like concat('%', :keyword, '%')
              or lower(coalesce(task.description, '')) like concat('%', :keyword, '%')
          )
        order by task.columnId asc, task.sortOrder asc, task.id asc
        """)
    List<Task> findProjectBoardTasks(
        @Param("projectId") Long projectId,
        @Param("sprintId") Long sprintId,
        @Param("assigneeId") Long assigneeId,
        @Param("taskType") String taskType,
        @Param("priority") String priority,
        @Param("keyword") String keyword
    );

    List<Task> findByAssigneeIdAndDeletedFalseOrderByProjectIdAscColumnIdAscSortOrderAscIdAsc(Long assigneeId);
}
