package com.sdkanban.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.auth.default-admin.account=sd-robot",
    "app.auth.default-admin.password=1",
    "app.auth.default-admin.nickname=系统管理员"
})
class DefaultAdminInitializerTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void startupEnsuresDefaultAdministratorExists() {
        AdminRow row = jdbcTemplate.queryForObject(
            """
                SELECT account, nickname, password_hash, status, role
                FROM users
                WHERE account = 'sd-robot'
                """,
            (rs, rowNum) -> new AdminRow(
                rs.getString("account"),
                rs.getString("nickname"),
                rs.getString("password_hash"),
                rs.getString("status"),
                rs.getString("role")
            )
        );

        assertThat(row.account()).isEqualTo("sd-robot");
        assertThat(row.nickname()).isEqualTo("系统管理员");
        assertThat(row.status()).isEqualTo("ACTIVE");
        assertThat(row.role()).isEqualTo("ADMIN");
        assertThat(passwordEncoder.matches("1", row.passwordHash())).isTrue();
    }

    private record AdminRow(String account, String nickname, String passwordHash, String status, String role) {
    }
}
