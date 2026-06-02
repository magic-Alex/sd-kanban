package com.sdkanban.user;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserAdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void deleteUsers() {
        jdbcTemplate.update("DELETE FROM task_activities");
        jdbcTemplate.update("DELETE FROM task_comments");
        jdbcTemplate.update("DELETE FROM task_tag_links");
        jdbcTemplate.update("DELETE FROM tasks");
        jdbcTemplate.update("DELETE FROM task_tags");
        jdbcTemplate.update("DELETE FROM board_columns");
        jdbcTemplate.update("DELETE FROM sprints");
        jdbcTemplate.update("DELETE FROM project_members");
        jdbcTemplate.update("DELETE FROM projects");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void adminCreatesUserAndCanListUsers() throws Exception {
        String adminToken = seedUserAndLogin("admin-user", "管理员", "ADMIN");

        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "account": "developer",
                      "nickname": "Developer",
                      "email": "developer@example.com",
                      "password": "1",
                      "role": "MEMBER"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.account").value("developer"))
            .andExpect(jsonPath("$.data.nickname").value("Developer"))
            .andExpect(jsonPath("$.data.role").value("MEMBER"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[?(@.account == 'admin-user')].role").value("ADMIN"))
            .andExpect(jsonPath("$.data[?(@.account == 'developer')].status").value("ACTIVE"));

        String hash = jdbcTemplate.queryForObject(
            "SELECT password_hash FROM users WHERE account = ?",
            String.class,
            "developer"
        );
        assertThat(passwordEncoder.matches("1", hash)).isTrue();
    }

    @Test
    void memberCannotCreateUsers() throws Exception {
        String memberToken = seedUserAndLogin("member", "Member", "MEMBER");

        mockMvc.perform(post("/api/admin/users")
                .header("Authorization", "Bearer " + memberToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "account": "blocked",
                      "nickname": "Blocked",
                      "email": "blocked@example.com",
                      "password": "1",
                      "role": "MEMBER"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanDisableAndEnableUsers() throws Exception {
        String adminToken = seedUserAndLogin("admin-user", "管理员", "ADMIN");
        seedUser("developer", "Developer", "MEMBER");

        mockMvc.perform(patch("/api/admin/users/{account}/status", "developer")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "DISABLED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(patch("/api/admin/users/{account}/status", "developer")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "ACTIVE"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    private String seedUserAndLogin(String account, String nickname, String role) throws Exception {
        seedUser(account, nickname, role);

        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "account": "%s",
                      "password": "1"
                    }
                    """.formatted(account)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("token").asText();
    }

    private void seedUser(String account, String nickname, String role) {
        jdbcTemplate.update(
            """
                INSERT INTO users (account, nickname, email, password_hash, status, role)
                VALUES (?, ?, ?, ?, 'ACTIVE', ?)
                """,
            account,
            nickname,
            account + "@example.com",
            passwordEncoder.encode("1"),
            role
        );
    }
}
