package com.sdkanban.schema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SchemaMigrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesCoreKanbanTables() {
        List<String> tableNames = jdbcTemplate.queryForList("SHOW TABLES", String.class);

        assertThat(tableNames)
            .contains(
                "users",
                "projects",
                "project_members",
                "sprints",
                "board_columns",
                "tasks",
                "task_tags",
                "task_tag_links",
                "task_comments",
                "task_activities",
                "task_checklist_items",
                "notifications"
            );
    }

    @Test
    void coreTablesExposeDesignSpecColumns() {
        assertColumns("users", "account", "nickname", "email", "avatar_url", "password_hash", "status", "created_at", "updated_at");
        assertColumns("projects", "name", "description", "owner_id", "creator_id", "status", "created_at", "updated_at");
        assertColumns("board_columns", "project_id", "name", "color", "sort_order", "is_done", "created_at", "updated_at");
        assertColumns(
            "tasks",
            "project_id",
            "sprint_id",
            "column_id",
            "assignee_id",
            "creator_id",
            "title",
            "description",
            "task_type",
            "priority",
            "story_points",
            "estimated_hours",
            "due_date",
            "acceptance_criteria",
            "sort_order",
            "is_archived",
            "is_deleted",
            "created_at",
            "updated_at"
        );
        assertColumns(
            "task_activities",
            "task_id",
            "project_id",
            "actor_id",
            "action_type",
            "field_name",
            "old_value",
            "new_value",
            "created_at"
        );
        assertColumns(
            "task_checklist_items",
            "task_id",
            "project_id",
            "title",
            "is_done",
            "sort_order",
            "created_by",
            "completed_by",
            "completed_at",
            "created_at",
            "updated_at"
        );
        assertColumns(
            "notifications",
            "recipient_id",
            "actor_id",
            "project_id",
            "task_id",
            "type",
            "title",
            "content",
            "is_read",
            "created_at",
            "read_at"
        );
    }

    @Test
    void canonicalColumnsDoNotKeepLegacyRequiredBlockers() {
        assertNoColumns("users", "username", "display_name");
        assertNoColumns("task_activities", "activity_type");
    }

    @Test
    void schemaHasRequiredForeignKeysAndProjectScopedIndexes() {
        assertForeignKeys(Map.ofEntries(
            Map.entry("fk_projects_owner_id", "projects.owner_id->users.id"),
            Map.entry("fk_projects_creator_id", "projects.creator_id->users.id"),
            Map.entry("fk_project_members_project_id", "project_members.project_id->projects.id"),
            Map.entry("fk_project_members_user_id", "project_members.user_id->users.id"),
            Map.entry("fk_tasks_project_id", "tasks.project_id->projects.id"),
            Map.entry("fk_tasks_sprint_project", "tasks.sprint_id,project_id->sprints.id,project_id"),
            Map.entry("fk_tasks_column_project", "tasks.column_id,project_id->board_columns.id,project_id"),
            Map.entry("fk_tasks_assignee_id", "tasks.assignee_id->users.id"),
            Map.entry("fk_tasks_creator_id", "tasks.creator_id->users.id"),
            Map.entry("fk_task_tag_links_task_project", "task_tag_links.task_id,project_id->tasks.id,project_id"),
            Map.entry("fk_task_tag_links_tag_project", "task_tag_links.tag_id,project_id->task_tags.id,project_id"),
            Map.entry("fk_task_activities_task_project", "task_activities.task_id,project_id->tasks.id,project_id"),
            Map.entry("fk_task_activities_actor_id", "task_activities.actor_id->users.id"),
            Map.entry("fk_task_checklist_items_task_project", "task_checklist_items.task_id,project_id->tasks.id,project_id"),
            Map.entry("fk_task_checklist_items_created_by", "task_checklist_items.created_by->users.id"),
            Map.entry("fk_task_checklist_items_completed_by", "task_checklist_items.completed_by->users.id"),
            Map.entry("fk_notifications_recipient_id", "notifications.recipient_id->users.id"),
            Map.entry("fk_notifications_actor_id", "notifications.actor_id->users.id"),
            Map.entry("fk_notifications_project_id", "notifications.project_id->projects.id"),
            Map.entry("fk_notifications_task_project", "notifications.task_id,project_id->tasks.id,project_id")
        ));

        assertIndexes(Map.of(
            "sprints", List.of("uk_sprints_id_project"),
            "board_columns", List.of("uk_board_columns_id_project", "uk_board_columns_project_sort"),
            "tasks", List.of("uk_tasks_id_project"),
            "task_tags", List.of("uk_task_tags_id_project", "uk_task_tags_project_name"),
            "task_checklist_items", List.of("idx_task_checklist_items_task_project"),
            "notifications", List.of("idx_notifications_recipient_read_created")
        ));
    }

    @Test
    @Transactional
    void projectScopedReferencesRejectCrossProjectColumnsAndTags() {
        seedCanonicalProject(1001, 2001, "owner1", "Project 1");
        seedCanonicalProject(1002, 2002, "owner2", "Project 2");
        jdbcTemplate.update("INSERT INTO board_columns (id, project_id, name, color, sort_order, is_done) VALUES (3001, 2001, 'Todo', '#4f46e5', 1, false)");
        jdbcTemplate.update("INSERT INTO board_columns (id, project_id, name, color, sort_order, is_done) VALUES (3002, 2002, 'Todo', '#0891b2', 1, false)");
        jdbcTemplate.update("INSERT INTO task_tags (id, project_id, name, color) VALUES (4001, 2001, 'Backend', '#16a34a')");
        jdbcTemplate.update("INSERT INTO task_tags (id, project_id, name, color) VALUES (4002, 2002, 'Backend', '#dc2626')");
        jdbcTemplate.update("INSERT INTO tasks (id, project_id, column_id, creator_id, title) VALUES (5001, 2001, 3001, 1001, 'Scoped task')");

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO tasks (id, project_id, column_id, creator_id, title) VALUES (5002, 2001, 3002, 1001, 'Wrong column')"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO task_tag_links (task_id, tag_id, project_id) VALUES (5001, 4002, 2001)"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void canonicalUserAndActivityInsertsDoNotNeedLegacyColumns() {
        seedCanonicalProject(1101, 2101, "canonical-owner", "Canonical Project");
        jdbcTemplate.update("INSERT INTO board_columns (id, project_id, name, color, sort_order, is_done) VALUES (3101, 2101, 'Todo', '#2563eb', 1, false)");
        jdbcTemplate.update("INSERT INTO tasks (id, project_id, column_id, creator_id, title) VALUES (5101, 2101, 3101, 1101, 'Canonical task')");

        int inserted = jdbcTemplate.update(
            """
                INSERT INTO task_activities (
                    task_id, project_id, actor_id, action_type, field_name, old_value, new_value
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
            5101,
            2101,
            1101,
            "TASK_CREATED",
            "status",
            null,
            "OPEN"
        );

        assertThat(inserted).isEqualTo(1);
    }

    @Test
    @Transactional
    void sprintScopeRejectsCrossProjectAssignmentAndRestrictsReferencedSprintDeletion() {
        seedCanonicalProject(1201, 2201, "sprint-owner-1", "Sprint Project 1");
        seedCanonicalProject(1202, 2202, "sprint-owner-2", "Sprint Project 2");
        jdbcTemplate.update("INSERT INTO board_columns (id, project_id, name, color, sort_order, is_done) VALUES (3201, 2201, 'Todo', '#7c3aed', 1, false)");
        jdbcTemplate.update("INSERT INTO sprints (id, project_id, name, status) VALUES (6201, 2201, 'Sprint 1', 'PLANNED')");
        jdbcTemplate.update("INSERT INTO sprints (id, project_id, name, status) VALUES (6202, 2202, 'Sprint 2', 'PLANNED')");
        jdbcTemplate.update("INSERT INTO tasks (id, project_id, sprint_id, column_id, creator_id, title) VALUES (5201, 2201, 6201, 3201, 1201, 'Sprint task')");

        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO tasks (id, project_id, sprint_id, column_id, creator_id, title) VALUES (5202, 2201, 6202, 3201, 1201, 'Wrong sprint')"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM sprints WHERE id = 6201"))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void assertColumns(String tableName, String... expectedColumns) {
        List<String> actualColumns = jdbcTemplate.queryForList(
            """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """,
            String.class,
            tableName
        );

        assertThat(actualColumns)
            .as("columns for %s", tableName)
            .contains(expectedColumns);
    }

    private void assertNoColumns(String tableName, String... unexpectedColumns) {
        List<String> actualColumns = jdbcTemplate.queryForList(
            """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """,
            String.class,
            tableName
        );

        assertThat(actualColumns)
            .as("columns for %s", tableName)
            .doesNotContain(unexpectedColumns);
    }

    private void seedCanonicalProject(long userId, long projectId, String account, String projectName) {
        jdbcTemplate.update(
            "INSERT INTO users (id, account, nickname, password_hash, status) VALUES (?, ?, ?, ?, ?)",
            userId,
            account,
            account,
            "hash",
            "ACTIVE"
        );
        jdbcTemplate.update(
            "INSERT INTO projects (id, owner_id, creator_id, name, status) VALUES (?, ?, ?, ?, ?)",
            projectId,
            userId,
            userId,
            projectName,
            "ACTIVE"
        );
    }

    private void assertForeignKeys(Map<String, String> expectedForeignKeys) {
        Map<String, String> actualForeignKeys = new LinkedHashMap<>();
        jdbcTemplate.query(
            """
                SELECT
                    constraint_name,
                    table_name,
                    GROUP_CONCAT(column_name ORDER BY ordinal_position SEPARATOR ',') AS columns,
                    referenced_table_name,
                    GROUP_CONCAT(referenced_column_name ORDER BY ordinal_position SEPARATOR ',') AS referenced_columns
                FROM information_schema.key_column_usage
                WHERE table_schema = DATABASE()
                  AND referenced_table_name IS NOT NULL
                GROUP BY constraint_name, table_name, referenced_table_name
                """,
            rs -> {
                actualForeignKeys.put(
                    rs.getString("constraint_name"),
                    rs.getString("table_name") + "."
                        + rs.getString("columns") + "->"
                        + rs.getString("referenced_table_name") + "."
                        + rs.getString("referenced_columns")
                );
            }
        );

        assertThat(actualForeignKeys)
            .as("foreign keys")
            .containsAllEntriesOf(expectedForeignKeys);
    }

    private void assertIndexes(Map<String, List<String>> expectedIndexesByTable) {
        expectedIndexesByTable.forEach((tableName, expectedIndexes) -> {
            List<String> actualIndexes = jdbcTemplate.queryForList(
                """
                    SELECT DISTINCT index_name
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = ?
                    """,
                String.class,
                tableName
            );

            assertThat(actualIndexes)
                .as("indexes for %s", tableName)
                .containsAll(expectedIndexes);
        });
    }
}
