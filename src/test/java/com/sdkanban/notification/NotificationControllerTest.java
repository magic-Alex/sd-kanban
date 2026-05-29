package com.sdkanban.notification;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void deleteData() {
        jdbcTemplate.update("DELETE FROM notifications");
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
    void userCanListAndReadOwnNotifications() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");
        long taskId = createTask(owner.token(), projectId, firstColumnId(projectId), "Notify me");
        jdbcTemplate.update(
            """
            INSERT INTO notifications (recipient_id, actor_id, project_id, task_id, type, title, content)
            VALUES (?, ?, ?, ?, 'MENTION', 'New mention', 'Owner mentioned you')
            """,
            owner.id(), owner.id(), projectId, taskId
        );

        mockMvc.perform(get("/api/notifications/unread-count")
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.count").value(1));

        String response = mockMvc.perform(get("/api/notifications?status=unread")
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].type").value("MENTION"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long notificationId = objectMapper.readTree(response).path("data").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.read").value(true));
    }

    @Test
    void userCannotReadAnotherUsersNotification() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser other = register("other", "Other");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");
        long taskId = createTask(owner.token(), projectId, firstColumnId(projectId), "Private notification");
        jdbcTemplate.update(
            """
            INSERT INTO notifications (recipient_id, actor_id, project_id, task_id, type, title, content)
            VALUES (?, ?, ?, ?, 'MENTION', 'New mention', 'Owner mentioned you')
            """,
            owner.id(), owner.id(), projectId, taskId
        );
        Long notificationId = jdbcTemplate.queryForObject(
            "SELECT id FROM notifications WHERE recipient_id = ?",
            Long.class,
            owner.id()
        );

        mockMvc.perform(get("/api/notifications")
                .header("Authorization", "Bearer " + other.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                .header("Authorization", "Bearer " + other.token()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    void listReturnsMostRecentFiftyWithActorSummary() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");
        long taskId = createTask(owner.token(), projectId, firstColumnId(projectId), "Many notifications");
        for (int i = 1; i <= 55; i++) {
            jdbcTemplate.update(
                """
                INSERT INTO notifications (recipient_id, actor_id, project_id, task_id, type, title, content)
                VALUES (?, ?, ?, ?, 'MENTION', ?, ?)
                """,
                owner.id(),
                owner.id(),
                projectId,
                taskId,
                "Notification " + i,
                "Content " + i
            );
        }

        String response = mockMvc.perform(get("/api/notifications")
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(50))
            .andExpect(jsonPath("$.data[0].title").value("Notification 55"))
            .andExpect(jsonPath("$.data[0].actor.id").value(owner.id()))
            .andExpect(jsonPath("$.data[0].actor.nickname").value("Owner"))
            .andExpect(jsonPath("$.data[49].title").value("Notification 6"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(objectMapper.readTree(response).path("data")).hasSize(50);
    }

    @Test
    void markAllReadOnlyMarksCurrentUsersUnreadNotifications() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser other = register("other", "Other");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");
        long taskId = createTask(owner.token(), projectId, firstColumnId(projectId), "Read all");
        jdbcTemplate.update(
            """
            INSERT INTO notifications (recipient_id, actor_id, project_id, task_id, type, title, content)
            VALUES (?, ?, ?, ?, 'MENTION', 'Owner notification', 'Owner content')
            """,
            owner.id(), owner.id(), projectId, taskId
        );
        jdbcTemplate.update(
            """
            INSERT INTO notifications (recipient_id, actor_id, project_id, task_id, type, title, content)
            VALUES (?, ?, ?, ?, 'MENTION', 'Other notification', 'Other content')
            """,
            other.id(), owner.id(), projectId, taskId
        );

        mockMvc.perform(patch("/api/notifications/read-all")
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND is_read = false",
            Integer.class,
            owner.id()
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND is_read = false",
            Integer.class,
            other.id()
        )).isEqualTo(1);
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
