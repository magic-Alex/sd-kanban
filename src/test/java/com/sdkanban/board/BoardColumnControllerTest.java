package com.sdkanban.board;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BoardColumnControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void deleteData() {
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
    void creatingProjectCreatesDefaultColumns() throws Exception {
        RegisteredUser owner = register("owner", "Owner");

        long projectId = createProject(owner.token(), "Delivery", "Delivery board");

        List<String> names = jdbcTemplate.query(
            "SELECT name FROM board_columns WHERE project_id = ? ORDER BY sort_order",
            (rs, rowNum) -> rs.getString("name"),
            projectId
        );
        assertThat(names).containsExactly("Backlog", "Ready", "In Progress", "Testing", "Done");

        mockMvc.perform(get("/api/projects/{projectId}/columns", projectId)
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(5))
            .andExpect(jsonPath("$.data[0].name").value("Backlog"))
            .andExpect(jsonPath("$.data[4].isDone").value(true));
    }

    @Test
    void projectOwnerCannotCreateColumnsBecauseGlobalTemplatesAreRequired() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");

        mockMvc.perform(post("/api/projects/{projectId}/columns", projectId)
                .header("Authorization", "Bearer " + owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Blocked",
                      "color": "#dc2626",
                      "wipLimit": 3,
                      "isDone": false
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("GLOBAL_TEMPLATE_REQUIRED"));
    }

    @Test
    void ordinaryMemberCannotManageColumns() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");
        addMember(owner.token(), projectId, member.id());

        mockMvc.perform(post("/api/projects/{projectId}/columns", projectId)
                .header("Authorization", "Bearer " + member.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Blocked"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT_OWNER_REQUIRED"));
    }

    @Test
    void nonEmptyColumnDeletionReturnsConflict() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");
        jdbcTemplate.update(
            """
            INSERT INTO board_columns (project_id, template_key, name, color, sort_order, is_done)
            VALUES (?, 'CUSTOM_WORK', 'Work', '#64748b', 100, false)
            """,
            projectId
        );
        long columnId = jdbcTemplate.queryForObject(
            "SELECT id FROM board_columns WHERE project_id = ? AND name = 'Work'",
            Long.class,
            projectId
        );
        jdbcTemplate.update(
            """
            INSERT INTO tasks (project_id, column_id, creator_id, title, priority, task_type, sort_order)
            VALUES (?, ?, ?, 'Keep me', 'MEDIUM', 'TASK', 0)
            """,
            projectId,
            columnId,
            owner.id()
        );

        mockMvc.perform(delete("/api/projects/{projectId}/columns/{columnId}", projectId, columnId)
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("GLOBAL_TEMPLATE_REQUIRED"));
    }

    private long createProject(String token, String name, String description) throws Exception {
        String response = mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s",
                      "description": "%s"
                    }
                    """.formatted(name, description)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private void addMember(String ownerToken, long projectId, long userId) throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": %d
                    }
                    """.formatted(userId)))
            .andExpect(status().isOk());
    }

    private RegisteredUser register(String account, String nickname) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "account": "%s",
                      "nickname": "%s",
                      "email": "%s@example.com",
                      "password": "secret123"
                    }
                    """.formatted(account, nickname, account)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        long userId = root.path("data").path("user").path("id").asLong();
        String token = root.path("data").path("token").asText();
        return new RegisteredUser(userId, token);
    }

    private record RegisteredUser(long id, String token) {
    }
}
