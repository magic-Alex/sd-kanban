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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BoardApiTest {
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
    void projectBoardReturnsColumnsAndFilteredProjectTasks() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        List<Long> columns = columnIds(fixture.projectId());
        long backlogTaskId = createTask(
            fixture.member().token(),
            fixture.projectId(),
            columns.get(0),
            fixture.member().id(),
            null,
            "Backlog API",
            "TASK",
            "MEDIUM"
        );
        createTask(
            fixture.member().token(),
            fixture.projectId(),
            columns.get(1),
            fixture.member().id(),
            null,
            "Ready API",
            "TASK",
            "MEDIUM"
        );

        mockMvc.perform(get("/api/projects/{projectId}/board", fixture.projectId())
                .header("Authorization", "Bearer " + fixture.owner().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.projectId").value(fixture.projectId()))
            .andExpect(jsonPath("$.data.columns.length()").value(5))
            .andExpect(jsonPath("$.data.columns[0].name").value("Backlog"))
            .andExpect(jsonPath("$.data.columns[0].tasks[0].id").value(backlogTaskId))
            .andExpect(jsonPath("$.data.columns[0].tasks[0].assignee.id").value(fixture.member().id()))
            .andExpect(jsonPath("$.data.columns[0].tasks[0].assignee.nickname").value("member"))
            .andExpect(jsonPath("$.data.columns[1].tasks[0].title").value("Ready API"));
    }

    @Test
    void projectBoardSupportsSprintAssigneeTypePriorityAndKeywordFilters() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long sprintId = createSprint(fixture.member().token(), fixture.projectId(), "Sprint 1");
        long columnId = columnIds(fixture.projectId()).get(0);
        long expectedTaskId = createTask(
            fixture.member().token(),
            fixture.projectId(),
            columnId,
            fixture.member().id(),
            sprintId,
            "API filter target",
            "STORY",
            "HIGH"
        );
        createTask(
            fixture.member().token(),
            fixture.projectId(),
            columnId,
            fixture.member().id(),
            null,
            "Other item",
            "TASK",
            "LOW"
        );

        mockMvc.perform(get("/api/projects/{projectId}/board", fixture.projectId())
                .queryParam("sprintId", String.valueOf(sprintId))
                .queryParam("assigneeId", String.valueOf(fixture.member().id()))
                .queryParam("type", "STORY")
                .queryParam("priority", "HIGH")
                .queryParam("keyword", "filter")
                .header("Authorization", "Bearer " + fixture.owner().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.columns[0].tasks.length()").value(1))
            .andExpect(jsonPath("$.data.columns[0].tasks[0].id").value(expectedTaskId));
    }

    @Test
    void projectBoardSupportsUnassignedAssigneeFilter() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long columnId = columnIds(fixture.projectId()).get(0);
        long expectedTaskId = createTask(
            fixture.member().token(),
            fixture.projectId(),
            columnId,
            null,
            null,
            "Unassigned API",
            "TASK",
            "MEDIUM"
        );
        createTask(
            fixture.member().token(),
            fixture.projectId(),
            columnId,
            fixture.member().id(),
            null,
            "Assigned API",
            "TASK",
            "MEDIUM"
        );

        mockMvc.perform(get("/api/projects/{projectId}/board", fixture.projectId())
                .queryParam("assigneeId", "0")
                .header("Authorization", "Bearer " + fixture.owner().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.columns[0].tasks.length()").value(1))
            .andExpect(jsonPath("$.data.columns[0].tasks[0].id").value(expectedTaskId))
            .andExpect(jsonPath("$.data.columns[0].tasks[0].assignee").doesNotExist());
    }

    @Test
    void positionUpdateChangesColumnAndSortOrder() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        List<Long> columns = columnIds(fixture.projectId());
        long taskId = createTask(
            fixture.member().token(),
            fixture.projectId(),
            columns.get(0),
            fixture.member().id(),
            null,
            "Move me",
            "TASK",
            "MEDIUM"
        );

        mockMvc.perform(patch("/api/tasks/{taskId}/position", taskId)
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "columnId": %d,
                      "sortOrder": 7
                    }
                    """.formatted(columns.get(2))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.columnId").value(columns.get(2)))
            .andExpect(jsonPath("$.data.sortOrder").value(7));

        assertThat(jdbcTemplate.queryForObject(
            "SELECT CONCAT(column_id, ':', sort_order) FROM tasks WHERE id = ?",
            String.class,
            taskId
        )).isEqualTo(columns.get(2) + ":7");
    }

    @Test
    void failedPositionUpdateDoesNotChangeTaskState() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        Fixture other = fixtureWithOwnerAndMember("other-owner", "other-member", "Other project");
        long originalColumnId = columnIds(fixture.projectId()).get(0);
        long forbiddenColumnId = columnIds(other.projectId()).get(0);
        long taskId = createTask(
            fixture.member().token(),
            fixture.projectId(),
            originalColumnId,
            fixture.member().id(),
            null,
            "Stay put",
            "TASK",
            "MEDIUM"
        );

        mockMvc.perform(patch("/api/tasks/{taskId}/position", taskId)
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "columnId": %d,
                      "sortOrder": 99
                    }
                    """.formatted(forbiddenColumnId)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("BOARD_COLUMN_NOT_FOUND"));

        assertThat(jdbcTemplate.queryForObject(
            "SELECT CONCAT(column_id, ':', sort_order) FROM tasks WHERE id = ?",
            String.class,
            taskId
        )).isEqualTo(originalColumnId + ":0");
    }

    private Fixture fixtureWithOwnerAndMember() throws Exception {
        return fixtureWithOwnerAndMember("owner", "member", "Delivery");
    }

    private Fixture fixtureWithOwnerAndMember(String ownerAccount, String memberAccount, String projectName) throws Exception {
        RegisteredUser owner = register(ownerAccount, ownerAccount);
        RegisteredUser member = register(memberAccount, memberAccount);
        long projectId = createProject(owner.token(), projectName, projectName + " board");
        addMember(owner.token(), projectId, member.id());
        return new Fixture(owner, member, projectId);
    }

    private long createTask(
        String token,
        long projectId,
        long columnId,
        Long assigneeId,
        Long sprintId,
        String title,
        String taskType,
        String priority
    ) throws Exception {
        String sprintJson = sprintId == null ? "" : "\"sprintId\": %d,".formatted(sprintId);
        String assigneeJson = assigneeId == null ? "" : "\"assigneeId\": %d,".formatted(assigneeId);
        String response = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "%s",
                      "columnId": %d,
                      %s
                      %s
                      "taskType": "%s",
                      "priority": "%s"
                    }
                    """.formatted(title, columnId, assigneeJson, sprintJson, taskType, priority)))
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

    private long createSprint(String token, long projectId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/projects/{projectId}/sprints", projectId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s",
                      "startDate": "2026-05-21",
                      "endDate": "2026-06-04"
                    }
                    """.formatted(name)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
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
