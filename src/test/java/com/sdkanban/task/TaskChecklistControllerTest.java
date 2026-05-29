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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskChecklistControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void deleteData() {
        jdbcTemplate.update("DELETE FROM task_activities");
        jdbcTemplate.update("DELETE FROM task_checklist_items");
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
    void projectMemberCanCreateAndListChecklistItems() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Checklist task");

        mockMvc.perform(post("/api/tasks/{taskId}/checklist", taskId)
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Write API tests"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("Write API tests"))
            .andExpect(jsonPath("$.data.done").value(false))
            .andExpect(jsonPath("$.data.sortOrder").value(0));

        mockMvc.perform(get("/api/tasks/{taskId}/checklist", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title").value("Write API tests"));
    }

    @Test
    void projectMemberCanToggleRenameDeleteAndReorderChecklistItems() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Checklist task");
        long firstItemId = createChecklistItem(fixture.member().token(), taskId, "First");
        long secondItemId = createChecklistItem(fixture.member().token(), taskId, "Second");

        mockMvc.perform(patch("/api/tasks/{taskId}/checklist/{itemId}/toggle", taskId, firstItemId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.done").value(true));

        mockMvc.perform(patch("/api/tasks/{taskId}/checklist/{itemId}", taskId, firstItemId)
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "First renamed"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("First renamed"));

        mockMvc.perform(patch("/api/tasks/{taskId}/checklist/reorder", taskId)
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "itemIds": [%d, %d]
                    }
                    """.formatted(secondItemId, firstItemId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].id").value(secondItemId))
            .andExpect(jsonPath("$.data[1].id").value(firstItemId));

        mockMvc.perform(delete("/api/tasks/{taskId}/checklist/{itemId}", taskId, firstItemId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM task_checklist_items WHERE id = ?",
            Integer.class,
            firstItemId
        )).isZero();
    }

    @Test
    void nonMemberCannotAccessChecklistItems() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        RegisteredUser outsider = register("outsider", "Outsider");
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Private checklist");

        mockMvc.perform(get("/api/tasks/{taskId}/checklist", taskId)
                .header("Authorization", "Bearer " + outsider.token()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("PROJECT_MEMBER_REQUIRED"));
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

    private long createChecklistItem(String token, long taskId, String title) throws Exception {
        String response = mockMvc.perform(post("/api/tasks/{taskId}/checklist", taskId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "%s"
                    }
                    """.formatted(title)))
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
