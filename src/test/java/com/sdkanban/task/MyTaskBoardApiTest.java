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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MyTaskBoardApiTest {
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
        jdbcTemplate.update("DELETE FROM task_checklist_items");
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
    void myTaskBoardGroupedByProjectReturnsOnlyTasksAssignedToCurrentUser() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        List<Long> columns = columnIds(fixture.projectId());
        long mine = createTask(fixture.member().token(), fixture.projectId(), columns.get(0), fixture.member().id(), "Mine");
        createTask(fixture.member().token(), fixture.projectId(), columns.get(0), fixture.owner().id(), "Someone else's");

        mockMvc.perform(get("/api/tasks/mine/board")
                .queryParam("groupBy", "project")
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.groupBy").value("project"))
            .andExpect(jsonPath("$.data.groups.length()").value(1))
            .andExpect(jsonPath("$.data.groups[0].id").value(fixture.projectId()))
            .andExpect(jsonPath("$.data.groups[0].tasks.length()").value(1))
            .andExpect(jsonPath("$.data.groups[0].tasks[0].id").value(mine));
    }

    @Test
    void myTaskBoardCanGroupByColumnState() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        List<Long> columns = columnIds(fixture.projectId());
        createTask(fixture.member().token(), fixture.projectId(), columns.get(0), fixture.member().id(), "Backlog mine");
        createTask(fixture.member().token(), fixture.projectId(), columns.get(2), fixture.member().id(), "Progress mine");

        mockMvc.perform(get("/api/tasks/mine/board")
                .queryParam("groupBy", "column")
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.groupBy").value("column"))
            .andExpect(jsonPath("$.data.groups.length()").value(2))
            .andExpect(jsonPath("$.data.groups[?(@.name == 'Backlog')].tasks[0].title").value("Backlog mine"))
            .andExpect(jsonPath("$.data.groups[?(@.name == 'In Progress')].tasks[0].title").value("Progress mine"));
    }

    @Test
    void myTaskBoardExcludesArchivedTasks() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(
            fixture.member().token(),
            fixture.projectId(),
            columnIds(fixture.projectId()).get(0),
            fixture.member().id(),
            "Archived mine"
        );

        mockMvc.perform(patch("/api/tasks/{taskId}/archive", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/mine/board")
                .queryParam("groupBy", "project")
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groups.length()").value(0));
    }

    @Test
    void myTaskBoardExcludesDeletedTasks() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(
            fixture.member().token(),
            fixture.projectId(),
            columnIds(fixture.projectId()).get(0),
            fixture.member().id(),
            "Deleted mine"
        );

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/mine/board")
                .queryParam("groupBy", "project")
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groups.length()").value(0));
    }

    @Test
    void myTaskBoardCardsIncludeChecklistProgress() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(
            fixture.member().token(),
            fixture.projectId(),
            columnIds(fixture.projectId()).get(0),
            fixture.member().id(),
            "Checklist mine"
        );
        long doneItemId = createChecklistItem(fixture.member().token(), taskId, "Done item");
        createChecklistItem(fixture.member().token(), taskId, "Open item");

        mockMvc.perform(patch("/api/tasks/{taskId}/checklist/{itemId}/toggle", taskId, doneItemId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/mine/board")
                .queryParam("groupBy", "project")
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groups[0].tasks[0].checklistDoneCount").value(1))
            .andExpect(jsonPath("$.data.groups[0].tasks[0].checklistTotalCount").value(2));
    }

    private Fixture fixtureWithOwnerAndMember() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");
        addMember(owner.token(), projectId, member.id());
        return new Fixture(owner, member, projectId);
    }

    private long createTask(String token, long projectId, long columnId, long assigneeId, String title) throws Exception {
        String response = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "%s",
                      "columnId": %d,
                      "assigneeId": %d
                    }
                    """.formatted(title, columnId, assigneeId)))
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

    private List<Long> columnIds(long projectId) {
        return jdbcTemplate.query(
            "SELECT id FROM board_columns WHERE project_id = ? ORDER BY sort_order",
            (rs, rowNum) -> rs.getLong("id"),
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
        return new RegisteredUser(root.path("data").path("user").path("id").asLong(), root.path("data").path("token").asText());
    }

    private record RegisteredUser(long id, String token) {
    }

    private record Fixture(RegisteredUser owner, RegisteredUser member, long projectId) {
    }
}
