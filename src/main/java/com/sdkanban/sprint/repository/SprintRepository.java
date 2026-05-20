package com.sdkanban.sprint.repository;

import com.sdkanban.sprint.entity.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProjectIdOrderByCreatedAtDescIdDesc(Long projectId);

    Optional<Sprint> findByIdAndProjectId(Long id, Long projectId);
}
