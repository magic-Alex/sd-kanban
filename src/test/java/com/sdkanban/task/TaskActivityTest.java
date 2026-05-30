package com.sdkanban.task;

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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskActivityTest {
    private static final AtomicInteger PROJECT_SEQUENCE = new AtomicInteger();

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
    void updatingTaskWritesActivityRowsForChangedFields() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Initial title");

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Updated title",
                      "priority": "HIGH"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("Updated title"))
            .andExpect(jsonPath("$.data.priority").value("HIGH"));

        List<String> fieldNames = jdbcTemplate.query(
            """
            SELECT field_name FROM task_activities
            WHERE task_id = ? AND action_type = 'TASK_UPDATED'
            ORDER BY field_name
            """,
            (rs, rowNum) -> rs.getString("field_name"),
            taskId
        );
        assertThat(fieldNames).containsExactly("priority", "title");
    }

    @Test
    void addingCommentCreatesCommentAndActivity() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Needs notes");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                .header("Authorization", "Bearer " + fixture.owner().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Please include acceptance examples."
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").value("Please include acceptance examples."))
            .andExpect(jsonPath("$.data.author.id").value(fixture.owner().id()));

        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM task_comments WHERE task_id = ? AND author_id = ?",
            Integer.class,
            taskId,
            fixture.owner().id()
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM task_activities WHERE task_id = ? AND action_type = 'COMMENT_ADDED'",
            Integer.class,
            taskId
        )).isEqualTo(1);
    }

    private Fixture fixtureWithOwnerAndMember() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");
        addMember(owner.token(), projectId, member.id());
        return new Fixture(owner, member, projectId);
    }

    private long createTask(String token, long projectId, long columnId, String title) throws Exception {
        String response = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "%s",
                      "columnId": %d
                    }
                    """.formatted(title, columnId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private long firstColumnId(long projectId) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM board_columns WHERE project_id = ? ORDER BY sort_order LIMIT 1",
            Long.class,
            projectId
        );
    }

    private long createProject(String token, String name, String description) throws Exception {
        int sequence = PROJECT_SEQUENCE.incrementAndGet();
        String response = mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s",
                      "description": "%s",
                      "projectCode": "ACT-%d",
                      "projectColor": "#0f766e"
                    }
                    """.formatted(name, description, sequence)))
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

    private record Fixture(RegisteredUser owner, RegisteredUser member, long projectId) {
    }
}
