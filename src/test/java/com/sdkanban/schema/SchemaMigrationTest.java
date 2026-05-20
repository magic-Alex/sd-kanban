package com.sdkanban.schema;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
                "task_activities"
            );
    }
}
