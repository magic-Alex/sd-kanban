package com.sdkanban.user.repository;

import com.sdkanban.user.entity.User;
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

    @Query("""
        select user
        from User user
        where user.status = 'ACTIVE'
          and (
            :keyword is null
            or :keyword = ''
            or lower(user.account) like lower(concat('%', :keyword, '%'))
            or lower(user.nickname) like lower(concat('%', :keyword, '%'))
            or lower(user.email) like lower(concat('%', :keyword, '%'))
          )
        order by user.nickname asc, user.account asc
        """)
    List<User> searchActiveUsers(@Param("keyword") String keyword);
}
