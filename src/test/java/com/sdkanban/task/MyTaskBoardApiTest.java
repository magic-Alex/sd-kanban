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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MyTaskBoardApiTest {
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
        jdbcTemplate.update("DELETE FROM task_checklist_items");
        jdbcTemplate.update("DELETE FROM task_tag_links");
        jdbcTemplate.update("DELETE FROM tasks");
        jdbcTemplate.update("DELETE FROM task_tags");
        jdbcTemplate.update("DELETE FROM board_columns");
        jdbcTemplate.update("DELETE FROM sprints");
        jdbcTemplate.update("DELETE FROM project_members");
        jdbcTemplate.update("DELETE FROM projects");
        jdbcTemplate.update("DELETE FROM users");
        resetDefaultBoardTemplates();
    }

    @Test
    void myTaskBoardReturnsTemplateGroupsAndOnlyTasksAssignedToCurrentUser() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        List<Long> columns = columnIds(fixture.projectId());
        long mine = createTask(fixture.member().token(), fixture.projectId(), columns.get(0), fixture.member().id(), "Mine");
        createTask(fixture.member().token(), fixture.projectId(), columns.get(0), fixture.owner().id(), "Someone else's");

        mockMvc.perform(get("/api/tasks/mine/board")
                .queryParam("groupBy", "project")
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.groupBy").value("template"))
            .andExpect(jsonPath("$.data.groups.length()").value(5))
            .andExpect(jsonPath("$.data.groups[0].templateKey").value("BACKLOG"))
            .andExpect(jsonPath("$.data.groups[1].templateKey").value("READY"))
            .andExpect(jsonPath("$.data.groups[2].templateKey").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.data.groups[3].templateKey").value("TESTING"))
            .andExpect(jsonPath("$.data.groups[4].templateKey").value("DONE"))
            .andExpect(jsonPath("$.data.groups[0].name").value("\u5f85\u529e\uff08Backlog\uff09"))
            .andExpect(jsonPath("$.data.groups[0].tasks.length()").value(1))
            .andExpect(jsonPath("$.data.groups[0].tasks[0].id").value(mine))
            .andExpect(jsonPath("$.data.groups[0].tasks[0].projectCode").value(fixture.projectCode()))
            .andExpect(jsonPath("$.data.groups[0].tasks[0].projectName").value("Delivery"))
            .andExpect(jsonPath("$.data.groups[0].tasks[0].projectColor").value(fixture.projectColor()))
            .andExpect(jsonPath("$.data.groups[0].tasks[0].columnTemplateKey").value("BACKLOG"))
            .andExpect(jsonPath("$.data.groups[1].tasks.length()").value(0))
            .andExpect(jsonPath("$.data.groups[2].tasks.length()").value(0))
            .andExpect(jsonPath("$.data.groups[3].tasks.length()").value(0))
            .andExpect(jsonPath("$.data.groups[4].tasks.length()").value(0));
    }

    @Test
    void myTaskBoardGroupsTasksByGlobalTemplateState() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        List<Long> columns = columnIds(fixture.projectId());
        createTask(fixture.member().token(), fixture.projectId(), columns.get(0), fixture.member().id(), "Backlog mine");
        createTask(fixture.member().token(), fixture.projectId(), columns.get(2), fixture.member().id(), "Progress mine");

        mockMvc.perform(get("/api/tasks/mine/board")
                .queryParam("groupBy", "column")
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.groupBy").value("template"))
            .andExpect(jsonPath("$.data.groups.length()").value(5))
            .andExpect(jsonPath("$.data.groups[?(@.templateKey == 'BACKLOG')].tasks[0].title").value("Backlog mine"))
            .andExpect(jsonPath("$.data.groups[?(@.templateKey == 'IN_PROGRESS')].tasks[0].title").value("Progress mine"));
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
            .andExpect(jsonPath("$.data.groupBy").value("template"))
            .andExpect(jsonPath("$.data.groups.length()").value(5))
            .andExpect(jsonPath("$.data.groups[0].tasks.length()").value(0));
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
            .andExpect(jsonPath("$.data.groupBy").value("template"))
            .andExpect(jsonPath("$.data.groups.length()").value(5))
            .andExpect(jsonPath("$.data.groups[0].tasks.length()").value(0));
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
            .andExpect(jsonPath("$.data.groupBy").value("template"))
            .andExpect(jsonPath("$.data.groups[0].tasks[0].checklistDoneCount").value(1))
            .andExpect(jsonPath("$.data.groups[0].tasks[0].checklistTotalCount").value(2));
    }

    @Test
    void myTaskBoardKeepsTasksInCustomTemplateColumnsVisible() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long customColumnId = createCustomColumn(
            fixture.projectId(),
            "CUSTOM_LEGACY",
            "Legacy Queue",
            "#334155",
            99,
            false
        );
        long taskId = createTask(
            fixture.member().token(),
            fixture.projectId(),
            customColumnId,
            fixture.member().id(),
            "Legacy mine"
        );

        mockMvc.perform(get("/api/tasks/mine/board")
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groupBy").value("template"))
            .andExpect(jsonPath("$.data.groups.length()").value(6))
            .andExpect(jsonPath("$.data.groups[0].templateKey").value("BACKLOG"))
            .andExpect(jsonPath("$.data.groups[4].templateKey").value("DONE"))
            .andExpect(jsonPath("$.data.groups[5].templateKey").value("CUSTOM_LEGACY"))
            .andExpect(jsonPath("$.data.groups[5].name").value("Legacy Queue"))
            .andExpect(jsonPath("$.data.groups[5].color").value("#334155"))
            .andExpect(jsonPath("$.data.groups[5].sortOrder").value(99))
            .andExpect(jsonPath("$.data.groups[5].isDone").value(false))
            .andExpect(jsonPath("$.data.groups[5].tasks[0].id").value(taskId))
            .andExpect(jsonPath("$.data.groups[5].tasks[0].title").value("Legacy mine"))
            .andExpect(jsonPath("$.data.groups[5].tasks[0].projectCode").value(fixture.projectCode()))
            .andExpect(jsonPath("$.data.groups[5].tasks[0].projectName").value("Delivery"))
            .andExpect(jsonPath("$.data.groups[5].tasks[0].projectColor").value(fixture.projectColor()))
            .andExpect(jsonPath("$.data.groups[5].tasks[0].columnTemplateKey").value("CUSTOM_LEGACY"));
    }

    @Test
    void myTaskBoardIncludesNewGlobalTemplateWithoutTasks() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        upsertTemplate("REVIEW", "\u8bc4\u5ba1", "Review", "#14b8a6", 5, null, false);

        mockMvc.perform(get("/api/tasks/mine/board")
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.groupBy").value("template"))
            .andExpect(jsonPath("$.data.groups.length()").value(6))
            .andExpect(jsonPath("$.data.groups[0].templateKey").value("BACKLOG"))
            .andExpect(jsonPath("$.data.groups[4].templateKey").value("DONE"))
            .andExpect(jsonPath("$.data.groups[5].templateKey").value("REVIEW"))
            .andExpect(jsonPath("$.data.groups[5].name").value("\u8bc4\u5ba1\uff08Review\uff09"))
            .andExpect(jsonPath("$.data.groups[5].color").value("#14b8a6"))
            .andExpect(jsonPath("$.data.groups[5].sortOrder").value(5))
            .andExpect(jsonPath("$.data.groups[5].isDone").value(false))
            .andExpect(jsonPath("$.data.groups[5].tasks.length()").value(0));
    }

    private Fixture fixtureWithOwnerAndMember() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        CreatedProject project = createProject(owner.token(), "Delivery", "Delivery board");
        addMember(owner.token(), project.id(), member.id());
        return new Fixture(owner, member, project.id(), project.projectCode(), project.projectColor());
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

    private long createCustomColumn(
        long projectId,
        String templateKey,
        String name,
        String color,
        int sortOrder,
        boolean done
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO board_columns (project_id, template_key, name, color, sort_order, is_done)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            projectId,
            templateKey,
            name,
            color,
            sortOrder,
            done
        );
        return jdbcTemplate.queryForObject(
            "SELECT id FROM board_columns WHERE project_id = ? AND template_key = ? ORDER BY id DESC LIMIT 1",
            Long.class,
            projectId,
            templateKey
        );
    }

    private CreatedProject createProject(String token, String name, String description) throws Exception {
        int sequence = PROJECT_SEQUENCE.incrementAndGet();
        String projectCode = "MY-" + sequence;
        String projectColor = "#0f766e";
        String response = mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s",
                      "description": "%s",
                      "projectCode": "%s",
                      "projectColor": "%s"
                    }
                    """.formatted(name, description, projectCode, projectColor)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return new CreatedProject(
            objectMapper.readTree(response).path("data").path("id").asLong(),
            projectCode,
            projectColor
        );
    }

    private void resetDefaultBoardTemplates() {
        jdbcTemplate.update("UPDATE board_column_templates SET sort_order = sort_order + 1000");
        upsertTemplate("BACKLOG", "\u5f85\u529e", "Backlog", "#64748b", 0, null, false);
        upsertTemplate("READY", "\u5c31\u7eea", "Ready", "#0ea5e9", 1, null, false);
        upsertTemplate("IN_PROGRESS", "\u8fdb\u884c\u4e2d", "In Progress", "#f59e0b", 2, null, false);
        upsertTemplate("TESTING", "\u6d4b\u8bd5", "Testing", "#8b5cf6", 3, null, false);
        upsertTemplate("DONE", "\u5b8c\u6210", "Done", "#22c55e", 4, null, true);
        jdbcTemplate.update("DELETE FROM board_column_templates WHERE template_key NOT IN ('BACKLOG', 'READY', 'IN_PROGRESS', 'TESTING', 'DONE')");
    }

    private void upsertTemplate(
        String templateKey,
        String nameZh,
        String nameEn,
        String color,
        int sortOrder,
        Integer wipLimit,
        boolean done
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO board_column_templates (template_key, name_zh, name_en, color, sort_order, wip_limit, is_done)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              name_zh = VALUES(name_zh),
              name_en = VALUES(name_en),
              color = VALUES(color),
              sort_order = VALUES(sort_order),
              wip_limit = VALUES(wip_limit),
              is_done = VALUES(is_done)
            """,
            templateKey,
            nameZh,
            nameEn,
            color,
            sortOrder,
            wipLimit,
            done
        );
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

    private record CreatedProject(long id, String projectCode, String projectColor) {
    }

    private record Fixture(RegisteredUser owner, RegisteredUser member, long projectId, String projectCode, String projectColor) {
    }
}
