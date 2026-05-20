package com.sdkanban.board.repository;

import com.sdkanban.board.entity.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, Long> {
    List<BoardColumn> findByProjectIdOrderBySortOrderAscIdAsc(Long projectId);

    Optional<BoardColumn> findByIdAndProjectId(Long id, Long projectId);

    @Query("select coalesce(max(column.sortOrder), -1) from BoardColumn column where column.projectId = :projectId")
    int maxSortOrderByProjectId(@Param("projectId") Long projectId);

    @Query(value = "SELECT COUNT(*) FROM tasks WHERE project_id = :projectId AND column_id = :columnId", nativeQuery = true)
    long countTasksInColumn(@Param("projectId") Long projectId, @Param("columnId") Long columnId);
}
