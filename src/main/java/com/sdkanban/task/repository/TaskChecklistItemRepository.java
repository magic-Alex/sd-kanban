package com.sdkanban.task.repository;

import com.sdkanban.task.entity.TaskChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskChecklistItemRepository extends JpaRepository<TaskChecklistItem, Long> {
    List<TaskChecklistItem> findByTaskIdAndProjectIdOrderBySortOrderAscIdAsc(Long taskId, Long projectId);

    Optional<TaskChecklistItem> findByIdAndTaskIdAndProjectId(Long id, Long taskId, Long projectId);

    @Query("""
        select coalesce(max(item.sortOrder), -1)
        from TaskChecklistItem item
        where item.taskId = :taskId
          and item.projectId = :projectId
        """)
    int maxSortOrder(@Param("taskId") Long taskId, @Param("projectId") Long projectId);

    @Query("""
        select item.taskId as taskId,
               sum(case when item.done = true then 1 else 0 end) as doneCount,
               count(item) as totalCount
        from TaskChecklistItem item
        where item.taskId in :taskIds
        group by item.taskId
        """)
    List<ChecklistCountView> countByTaskIds(@Param("taskIds") Collection<Long> taskIds);

    interface ChecklistCountView {
        Long getTaskId();

        Long getDoneCount();

        Long getTotalCount();
    }
}
