package com.sdkanban.sprint;

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
class SprintControllerTest {
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
    void projectMemberCanCreateAndUpdateSprints() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");
        addMember(owner.token(), projectId, member.id());

        String createResponse = mockMvc.perform(post("/api/projects/{projectId}/sprints", projectId)
                .header("Authorization", "Bearer " + member.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Sprint 1",
                      "goal": "Ship the first workflow",
                      "startDate": "2026-05-21",
                      "endDate": "2026-06-04",
                      "status": "PLANNED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Sprint 1"))
            .andExpect(jsonPath("$.data.goal").value("Ship the first workflow"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        long sprintId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        mockMvc.perform(patch("/api/projects/{projectId}/sprints/{sprintId}", projectId, sprintId)
                .header("Authorization", "Bearer " + member.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Sprint 1A",
                      "goal": "Stabilize the first workflow",
                      "startDate": "2026-05-22",
                      "endDate": "2026-06-05",
                      "status": "ACTIVE"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Sprint 1A"))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/projects/{projectId}/sprints", projectId)
                .header("Authorization", "Bearer " + member.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(sprintId));

        String status = jdbcTemplate.queryForObject(
            "SELECT status FROM sprints WHERE id = ? AND project_id = ?",
            String.class,
            sprintId,
            projectId
        );
        assertThat(status).isEqualTo("ACTIVE");
    }

    @Test
    void sprintEndDateCannotBeBeforeStartDate() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");

        mockMvc.perform(post("/api/projects/{projectId}/sprints", projectId)
                .header("Authorization", "Bearer " + owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Impossible Sprint",
                      "startDate": "2026-06-04",
                      "endDate": "2026-05-21"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("SPRINT_DATE_RANGE_INVALID"));
    }

    @Test
    void projectMemberCanDeleteSprint() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");
        addMember(owner.token(), projectId, member.id());
        long sprintId = createSprint(member.token(), projectId, "Sprint to delete");

        mockMvc.perform(delete("/api/projects/{projectId}/sprints/{sprintId}", projectId, sprintId)
                .header("Authorization", "Bearer " + member.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        Integer sprintCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sprints WHERE id = ? AND project_id = ?",
            Integer.class,
            sprintId,
            projectId
        );
        assertThat(sprintCount).isZero();
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
