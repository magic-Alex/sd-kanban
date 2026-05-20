package com.sdkanban.project.repository;

import com.sdkanban.project.entity.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("""
        select distinct project
        from Project project
        left join ProjectMember member on member.id.projectId = project.id
        where project.ownerId = :userId or member.id.userId = :userId
        order by project.createdAt desc, project.id desc
        """)
    List<Project> findVisibleToUser(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select project from Project project where project.id = :projectId")
    Optional<Project> findByIdForUpdate(@Param("projectId") Long projectId);
}
