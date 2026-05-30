package com.sdkanban.settings.repository;

import com.sdkanban.settings.entity.BoardColumnTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardColumnTemplateRepository extends JpaRepository<BoardColumnTemplate, Long> {
    List<BoardColumnTemplate> findByOrderBySortOrderAscIdAsc();

    Optional<BoardColumnTemplate> findByTemplateKey(String templateKey);

    boolean existsByTemplateKey(String templateKey);
}
