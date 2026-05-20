package com.sdkanban.dashboard;

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

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {
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
    void summaryReturnsCurrentUsersTaskAndProjectCountsWithRecentActivity() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long pendingColumnId = columnIds(fixture.projectId()).get(0);
        long doneColumnId = doneColumnId(fixture.projectId());
        long overdueTaskId = createTask(
            fixture.member().token(),
            fixture.projectId(),
            pendingColumnId,
            fixture.member().id(),
            null,
            "Overdue API",
            "TASK",
            "HIGH",
            LocalDate.now().minusDays(1)
        );
        createTask(
            fixture.member().token(),
            fixture.projectId(),
            doneColumnId,
            fixture.member().id(),
            null,
            "Already done",
            "TASK",
            "LOW",
            null
        );
        createTask(
            fixture.owner().token(),
            fixture.ownedOnlyProjectId(),
            columnIds(fixture.ownedOnlyProjectId()).get(0),
            fixture.owner().id(),
            null,
            "Owner only work",
            "TASK",
            "MEDIUM",
            null
        );

        mockMvc.perform(patch("/api/tasks/{taskId}", overdueTaskId)
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "priority": "LOW"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/summary")
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.pendingTaskCount").value(1))
            .andExpect(jsonPath("$.data.overdueTaskCount").value(1))
            .andExpect(jsonPath("$.data.ownedProjectCount").value(0))
            .andExpect(jsonPath("$.data.joinedProjectCount").value(1))
            .andExpect(jsonPath("$.data.recentActivities[?(@.actionType == 'TASK_UPDATED')]").exists())
            .andExpect(jsonPath("$.data.recentActivities[?(@.taskTitle == 'Overdue API')]").exists());
    }

    @Test
    void trendsReturnCompletionBucketsForCurrentUsersVisibleProjects() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long doneColumnId = doneColumnId(fixture.projectId());
        createTask(
            fixture.member().token(),
            fixture.projectId(),
            doneColumnId,
            fixture.member().id(),
            null,
            "Done today",
            "TASK",
            "MEDIUM",
            null
        );

        mockMvc.perform(get("/api/dashboard/trends")
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.buckets.length()").value(7))
            .andExpect(jsonPath("$.data.buckets[?(@.date == '%s')].completedCount".formatted(LocalDate.now())).value(1));
    }

    @Test
    void projectStatsReturnsSprintTypeAndMemberWorkload() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long sprintId = createSprint(fixture.owner().token(), fixture.projectId(), "Sprint 1");
        List<Long> columns = columnIds(fixture.projectId());
        long doneColumnId = doneColumnId(fixture.projectId());
        createTask(
            fixture.member().token(),
            fixture.projectId(),
            doneColumnId,
            fixture.member().id(),
            sprintId,
            "Story done",
            "STORY",
            "HIGH",
            null
        );
        createTask(
            fixture.owner().token(),
            fixture.projectId(),
            columns.get(0),
            fixture.owner().id(),
            sprintId,
            "Bug pending",
            "BUG",
            "MEDIUM",
            null
        );

        mockMvc.perform(get("/api/projects/{projectId}/stats", fixture.projectId())
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.projectId").value(fixture.projectId()))
            .andExpect(jsonPath("$.data.totalTaskCount").value(2))
            .andExpect(jsonPath("$.data.doneTaskCount").value(1))
            .andExpect(jsonPath("$.data.openTaskCount").value(1))
            .andExpect(jsonPath("$.data.sprintProgress[0].sprintId").value(sprintId))
            .andExpect(jsonPath("$.data.sprintProgress[0].totalTaskCount").value(2))
            .andExpect(jsonPath("$.data.sprintProgress[0].doneTaskCount").value(1))
            .andExpect(jsonPath("$.data.taskTypeDistribution[?(@.type == 'STORY')].count").value(1))
            .andExpect(jsonPath("$.data.taskTypeDistribution[?(@.type == 'BUG')].count").value(1))
            .andExpect(jsonPath("$.data.memberWorkload[?(@.user.id == %d)].openTaskCount".formatted(fixture.owner().id())).value(1))
            .andExpect(jsonPath("$.data.memberWorkload[?(@.user.id == %d)].doneTaskCount".formatted(fixture.member().id())).value(1));
    }

    @Test
    void sprintStatsReturnsProgressForOneSprint() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long sprintId = createSprint(fixture.owner().token(), fixture.projectId(), "Sprint 1");
        long pendingColumnId = columnIds(fixture.projectId()).get(0);
        long doneColumnId = doneColumnId(fixture.projectId());
        createTask(
            fixture.member().token(),
            fixture.projectId(),
            doneColumnId,
            fixture.member().id(),
            sprintId,
            "Done in sprint",
            "STORY",
            "HIGH",
            null
        );
        createTask(
            fixture.owner().token(),
            fixture.projectId(),
            pendingColumnId,
            fixture.owner().id(),
            sprintId,
            "Open in sprint",
            "TASK",
            "MEDIUM",
            null
        );

        mockMvc.perform(get("/api/projects/{projectId}/sprints/{sprintId}/stats", fixture.projectId(), sprintId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.sprintId").value(sprintId))
            .andExpect(jsonPath("$.data.totalTaskCount").value(2))
            .andExpect(jsonPath("$.data.doneTaskCount").value(1))
            .andExpect(jsonPath("$.data.openTaskCount").value(1));
    }

    @Test
    void nonMemberCannotReadProjectStats() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        RegisteredUser outsider = register("outsider", "Outsider");

        mockMvc.perform(get("/api/projects/{projectId}/stats", fixture.projectId())
                .header("Authorization", "Bearer " + outsider.token()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT_MEMBER_REQUIRED"));
    }

    private Fixture fixtureWithOwnerAndMember() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");
        addMember(owner.token(), projectId, member.id());
        long ownedOnlyProjectId = createProject(owner.token(), "Owner Only", "Only owner can see it");
        return new Fixture(owner, member, projectId, ownedOnlyProjectId);
    }

    private long createTask(
        String token,
        long projectId,
        long columnId,
        long assigneeId,
        Long sprintId,
        String title,
        String taskType,
        String priority,
        LocalDate dueDate
    ) throws Exception {
        String sprintJson = sprintId == null ? "" : "\"sprintId\": %d,".formatted(sprintId);
        String dueDateJson = dueDate == null ? "" : "\"dueDate\": \"%s\",".formatted(dueDate);
        String response = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "%s",
                      "columnId": %d,
                      "assigneeId": %d,
                      %s
                      %s
                      "taskType": "%s",
                      "priority": "%s"
                    }
                    """.formatted(title, columnId, assigneeId, sprintJson, dueDateJson, taskType, priority)))
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

    private long doneColumnId(long projectId) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM board_columns WHERE project_id = ? AND is_done = TRUE ORDER BY sort_order LIMIT 1",
            Long.class,
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

    private record Fixture(RegisteredUser owner, RegisteredUser member, long projectId, long ownedOnlyProjectId) {
    }
}
