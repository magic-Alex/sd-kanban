package com.sdkanban.project;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

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
    void creatingProjectSetsOwnerToCurrentUser() throws Exception {
        RegisteredUser alice = register("alice", "Alice");

        long projectId = createProject(alice.token(), "Roadmap", "Plan the release");

        Long ownerId = jdbcTemplate.queryForObject(
            "SELECT owner_id FROM projects WHERE id = ?",
            Long.class,
            projectId
        );
        assertThat(ownerId).isEqualTo(alice.id());
    }

    @Test
    void creatingProjectCreatesOwnerMembership() throws Exception {
        RegisteredUser alice = register("alice", "Alice");

        long projectId = createProject(alice.token(), "Roadmap", "Plan the release");

        String role = jdbcTemplate.queryForObject(
            "SELECT role FROM project_members WHERE project_id = ? AND user_id = ?",
            String.class,
            projectId,
            alice.id()
        );
        assertThat(role).isEqualTo("owner");
    }

    @Test
    void projectListReturnsOwnedAndJoinedProjectsOnly() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        RegisteredUser outsider = register("outsider", "Outsider");
        long ownedProjectId = createProject(member.token(), "Owned", "Created by member");
        long joinedProjectId = createProject(owner.token(), "Joined", "Joined by member");
        long hiddenProjectId = createProject(outsider.token(), "Hidden", "Not visible");
        addMember(owner.token(), joinedProjectId, member.id());

        mockMvc.perform(get("/api/projects")
                .header("Authorization", "Bearer " + member.token()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[?(@.id == %d)]".formatted(ownedProjectId)).exists())
            .andExpect(jsonPath("$.data[?(@.id == %d)]".formatted(joinedProjectId)).exists())
            .andExpect(jsonPath("$.data[?(@.id == %d)]".formatted(hiddenProjectId)).doesNotExist());
    }

    @Test
    void nonMemberCannotReadProjectDetail() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser outsider = register("outsider", "Outsider");
        long projectId = createProject(owner.token(), "Private", "Members only");

        mockMvc.perform(get("/api/projects/{projectId}", projectId)
                .header("Authorization", "Bearer " + outsider.token()))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT_MEMBER_REQUIRED"));
    }

    @Test
    void projectOwnerCanAddMember() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        long projectId = createProject(owner.token(), "Team Board", "Shared work");

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                .header("Authorization", "Bearer " + owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": %d
                    }
                    """.formatted(member.id())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.id").value(member.id()))
            .andExpect(jsonPath("$.data.role").value("member"));

        String role = jdbcTemplate.queryForObject(
            "SELECT role FROM project_members WHERE project_id = ? AND user_id = ?",
            String.class,
            projectId,
            member.id()
        );
        assertThat(role).isEqualTo("member");
    }

    @Test
    void ordinaryMemberCannotTransferProjectOwner() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        long projectId = createProject(owner.token(), "Team Board", "Shared work");
        addMember(owner.token(), projectId, member.id());

        mockMvc.perform(patch("/api/projects/{projectId}/owner", projectId)
                .header("Authorization", "Bearer " + member.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": %d
                    }
                    """.formatted(member.id())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT_OWNER_REQUIRED"));
    }

    @Test
    void ownerTransferChangesProjectOwnerAndMembershipRoles() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        long projectId = createProject(owner.token(), "Team Board", "Shared work");
        addMember(owner.token(), projectId, member.id());

        mockMvc.perform(patch("/api/projects/{projectId}/owner", projectId)
                .header("Authorization", "Bearer " + owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": %d
                    }
                    """.formatted(member.id())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.owner.id").value(member.id()));

        Long ownerId = jdbcTemplate.queryForObject(
            "SELECT owner_id FROM projects WHERE id = ?",
            Long.class,
            projectId
        );
        assertThat(ownerId).isEqualTo(member.id());

        List<String> roles = jdbcTemplate.query(
            "SELECT role FROM project_members WHERE project_id = ? ORDER BY user_id",
            (rs, rowNum) -> rs.getString("role"),
            projectId
        );
        assertThat(roles).containsExactly("member", "owner");
    }

    @Test
    void ownerTransferCleansUpExtraOwnerRolesAndMatchesCanonicalOwner() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        RegisteredUser extraOwner = register("extra-owner", "Extra Owner");
        long projectId = createProject(owner.token(), "Team Board", "Shared work");
        addMember(owner.token(), projectId, member.id());
        addMember(owner.token(), projectId, extraOwner.id());
        jdbcTemplate.update(
            "UPDATE project_members SET role = 'owner' WHERE project_id = ? AND user_id = ?",
            projectId,
            extraOwner.id()
        );

        mockMvc.perform(patch("/api/projects/{projectId}/owner", projectId)
                .header("Authorization", "Bearer " + owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": %d
                    }
                    """.formatted(member.id())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.owner.id").value(member.id()));

        Long canonicalOwnerId = jdbcTemplate.queryForObject(
            "SELECT owner_id FROM projects WHERE id = ?",
            Long.class,
            projectId
        );
        assertThat(canonicalOwnerId).isEqualTo(member.id());
        assertThat(ownerRoleUserIds(projectId)).containsExactly(member.id());
    }

    @Test
    void ownerTransferToNonMemberIsRejectedWithoutChangingRoles() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        RegisteredUser outsider = register("outsider", "Outsider");
        long projectId = createProject(owner.token(), "Team Board", "Shared work");
        addMember(owner.token(), projectId, member.id());

        mockMvc.perform(patch("/api/projects/{projectId}/owner", projectId)
                .header("Authorization", "Bearer " + owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": %d
                    }
                    """.formatted(outsider.id())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT_MEMBER_REQUIRED"));

        Long canonicalOwnerId = jdbcTemplate.queryForObject(
            "SELECT owner_id FROM projects WHERE id = ?",
            Long.class,
            projectId
        );
        assertThat(canonicalOwnerId).isEqualTo(owner.id());
        assertThat(ownerRoleUserIds(projectId)).containsExactly(owner.id());
        assertThat(roleOf(projectId, member.id())).isEqualTo("member");
    }

    @Test
    void ownerCanRemoveProjectMember() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        long projectId = createProject(owner.token(), "Team Board", "Shared work");
        addMember(owner.token(), projectId, member.id());

        mockMvc.perform(delete("/api/projects/{projectId}/members/{userId}", projectId, member.id())
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        Integer memberCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM project_members WHERE project_id = ? AND user_id = ?",
            Integer.class,
            projectId,
            member.id()
        );
        assertThat(memberCount).isZero();
        assertThat(ownerRoleUserIds(projectId)).containsExactly(owner.id());
    }

    @Test
    void oldOwnerCannotRemoveTransferTargetWhileOwnerTransferIsCommitting() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        long projectId = createProject(owner.token(), "Team Board", "Shared work");
        addMember(owner.token(), projectId, member.id());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch transferReady = new CountDownLatch(1);
        CountDownLatch allowTransferCommit = new CountDownLatch(1);
        try {
            Future<?> transfer = executor.submit(() -> new TransactionTemplate(transactionManager)
                .executeWithoutResult(status -> {
                    jdbcTemplate.queryForObject(
                        "SELECT id FROM projects WHERE id = ? FOR UPDATE",
                        Long.class,
                        projectId
                    );
                    jdbcTemplate.update(
                        "UPDATE project_members SET role = 'member' WHERE project_id = ? AND user_id = ?",
                        projectId,
                        owner.id()
                    );
                    jdbcTemplate.update(
                        "UPDATE project_members SET role = 'owner' WHERE project_id = ? AND user_id = ?",
                        projectId,
                        member.id()
                    );
                    jdbcTemplate.update(
                        "UPDATE projects SET owner_id = ? WHERE id = ?",
                        member.id(),
                        projectId
                    );
                    transferReady.countDown();
                    try {
                        assertThat(allowTransferCommit.await(5, TimeUnit.SECONDS)).isTrue();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(exception);
                    }
                }));

            assertThat(transferReady.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> removal = executor.submit(() -> {
                try {
                    mockMvc.perform(delete("/api/projects/{projectId}/members/{userId}", projectId, member.id())
                            .header("Authorization", "Bearer " + owner.token()))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.code").value("PROJECT_OWNER_REQUIRED"));
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            });

            Thread.sleep(250);
            allowTransferCommit.countDown();
            transfer.get(5, TimeUnit.SECONDS);
            removal.get(5, TimeUnit.SECONDS);
        } finally {
            allowTransferCommit.countDown();
            executor.shutdownNow();
        }

        Long canonicalOwnerId = jdbcTemplate.queryForObject(
            "SELECT owner_id FROM projects WHERE id = ?",
            Long.class,
            projectId
        );
        assertThat(canonicalOwnerId).isEqualTo(member.id());
        assertThat(ownerRoleUserIds(projectId)).containsExactly(member.id());
        assertThat(roleOf(projectId, member.id())).isEqualTo("owner");
    }

    @Test
    void addingDuplicateProjectMemberReturnsConflict() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        RegisteredUser member = register("member", "Member");
        long projectId = createProject(owner.token(), "Team Board", "Shared work");
        addMember(owner.token(), projectId, member.id());

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                .header("Authorization", "Bearer " + owner.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": %d
                    }
                    """.formatted(member.id())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("PROJECT_MEMBER_EXISTS"));
    }

    @Test
    void ownerCannotRemoveCurrentOwnerFromMembers() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        long projectId = createProject(owner.token(), "Team Board", "Shared work");

        mockMvc.perform(delete("/api/projects/{projectId}/members/{userId}", projectId, owner.id())
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("CANNOT_REMOVE_PROJECT_OWNER"));
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

    private List<Long> ownerRoleUserIds(long projectId) {
        return jdbcTemplate.query(
            "SELECT user_id FROM project_members WHERE project_id = ? AND role = 'owner' ORDER BY user_id",
            (rs, rowNum) -> rs.getLong("user_id"),
            projectId
        );
    }

    private String roleOf(long projectId, long userId) {
        return jdbcTemplate.queryForObject(
            "SELECT role FROM project_members WHERE project_id = ? AND user_id = ?",
            String.class,
            projectId,
            userId
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

    private record RegisteredUser(long id, String token) {
    }
}
