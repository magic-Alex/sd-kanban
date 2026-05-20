package com.sdkanban.project.repository;

import com.sdkanban.project.entity.ProjectMember;
import com.sdkanban.project.entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    boolean existsByIdProjectIdAndIdUserId(Long projectId, Long userId);

    List<ProjectMember> findByIdProjectIdOrderByCreatedAtAsc(Long projectId);

    List<ProjectMember> findByIdProjectIdAndRole(Long projectId, String role);

    long countByIdProjectId(Long projectId);
}
