package com.sdkanban.settings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class BoardTemplateControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetData() {
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
        jdbcTemplate.update("UPDATE board_column_templates SET sort_order = sort_order + 1000");
        upsertTemplate("BACKLOG", "待办", "Backlog", "#64748b", 0, null, false);
        upsertTemplate("READY", "就绪", "Ready", "#0ea5e9", 1, null, false);
        upsertTemplate("IN_PROGRESS", "进行中", "In Progress", "#f59e0b", 2, null, false);
        upsertTemplate("TESTING", "测试", "Testing", "#8b5cf6", 3, null, false);
        upsertTemplate("DONE", "完成", "Done", "#22c55e", 4, null, true);
        jdbcTemplate.update("DELETE FROM board_column_templates WHERE template_key NOT IN ('BACKLOG', 'READY', 'IN_PROGRESS', 'TESTING', 'DONE')");
    }

    @Test
    void adminCanListTemplatesAndUpdateSyncsProjectColumns() throws Exception {
        String adminToken = seedUserAndLogin("admin", "Admin", "ADMIN");
        RegisteredUser owner = register("owner", "Owner");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");

        mockMvc.perform(get("/api/admin/board-templates")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(5))
            .andExpect(jsonPath("$.data[4].templateKey").value("DONE"));

        mockMvc.perform(patch("/api/admin/board-templates/{templateKey}", "DONE")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nameZh": "已完成",
                      "nameEn": "Done",
                      "color": "#16a34a",
                      "wipLimit": 7,
                      "isDone": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.displayName").value("已完成（Done）"));

        assertThat(jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM board_columns
            WHERE project_id = ? AND template_key = 'DONE'
              AND name = '已完成（Done）' AND color = '#16a34a'
              AND sort_order = 4 AND wip_limit = 7 AND is_done = true
            """,
            Integer.class,
            projectId
        )).isEqualTo(1);
    }

    @Test
    void ordinaryMemberCannotManageTemplates() throws Exception {
        RegisteredUser member = register("member", "Member");

        mockMvc.perform(get("/api/admin/board-templates")
                .header("Authorization", "Bearer " + member.token()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void ordinaryMemberGetsForbiddenBeforeInvalidTemplateBodiesAreValidated() throws Exception {
        RegisteredUser member = register("member", "Member");

        mockMvc.perform(post("/api/admin/board-templates")
                .header("Authorization", "Bearer " + member.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateKey": "bad key",
                      "nameZh": "",
                      "nameEn": "",
                      "color": "blue",
                      "wipLimit": 0
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/admin/board-templates/{templateKey}", "DONE")
                .header("Authorization", "Bearer " + member.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nameZh": "",
                      "nameEn": "",
                      "color": "blue",
                      "wipLimit": -1
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/admin/board-templates/reorder")
                .header("Authorization", "Bearer " + member.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateKeys": []
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void deletingTemplateWithMatchingTasksIsBlocked() throws Exception {
        String adminToken = seedUserAndLogin("admin", "Admin", "ADMIN");
        RegisteredUser owner = register("owner", "Owner");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");
        long backlogColumnId = jdbcTemplate.queryForObject(
            "SELECT id FROM board_columns WHERE project_id = ? AND template_key = 'BACKLOG'",
            Long.class,
            projectId
        );
        jdbcTemplate.update(
            """
            INSERT INTO tasks (project_id, column_id, creator_id, title, priority, task_type, sort_order)
            VALUES (?, ?, ?, 'Keep backlog', 'MEDIUM', 'TASK', 0)
            """,
            projectId,
            backlogColumnId,
            owner.id()
        );

        mockMvc.perform(delete("/api/admin/board-templates/{templateKey}", "BACKLOG")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("TEMPLATE_COLUMN_NOT_EMPTY"));
    }

    @Test
    void deletingTemplateWithoutTasksDeletesMatchingProjectColumnsAndTemplate() throws Exception {
        String adminToken = seedUserAndLogin("admin", "Admin", "ADMIN");
        RegisteredUser owner = register("owner", "Owner");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");

        mockMvc.perform(delete("/api/admin/board-templates/{templateKey}", "TESTING")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM board_columns WHERE project_id = ? AND template_key = 'TESTING'",
            Integer.class,
            projectId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM board_column_templates WHERE template_key = 'TESTING'",
            Integer.class
        )).isZero();
    }

    @Test
    void createAndReorderValidatePayloadsAndDuplicateTemplateKeys() throws Exception {
        String adminToken = seedUserAndLogin("admin", "Admin", "ADMIN");

        mockMvc.perform(post("/api/admin/board-templates")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateKey": "DONE",
                      "nameZh": "重复",
                      "nameEn": "Duplicate",
                      "color": "#111827",
                      "isDone": false
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TEMPLATE_KEY_EXISTS"));

        mockMvc.perform(post("/api/admin/board-templates")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateKey": "REVIEW",
                      "nameZh": "评审",
                      "nameEn": "Review",
                      "color": "#111827",
                      "wipLimit": 2,
                      "isDone": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.templateKey").value("REVIEW"))
            .andExpect(jsonPath("$.data.sortOrder").value(5));

        mockMvc.perform(patch("/api/admin/board-templates/reorder")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateKeys": ["REVIEW", "BACKLOG"]
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("TEMPLATE_REORDER_INVALID"));

        mockMvc.perform(patch("/api/admin/board-templates/reorder")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateKeys": ["REVIEW", "BACKLOG", "READY", "IN_PROGRESS", "TESTING", "DONE"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].templateKey").value("REVIEW"));
    }

    @Test
    void adminCreateAndUpdateRejectNonPositiveWipLimits() throws Exception {
        String adminToken = seedUserAndLogin("admin", "Admin", "ADMIN");

        mockMvc.perform(post("/api/admin/board-templates")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "templateKey": "REVIEW",
                      "nameZh": "评审",
                      "nameEn": "Review",
                      "color": "#111827",
                      "wipLimit": 0,
                      "isDone": false
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(patch("/api/admin/board-templates/{templateKey}", "DONE")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nameZh": "已完成",
                      "nameEn": "Done",
                      "color": "#16a34a",
                      "wipLimit": -1,
                      "isDone": true
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
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

    private String seedUserAndLogin(String account, String nickname, String role) throws Exception {
        jdbcTemplate.update(
            """
            INSERT INTO users (account, nickname, email, password_hash, status, role)
            VALUES (?, ?, ?, ?, 'ACTIVE', ?)
            """,
            account,
            nickname,
            account + "@example.com",
            passwordEncoder.encode("1"),
            role
        );

        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "account": "%s",
                      "password": "1"
                    }
                    """.formatted(account)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).path("data").path("token").asText();
    }

    private record RegisteredUser(long id, String token) {
    }
}
