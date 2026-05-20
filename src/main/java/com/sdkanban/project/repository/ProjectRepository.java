package com.sdkanban.project.repository;

import com.sdkanban.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    @Query("""
        select distinct project
        from Project project
        left join ProjectMember member on member.id.projectId = project.id
        where project.ownerId = :userId or member.id.userId = :userId
        order by project.createdAt desc, project.id desc
        """)
    List<Project> findVisibleToUser(@Param("userId") Long userId);
}
