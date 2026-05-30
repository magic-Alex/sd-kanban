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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {
    private static final AtomicInteger PROJECT_SEQUENCE = new AtomicInteger();

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
    void projectMemberCanCreateTaskWithPlanningFields() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long sprintId = createSprint(fixture.member().token(), fixture.projectId(), "Sprint 1");
        long columnId = firstColumnId(fixture.projectId());
        long tagId = createTag(fixture.owner().token(), fixture.projectId(), "Backend", "#16a34a");

        String response = mockMvc.perform(post("/api/projects/{projectId}/tasks", fixture.projectId())
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Implement task API",
                      "description": "Create the collaboration endpoints",
                      "taskType": "STORY",
                      "priority": "HIGH",
                      "storyPoints": 5,
                      "estimatedHours": 12.5,
                      "acceptanceCriteria": "API persists all planning fields",
                      "assigneeId": %d,
                      "sprintId": %d,
                      "columnId": %d,
                      "tagIds": [%d]
                    }
                    """.formatted(fixture.member().id(), sprintId, columnId, tagId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.title").value("Implement task API"))
            .andExpect(jsonPath("$.data.assignee.id").value(fixture.member().id()))
            .andExpect(jsonPath("$.data.sprintId").value(sprintId))
            .andExpect(jsonPath("$.data.columnId").value(columnId))
            .andExpect(jsonPath("$.data.tags[0].name").value("Backend"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long taskId = objectMapper.readTree(response).path("data").path("id").asLong();
        assertThat(jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM tasks
            WHERE id = ? AND project_id = ? AND creator_id = ? AND assignee_id = ?
              AND sprint_id = ? AND column_id = ? AND task_type = 'STORY'
              AND priority = 'HIGH' AND story_points = 5 AND estimated_hours = 12.5
              AND acceptance_criteria = 'API persists all planning fields'
            """,
            Integer.class,
            taskId,
            fixture.projectId(),
            fixture.member().id(),
            fixture.member().id(),
            sprintId,
            columnId
        )).isEqualTo(1);
        assertThat(tagNamesForTask(taskId)).containsExactly("Backend");
    }

    @Test
    void taskAssigneeMustBeProjectMember() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        RegisteredUser outsider = register("outsider", "Outsider");
        long columnId = firstColumnId(fixture.projectId());

        mockMvc.perform(post("/api/projects/{projectId}/tasks", fixture.projectId())
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Bad assignment",
                      "columnId": %d,
                      "assigneeId": %d
                    }
                    """.formatted(columnId, outsider.id())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("TASK_ASSIGNEE_NOT_MEMBER"));
    }

    @Test
    void tagsAreProjectScopedAndCanBeLinkedToTasks() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long columnId = firstColumnId(fixture.projectId());
        long tagId = createTag(fixture.owner().token(), fixture.projectId(), "UI", "#2563eb");
        long taskId = createTask(fixture.member().token(), fixture.projectId(), columnId, "Wire board UI");

        mockMvc.perform(patch("/api/tasks/{taskId}/tags", taskId)
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tagIds": [%d]
                    }
                    """.formatted(tagId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.tags[0].name").value("UI"));

        assertThat(tagNamesForTask(taskId)).containsExactly("UI");
    }

    @Test
    void projectMemberCanClearNullableTaskFields() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long sprintId = createSprint(fixture.member().token(), fixture.projectId(), "Sprint 1");
        long columnId = firstColumnId(fixture.projectId());

        String response = mockMvc.perform(post("/api/projects/{projectId}/tasks", fixture.projectId())
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Editable task",
                      "description": "Clear me",
                      "columnId": %d,
                      "assigneeId": %d,
                      "sprintId": %d,
                      "storyPoints": 8,
                      "estimatedHours": 13.5,
                      "dueDate": "2026-06-08",
                      "acceptanceCriteria": "Clear acceptance"
                    }
                    """.formatted(columnId, fixture.member().id(), sprintId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        long taskId = objectMapper.readTree(response).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "clearFields": [
                        "description",
                        "assigneeId",
                        "sprintId",
                        "storyPoints",
                        "estimatedHours",
                        "dueDate",
                        "acceptanceCriteria"
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.description").isEmpty())
            .andExpect(jsonPath("$.data.assignee").isEmpty())
            .andExpect(jsonPath("$.data.sprintId").isEmpty())
            .andExpect(jsonPath("$.data.storyPoints").isEmpty())
            .andExpect(jsonPath("$.data.estimatedHours").isEmpty())
            .andExpect(jsonPath("$.data.dueDate").isEmpty())
            .andExpect(jsonPath("$.data.acceptanceCriteria").isEmpty());

        assertThat(jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM tasks
            WHERE id = ?
              AND description IS NULL
              AND assignee_id IS NULL
              AND sprint_id IS NULL
              AND story_points IS NULL
              AND estimated_hours IS NULL
              AND due_date IS NULL
              AND acceptance_criteria IS NULL
            """,
            Integer.class,
            taskId
        )).isEqualTo(1);
    }

    @Test
    void unknownClearFieldIsRejected() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Invalid clear");

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "clearFields": ["title"]
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("TASK_CLEAR_FIELD_NOT_ALLOWED"));
    }

    @Test
    void projectMemberCanArchiveTaskAndHideItFromBoard() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long columnId = firstColumnId(fixture.projectId());
        long taskId = createTask(fixture.member().token(), fixture.projectId(), columnId, "Archive me");

        mockMvc.perform(patch("/api/tasks/{taskId}/archive", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(taskId));

        mockMvc.perform(get("/api/projects/{projectId}/board", fixture.projectId())
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.columns[0].tasks.length()").value(0));

        assertThat(jdbcTemplate.queryForObject(
            "SELECT is_archived FROM tasks WHERE id = ?",
            Boolean.class,
            taskId
        )).isTrue();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM task_activities WHERE task_id = ? AND action_type = 'TASK_ARCHIVED'",
            Integer.class,
            taskId
        )).isEqualTo(1);
    }

    @Test
    void projectOwnerCanArchiveTaskCreatedByMember() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Owner archive");

        mockMvc.perform(patch("/api/tasks/{taskId}/archive", taskId)
                .header("Authorization", "Bearer " + fixture.owner().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        assertThat(jdbcTemplate.queryForObject(
            "SELECT is_archived FROM tasks WHERE id = ?",
            Boolean.class,
            taskId
        )).isTrue();
    }

    @Test
    void projectMemberCanSearchArchivedTasks() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long columnId = firstColumnId(fixture.projectId());
        long archivedTaskId = createTask(fixture.member().token(), fixture.projectId(), columnId, "Archived onboarding", fixture.member().id());
        createTask(fixture.member().token(), fixture.projectId(), columnId, "Visible task", fixture.member().id());

        mockMvc.perform(patch("/api/tasks/{taskId}/archive", archivedTaskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/{projectId}/tasks/archived?keyword=onboarding&assigneeId={assigneeId}&type=TASK&priority=MEDIUM", fixture.projectId(), fixture.member().id())
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(archivedTaskId))
            .andExpect(jsonPath("$.data[0].title").value("Archived onboarding"));
    }

    @Test
    void permittedUserCanRestoreArchivedTaskToBoard() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long columnId = firstColumnId(fixture.projectId());
        long taskId = createTask(fixture.member().token(), fixture.projectId(), columnId, "Restore me", fixture.member().id());

        mockMvc.perform(patch("/api/tasks/{taskId}/archive", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/tasks/{taskId}/restore", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(taskId));

        mockMvc.perform(get("/api/projects/{projectId}/board", fixture.projectId())
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.columns[0].tasks[0].id").value(taskId));

        assertThat(jdbcTemplate.queryForObject("SELECT is_archived FROM tasks WHERE id = ?", Boolean.class, taskId)).isFalse();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM task_activities WHERE task_id = ? AND action_type = 'TASK_RESTORED'",
            Integer.class,
            taskId
        )).isEqualTo(1);
    }

    @Test
    void restoringActiveTaskDoesNotMoveOrRecordActivity() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long columnId = firstColumnId(fixture.projectId());
        long firstTaskId = createTask(fixture.member().token(), fixture.projectId(), columnId, "Already active", fixture.member().id());
        createTask(fixture.member().token(), fixture.projectId(), columnId, "Second active", fixture.member().id());

        Integer originalSortOrder = jdbcTemplate.queryForObject(
            "SELECT sort_order FROM tasks WHERE id = ?",
            Integer.class,
            firstTaskId
        );

        mockMvc.perform(patch("/api/tasks/{taskId}/restore", firstTaskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(firstTaskId));

        assertThat(jdbcTemplate.queryForObject("SELECT is_archived FROM tasks WHERE id = ?", Boolean.class, firstTaskId)).isFalse();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT sort_order FROM tasks WHERE id = ?",
            Integer.class,
            firstTaskId
        )).isEqualTo(originalSortOrder);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM task_activities WHERE task_id = ? AND action_type = 'TASK_RESTORED'",
            Integer.class,
            firstTaskId
        )).isZero();
    }

    @Test
    void ordinaryProjectMemberCannotRestoreUnrelatedTask() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.owner().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Owner task", fixture.owner().id());

        mockMvc.perform(patch("/api/tasks/{taskId}/archive", taskId)
                .header("Authorization", "Bearer " + fixture.owner().token()))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/tasks/{taskId}/restore", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("TASK_ACTION_FORBIDDEN"));
    }

    @Test
    void projectMemberCanSoftDeleteTaskAndDetailReturnsNotFound() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Delete me");

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));

        assertThat(jdbcTemplate.queryForObject(
            "SELECT is_deleted FROM tasks WHERE id = ?",
            Boolean.class,
            taskId
        )).isTrue();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM task_activities WHERE task_id = ? AND action_type = 'TASK_DELETED'",
            Integer.class,
            taskId
        )).isEqualTo(1);
    }

    @Test
    void taskAssigneeCanSoftDeleteTaskCreatedByOwner() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(
            fixture.owner().token(),
            fixture.projectId(),
            firstColumnId(fixture.projectId()),
            "Assigned delete",
            fixture.member().id()
        );

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        assertThat(jdbcTemplate.queryForObject(
            "SELECT is_deleted FROM tasks WHERE id = ?",
            Boolean.class,
            taskId
        )).isTrue();
    }

    @Test
    void ordinaryProjectMemberCannotArchiveOrDeleteUnrelatedTask() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(
            fixture.owner().token(),
            fixture.projectId(),
            firstColumnId(fixture.projectId()),
            "Owner only task",
            fixture.owner().id()
        );

        mockMvc.perform(patch("/api/tasks/{taskId}/archive", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("TASK_ACTION_FORBIDDEN"));

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("TASK_ACTION_FORBIDDEN"));
    }

    @Test
    void projectMemberCanSoftDeleteTaskAndHideItFromBoard() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long columnId = firstColumnId(fixture.projectId());
        long taskId = createTask(fixture.member().token(), fixture.projectId(), columnId, "Delete from board");

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/projects/{projectId}/board", fixture.projectId())
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.columns[0].tasks.length()").value(0));
    }

    @Test
    void nullClearFieldIsRejected() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Null clear");

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "clearFields": [null]
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("TASK_CLEAR_FIELD_NOT_ALLOWED"));
    }

    @Test
    void negativePlanningNumbersAreRejected() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Invalid planning");

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "storyPoints": -1,
                      "estimatedHours": -0.5
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.storyPoints").exists())
            .andExpect(jsonPath("$.fieldErrors.estimatedHours").exists());
    }

    @Test
    void nonMemberCannotReadOrUpdateTask() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        RegisteredUser outsider = register("outsider", "Outsider");
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Private task");

        mockMvc.perform(get("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + outsider.token()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT_MEMBER_REQUIRED"));

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + outsider.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "title": "Not allowed"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT_MEMBER_REQUIRED"));
    }

    @Test
    void nonMemberCannotArchiveOrDeleteTask() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        RegisteredUser outsider = register("outsider", "Outsider");
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Private task");

        mockMvc.perform(patch("/api/tasks/{taskId}/archive", taskId)
                .header("Authorization", "Bearer " + outsider.token()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT_MEMBER_REQUIRED"));

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + outsider.token()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT_MEMBER_REQUIRED"));
    }

    @Test
    void taskCommentsAndActivitiesCanBeListedWithDisplayText() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Activity task");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                .header("Authorization", "Bearer " + fixture.member().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Ready for review"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].content").value("Ready for review"));

        mockMvc.perform(get("/api/tasks/{taskId}/activities", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].displayText").value("Member \u8bc4\u8bba\u4e86\u4efb\u52a1"));
    }

    @Test
    void deletedTaskCommentsAndActivitiesReturnNotFound() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Deleted activity");

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks/{taskId}/comments", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));

        mockMvc.perform(get("/api/tasks/{taskId}/activities", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }

    @Test
    void commentMentionCreatesNotificationForProjectMember() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.owner().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Mention task");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                .header("Authorization", "Bearer " + fixture.owner().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "请 @Member 看一下这个任务"
                    }
                    """))
            .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND type = 'MENTION' AND task_id = ?",
            Integer.class,
            fixture.member().id(),
            taskId
        )).isEqualTo(1);
    }

    @Test
    void emailLikeTextDoesNotCreateMentionNotification() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.owner().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Email mention");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                .header("Authorization", "Bearer " + fixture.owner().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Please contact email@Member before review"
                    }
                    """))
            .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE type = 'MENTION' AND task_id = ?",
            Integer.class,
            taskId
        )).isZero();
    }

    @Test
    void duplicateSelfAndNonMemberMentionsCreateOnlyValidNotifications() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        RegisteredUser outsider = register("outsider", "Outsider");
        long taskId = createTask(fixture.owner().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Mention filter");

        mockMvc.perform(post("/api/tasks/{taskId}/comments", taskId)
                .header("Authorization", "Bearer " + fixture.owner().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "@Member please check this with @Member, @Owner, and @Outsider"
                    }
                    """))
            .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND type = 'MENTION' AND task_id = ?",
            Integer.class,
            fixture.member().id(),
            taskId
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE recipient_id IN (?, ?) AND type = 'MENTION' AND task_id = ?",
            Integer.class,
            fixture.owner().id(),
            outsider.id(),
            taskId
        )).isZero();
    }

    @Test
    void assignmentCreatesNotificationForNewAssignee() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(fixture.owner().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Assign task");

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId)
                .header("Authorization", "Bearer " + fixture.owner().token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "assigneeId": %d
                    }
                    """.formatted(fixture.member().id())))
            .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND type = 'TASK_ASSIGNED' AND task_id = ?",
            Integer.class,
            fixture.member().id(),
            taskId
        )).isEqualTo(1);
    }

    @Test
    void archiveAndRestoreNotifyTaskStakeholdersExceptActor() throws Exception {
        Fixture fixture = fixtureWithOwnerAndMember();
        long taskId = createTask(
            fixture.owner().token(),
            fixture.projectId(),
            firstColumnId(fixture.projectId()),
            "Archive notify",
            fixture.member().id()
        );

        mockMvc.perform(patch("/api/tasks/{taskId}/archive", taskId)
                .header("Authorization", "Bearer " + fixture.owner().token()))
            .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND type = 'TASK_ARCHIVED' AND task_id = ?",
            Integer.class,
            fixture.member().id(),
            taskId
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND type = 'TASK_ARCHIVED' AND task_id = ?",
            Integer.class,
            fixture.owner().id(),
            taskId
        )).isZero();

        mockMvc.perform(patch("/api/tasks/{taskId}/restore", taskId)
                .header("Authorization", "Bearer " + fixture.member().token()))
            .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND type = 'TASK_RESTORED' AND task_id = ?",
            Integer.class,
            fixture.owner().id(),
            taskId
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notifications WHERE recipient_id = ? AND type = 'TASK_RESTORED' AND task_id = ?",
            Integer.class,
            fixture.member().id(),
            taskId
        )).isZero();
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

    private long createTask(String token, long projectId, long columnId, String title, Long assigneeId) throws Exception {
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

    private long createTag(String token, long projectId, String name, String color) throws Exception {
        String response = mockMvc.perform(post("/api/projects/{projectId}/tags", projectId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s",
                      "color": "%s"
                    }
                    """.formatted(name, color)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private List<String> tagNamesForTask(long taskId) {
        return jdbcTemplate.query(
            """
            SELECT tag.name
            FROM task_tags tag
            JOIN task_tag_links link ON link.tag_id = tag.id AND link.project_id = tag.project_id
            WHERE link.task_id = ?
            ORDER BY tag.name
            """,
            (rs, rowNum) -> rs.getString("name"),
            taskId
        );
    }

    private long firstColumnId(long projectId) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM board_columns WHERE project_id = ? ORDER BY sort_order LIMIT 1",
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
        int sequence = PROJECT_SEQUENCE.incrementAndGet();
        String response = mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s",
                      "description": "%s",
                      "projectCode": "TASK-%d",
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
