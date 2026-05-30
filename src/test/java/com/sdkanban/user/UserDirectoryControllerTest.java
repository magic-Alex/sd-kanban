package com.sdkanban.user;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserDirectoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void deleteUsers() {
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
    void authenticatedSearchReturnsOnlyActiveMatchingUsers() throws Exception {
        String token = seedUserAndLogin("requester", "Requester", "requester@example.com", "ACTIVE");
        seedUser("alpha", "Alpha Match", "alpha@example.com", "ACTIVE");
        seedUser("beta", "Beta Match", "beta.match@example.com", "ACTIVE");
        seedUser("gamma", "Gamma", "gamma@example.com", "ACTIVE");
        seedUser("disabled-match", "Disabled Match", "disabled.match@example.com", "DISABLED");

        mockMvc.perform(get("/api/users/directory")
                .header("Authorization", "Bearer " + token)
                .param("keyword", "mAtCh"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].account").value("alpha"))
            .andExpect(jsonPath("$.data[0].nickname").value("Alpha Match"))
            .andExpect(jsonPath("$.data[1].account").value("beta"))
            .andExpect(jsonPath("$.data[1].email").value("beta.match@example.com"))
            .andExpect(jsonPath("$.data[?(@.account == 'disabled-match')]").doesNotExist())
            .andExpect(jsonPath("$.data[?(@.account == 'gamma')]").doesNotExist());
    }

    @Test
    void directoryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/directory"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void missingKeywordReturnsActiveUsersSortedByNicknameAndAccount() throws Exception {
        String token = seedUserAndLogin("requester", "Requester", "requester@example.com", "ACTIVE");
        seedUser("zeta", "Zed", "zeta@example.com", "ACTIVE");
        seedUser("alpha-2", "Alpha", "alpha2@example.com", "ACTIVE");
        seedUser("alpha-1", "Alpha", "alpha1@example.com", "ACTIVE");
        seedUser("disabled", "Aardvark", "disabled@example.com", "DISABLED");

        mockMvc.perform(get("/api/users/directory")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(4))
            .andExpect(jsonPath("$.data[0].account").value("alpha-1"))
            .andExpect(jsonPath("$.data[1].account").value("alpha-2"))
            .andExpect(jsonPath("$.data[2].account").value("requester"))
            .andExpect(jsonPath("$.data[3].account").value("zeta"))
            .andExpect(jsonPath("$.data[?(@.account == 'disabled')]").doesNotExist());
    }

    private String seedUserAndLogin(String account, String nickname, String email, String status) throws Exception {
        seedUser(account, nickname, email, status);

        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "account": "%s",
                      "password": "secret123"
                    }
                    """.formatted(account)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("token").asText();
    }

    private void seedUser(String account, String nickname, String email, String status) {
        jdbcTemplate.update(
            """
                INSERT INTO users (account, nickname, email, password_hash, status, role)
                VALUES (?, ?, ?, ?, ?, 'MEMBER')
                """,
            account,
            nickname,
            email,
            passwordEncoder.encode("secret123"),
            status
        );
    }
}
