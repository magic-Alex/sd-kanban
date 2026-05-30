package com.sdkanban.user.repository;

import com.sdkanban.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByAccount(String account);

    boolean existsByEmail(String email);

    Optional<User> findByAccount(String account);

    List<User> findAllByOrderByCreatedAtDescIdDesc();

    @Query(value = """
        SELECT *
        FROM users u
        WHERE u.status = 'ACTIVE'
          AND (
            LOWER(u.account) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\\\'
            OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\\\'
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\\\'
          )
        ORDER BY u.nickname ASC, u.account ASC
        """, nativeQuery = true)
    List<User> searchActiveUsers(@Param("keyword") String keyword, Pageable pageable);
}
