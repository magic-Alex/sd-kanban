# Board Template Personal Board Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single global board-column template, project code/color support, project member management, and a draggable one-lane personal task board that preserves each task's project binding.

**Architecture:** Keep the existing Spring Boot controller/service/service-impl/repository shape. Add a global `board_column_templates` model and use `template_key` to link global statuses to per-project `board_columns`; project boards still operate on concrete project columns, while the personal board moves tasks by template key and resolves the correct project column server-side.

**Tech Stack:** Spring Boot 3, Spring Data JPA, Flyway, MySQL, Vue 3, Pinia, Vitest, Maven.

---

## File Structure

### Backend

- Create `src/main/resources/db/migration/V7__board_templates_project_codes.sql`
  - Adds project code/color, board column template table, and board column template keys.
- Create `src/main/java/com/sdkanban/settings/entity/BoardColumnTemplate.java`
  - JPA entity for the global column template.
- Create `src/main/java/com/sdkanban/settings/repository/BoardColumnTemplateRepository.java`
  - CRUD and lookup methods by template key and sort order.
- Create `src/main/java/com/sdkanban/settings/dto/BoardColumnTemplateResponse.java`
  - API response for settings page and board rendering.
- Create `src/main/java/com/sdkanban/settings/dto/CreateBoardColumnTemplateRequest.java`
  - Request for creating a template column.
- Create `src/main/java/com/sdkanban/settings/dto/UpdateBoardColumnTemplateRequest.java`
  - Request for editing a template column.
- Create `src/main/java/com/sdkanban/settings/dto/ReorderBoardColumnTemplatesRequest.java`
  - Request for global template reordering.
- Create `src/main/java/com/sdkanban/settings/controller/BoardTemplateController.java`
  - Admin-only template API under `/api/admin/board-templates`.
- Create `src/main/java/com/sdkanban/settings/service/BoardTemplateService.java`
  - Interface for listing and mutating global templates plus creating project columns from templates.
- Create `src/main/java/com/sdkanban/settings/service/impl/BoardTemplateServiceImpl.java`
  - Template validation, synchronization, and deletion safety.
- Create `src/main/java/com/sdkanban/task/dto/UpdatePersonalTaskPositionRequest.java`
  - Request body for moving personal-board cards by template key.
- Create `src/main/java/com/sdkanban/user/controller/UserDirectoryController.java`
  - Authenticated active-user directory for project member adding.
- Create `src/main/java/com/sdkanban/user/service/UserDirectoryService.java`
  - Interface for searchable active users.
- Create `src/main/java/com/sdkanban/user/service/impl/UserDirectoryServiceImpl.java`
  - Directory implementation backed by `UserRepository`.
- Modify `src/main/java/com/sdkanban/project/entity/Project.java`
  - Add `projectCode` and `projectColor`.
- Modify `src/main/java/com/sdkanban/project/dto/CreateProjectRequest.java`
  - Require `projectCode` and `projectColor`.
- Modify `src/main/java/com/sdkanban/project/dto/ProjectResponse.java`
  - Return project code and color.
- Modify `src/main/java/com/sdkanban/project/repository/ProjectRepository.java`
  - Add project-code existence lookup.
- Modify `src/main/java/com/sdkanban/project/service/impl/ProjectServiceImpl.java`
  - Validate project code/color and create project columns from global template.
- Modify `src/main/java/com/sdkanban/board/entity/BoardColumn.java`
  - Add `templateKey` and update/sync helpers.
- Modify `src/main/java/com/sdkanban/board/dto/BoardColumnTasks.java`
  - Add `templateKey`.
- Modify `src/main/java/com/sdkanban/board/dto/TaskCardResponse.java`
  - Add project code/name/color and column template key.
- Modify `src/main/java/com/sdkanban/board/dto/MyTaskBoardGroup.java`
  - Use template key as stable group id.
- Modify `src/main/java/com/sdkanban/board/service/impl/BoardServiceImpl.java`
  - Return global template groups for the personal board and enrich cards with project metadata.
- Modify `src/main/java/com/sdkanban/board/service/impl/BoardColumnServiceImpl.java`
  - Disable per-project column mutations with a clear error.
- Modify `src/main/java/com/sdkanban/task/controller/TaskController.java`
  - Add personal-position endpoint.
- Modify `src/main/java/com/sdkanban/task/service/TaskService.java`
  - Add personal-position method.
- Modify `src/main/java/com/sdkanban/task/service/impl/TaskServiceImpl.java`
  - Resolve target column by task project plus target template key.
- Modify `src/main/java/com/sdkanban/user/repository/UserRepository.java`
  - Add active-user directory query.
- Modify backend tests under `src/test/java/com/sdkanban/...`
  - Add focused tests for migration, templates, project metadata, personal board moves, and member management.

### Frontend

- Create `web/src/api/settings.ts`
  - Board template API client.
- Create `web/src/api/user-directory.ts`
  - Active user directory client for project member adding.
- Create `web/src/stores/settings.ts`
  - Pinia store for board templates.
- Create `web/src/views/BoardTemplateSettingsView.vue`
  - Admin settings page for global template editing.
- Create or extend `web/src/components/project/ProjectMemberManager.vue`
  - Member list and add/remove UI.
- Modify `web/src/api/projects.ts`
  - Add project code/color types and member mutation calls.
- Modify `web/src/api/board.ts`
  - Add template key and project metadata to board/card types.
- Modify `web/src/api/tasks.ts`
  - Add personal-position API call.
- Modify `web/src/router/index.ts`
  - Add `/admin/settings/board-template`.
- Modify `web/src/App.vue`
  - Add admin-only System Settings navigation.
- Modify `web/src/views/ProjectListView.vue`
  - Require project code and color on create.
- Modify `web/src/views/ProjectDetailView.vue`
  - Show project metadata and member manager.
- Modify `web/src/views/MyTaskBoardView.vue`
  - Render one global-lane board and support drag/drop by template key.
- Modify `web/src/components/board/BoardColumn.vue`
  - Accept optional `templateKey`, optional add button, and shared drop behavior.
- Modify `web/src/components/board/TaskCard.vue`
  - Show project badge when project metadata is present.
- Modify `web/src/styles/main.css`
  - Add styles for project color swatches, template settings rows, member manager, and personal board cards.
- Modify frontend tests under `web/tests/`
  - Cover settings navigation, project form, member manager, personal-board drag, and task-card project badge.

---

### Task 1: Database Migration And Template Model

**Files:**
- Create: `src/main/resources/db/migration/V7__board_templates_project_codes.sql`
- Create: `src/main/java/com/sdkanban/settings/entity/BoardColumnTemplate.java`
- Create: `src/main/java/com/sdkanban/settings/repository/BoardColumnTemplateRepository.java`
- Create: `src/main/java/com/sdkanban/settings/dto/BoardColumnTemplateResponse.java`
- Modify: `src/main/java/com/sdkanban/board/entity/BoardColumn.java`
- Test: `src/test/java/com/sdkanban/schema/SchemaMigrationTest.java`

- [ ] **Step 1: Write the failing schema migration test**

Add these assertions to `SchemaMigrationTest`:

```java
@Test
void projectAndBoardTemplateColumnsExist() {
    assertThat(columns("projects"))
        .contains("project_code", "project_color");
    assertThat(columns("board_columns"))
        .contains("template_key");
    assertThat(columns("board_column_templates"))
        .contains("template_key", "name_zh", "name_en", "color", "sort_order", "wip_limit", "is_done");
}

@Test
void defaultBoardTemplatesAreSeeded() {
    List<String> labels = jdbcTemplate.query(
        "SELECT CONCAT(name_zh, '（', name_en, '）') FROM board_column_templates ORDER BY sort_order",
        (rs, rowNum) -> rs.getString(1)
    );
    assertThat(labels).containsExactly(
        "待办（Backlog）",
        "就绪（Ready）",
        "进行中（In Progress）",
        "测试（Testing）",
        "完成（Done）"
    );
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=SchemaMigrationTest" test
```

Expected: FAIL because `project_code`, `project_color`, `template_key`, and `board_column_templates` do not exist.

- [ ] **Step 3: Add the Flyway migration**

Create `V7__board_templates_project_codes.sql`:

```sql
ALTER TABLE projects
    ADD COLUMN project_code VARCHAR(40) NULL AFTER creator_id,
    ADD COLUMN project_color VARCHAR(20) NOT NULL DEFAULT '#0f766e' AFTER project_code;

UPDATE projects
SET project_code = CONCAT('PRJ-', id)
WHERE project_code IS NULL;

ALTER TABLE projects
    MODIFY project_code VARCHAR(40) NOT NULL,
    ADD UNIQUE KEY uk_projects_project_code (project_code);

ALTER TABLE board_columns
    ADD COLUMN template_key VARCHAR(60) NULL AFTER project_id,
    ADD KEY idx_board_columns_template_key (template_key);

UPDATE board_columns
SET template_key = CASE
    WHEN sort_order = 0 OR name = 'Backlog' THEN 'BACKLOG'
    WHEN sort_order = 1 OR name = 'Ready' THEN 'READY'
    WHEN sort_order = 2 OR name = 'In Progress' THEN 'IN_PROGRESS'
    WHEN sort_order = 3 OR name = 'Testing' THEN 'TESTING'
    WHEN sort_order = 4 OR name = 'Done' THEN 'DONE'
    ELSE CONCAT('CUSTOM_', id)
END
WHERE template_key IS NULL;

ALTER TABLE board_columns
    MODIFY template_key VARCHAR(60) NOT NULL,
    ADD UNIQUE KEY uk_board_columns_project_template (project_id, template_key);

CREATE TABLE IF NOT EXISTS board_column_templates (
    id BIGINT NOT NULL AUTO_INCREMENT,
    template_key VARCHAR(60) NOT NULL,
    name_zh VARCHAR(80) NOT NULL,
    name_en VARCHAR(80) NOT NULL,
    color VARCHAR(20) NOT NULL,
    sort_order INT NOT NULL,
    wip_limit INT NULL,
    is_done BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_board_column_templates_key (template_key),
    UNIQUE KEY uk_board_column_templates_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO board_column_templates (template_key, name_zh, name_en, color, sort_order, wip_limit, is_done)
VALUES
    ('BACKLOG', '待办', 'Backlog', '#64748b', 0, NULL, false),
    ('READY', '就绪', 'Ready', '#0ea5e9', 1, NULL, false),
    ('IN_PROGRESS', '进行中', 'In Progress', '#f59e0b', 2, NULL, false),
    ('TESTING', '测试', 'Testing', '#8b5cf6', 3, NULL, false),
    ('DONE', '完成', 'Done', '#22c55e', 4, NULL, true)
ON DUPLICATE KEY UPDATE
    name_zh = VALUES(name_zh),
    name_en = VALUES(name_en),
    color = VALUES(color),
    sort_order = VALUES(sort_order),
    wip_limit = VALUES(wip_limit),
    is_done = VALUES(is_done);

UPDATE board_columns column_table
JOIN board_column_templates template_table ON template_table.template_key = column_table.template_key
SET column_table.name = CONCAT(template_table.name_zh, '（', template_table.name_en, '）'),
    column_table.color = template_table.color,
    column_table.sort_order = template_table.sort_order,
    column_table.wip_limit = template_table.wip_limit,
    column_table.is_done = template_table.is_done;
```

- [ ] **Step 4: Add template entity and repository**

Create `BoardColumnTemplate.java`:

```java
package com.sdkanban.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_column_templates")
public class BoardColumnTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_key", nullable = false, length = 60)
    private String templateKey;

    @Column(name = "name_zh", nullable = false, length = 80)
    private String nameZh;

    @Column(name = "name_en", nullable = false, length = 80)
    private String nameEn;

    @Column(nullable = false, length = 20)
    private String color;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "wip_limit")
    private Integer wipLimit;

    @Column(name = "is_done", nullable = false)
    private boolean done;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BoardColumnTemplate() {
    }

    public BoardColumnTemplate(String templateKey, String nameZh, String nameEn, String color, Integer sortOrder, Integer wipLimit, boolean done) {
        this.templateKey = templateKey;
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.color = color;
        this.sortOrder = sortOrder;
        this.wipLimit = wipLimit;
        this.done = done;
    }

    public Long getId() { return id; }
    public String getTemplateKey() { return templateKey; }
    public String getNameZh() { return nameZh; }
    public String getNameEn() { return nameEn; }
    public String getDisplayName() { return nameZh + "（" + nameEn + "）"; }
    public String getColor() { return color; }
    public Integer getSortOrder() { return sortOrder; }
    public Integer getWipLimit() { return wipLimit; }
    public boolean isDone() { return done; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void update(String nameZh, String nameEn, String color, Integer wipLimit, boolean done) {
        this.nameZh = nameZh;
        this.nameEn = nameEn;
        this.color = color;
        this.wipLimit = wipLimit;
        this.done = done;
    }

    public void changeSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
```

Create `BoardColumnTemplateRepository.java`:

```java
package com.sdkanban.settings.repository;

import com.sdkanban.settings.entity.BoardColumnTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardColumnTemplateRepository extends JpaRepository<BoardColumnTemplate, Long> {
    List<BoardColumnTemplate> findByOrderBySortOrderAscIdAsc();
    Optional<BoardColumnTemplate> findByTemplateKey(String templateKey);
    boolean existsByTemplateKey(String templateKey);
}
```

Create `BoardColumnTemplateResponse.java`:

```java
package com.sdkanban.settings.dto;

import com.sdkanban.settings.entity.BoardColumnTemplate;

public record BoardColumnTemplateResponse(
    Long id,
    String templateKey,
    String nameZh,
    String nameEn,
    String displayName,
    String color,
    Integer sortOrder,
    Integer wipLimit,
    boolean isDone
) {
    public static BoardColumnTemplateResponse from(BoardColumnTemplate template) {
        return new BoardColumnTemplateResponse(
            template.getId(),
            template.getTemplateKey(),
            template.getNameZh(),
            template.getNameEn(),
            template.getDisplayName(),
            template.getColor(),
            template.getSortOrder(),
            template.getWipLimit(),
            template.isDone()
        );
    }
}
```

- [ ] **Step 5: Extend `BoardColumn` with template key**

Update constructor and methods:

```java
@Column(name = "template_key", nullable = false, length = 60)
private String templateKey;

public BoardColumn(
    Long projectId,
    String templateKey,
    String name,
    String color,
    Integer sortOrder,
    Integer wipLimit,
    boolean done
) {
    this.projectId = projectId;
    this.templateKey = templateKey;
    this.name = name;
    this.color = color;
    this.sortOrder = sortOrder;
    this.wipLimit = wipLimit;
    this.done = done;
}

public String getTemplateKey() {
    return templateKey;
}

public void syncFromTemplate(String name, String color, Integer sortOrder, Integer wipLimit, boolean done) {
    this.name = name;
    this.color = color;
    this.sortOrder = sortOrder;
    this.wipLimit = wipLimit;
    this.done = done;
}
```

Update old constructor call sites temporarily by passing a literal template key in tests; Task 2 removes hard-coded project-column creation.

- [ ] **Step 6: Run migration test**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=SchemaMigrationTest" test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/main/resources/db/migration/V7__board_templates_project_codes.sql src/main/java/com/sdkanban/settings src/main/java/com/sdkanban/board/entity/BoardColumn.java src/test/java/com/sdkanban/schema/SchemaMigrationTest.java
git commit -m "feat: add global board template schema"
```

---

### Task 2: Template Service And Admin API

**Files:**
- Create: `src/main/java/com/sdkanban/settings/controller/BoardTemplateController.java`
- Create: `src/main/java/com/sdkanban/settings/service/BoardTemplateService.java`
- Create: `src/main/java/com/sdkanban/settings/service/impl/BoardTemplateServiceImpl.java`
- Create: `src/main/java/com/sdkanban/settings/dto/CreateBoardColumnTemplateRequest.java`
- Create: `src/main/java/com/sdkanban/settings/dto/UpdateBoardColumnTemplateRequest.java`
- Create: `src/main/java/com/sdkanban/settings/dto/ReorderBoardColumnTemplatesRequest.java`
- Modify: `src/main/java/com/sdkanban/board/repository/BoardColumnRepository.java`
- Modify: `src/main/java/com/sdkanban/board/service/impl/BoardColumnServiceImpl.java`
- Test: `src/test/java/com/sdkanban/settings/BoardTemplateControllerTest.java`
- Test: `src/test/java/com/sdkanban/board/BoardColumnControllerTest.java`

- [ ] **Step 1: Write failing admin template API tests**

Create `BoardTemplateControllerTest.java` with:

```java
package com.sdkanban.settings;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BoardTemplateControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanData() {
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
    void adminCanListAndRenameTemplateColumns() throws Exception {
        RegisteredUser admin = register("admin", "Admin", "ADMIN");
        RegisteredUser owner = register("owner", "Owner", "MEMBER");
        long projectId = createProject(owner.token(), "SD-KB", "#0f766e");

        mockMvc.perform(patch("/api/admin/board-templates/{templateKey}", "DONE")
                .header("Authorization", "Bearer " + admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nameZh": "已完成",
                      "nameEn": "Done",
                      "color": "#16a34a",
                      "wipLimit": null,
                      "isDone": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.displayName").value("已完成（Done）"));

        String syncedName = jdbcTemplate.queryForObject(
            "SELECT name FROM board_columns WHERE project_id = ? AND template_key = 'DONE'",
            String.class,
            projectId
        );
        assertThat(syncedName).isEqualTo("已完成（Done）");
    }

    @Test
    void ordinaryMemberCannotManageTemplates() throws Exception {
        RegisteredUser member = register("member", "Member", "MEMBER");

        mockMvc.perform(get("/api/admin/board-templates")
                .header("Authorization", "Bearer " + member.token()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void deletingTemplateWithTasksIsBlocked() throws Exception {
        RegisteredUser admin = register("admin", "Admin", "ADMIN");
        RegisteredUser owner = register("owner", "Owner", "MEMBER");
        long projectId = createProject(owner.token(), "SD-KB", "#0f766e");
        long backlogColumnId = jdbcTemplate.queryForObject(
            "SELECT id FROM board_columns WHERE project_id = ? AND template_key = 'BACKLOG'",
            Long.class,
            projectId
        );
        jdbcTemplate.update(
            "INSERT INTO tasks (project_id, column_id, creator_id, title, priority, task_type, sort_order) VALUES (?, ?, ?, 'Blocked deletion', 'MEDIUM', 'TASK', 0)",
            projectId,
            backlogColumnId,
            owner.id()
        );

        mockMvc.perform(delete("/api/admin/board-templates/{templateKey}", "BACKLOG")
                .header("Authorization", "Bearer " + admin.token()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("TEMPLATE_COLUMN_NOT_EMPTY"));
    }
}
```

Use the existing test helper style in project tests for `RegisteredUser`, `register`, and `createProject`; make the helper create `{ "name": "Delivery", "projectCode": code, "projectColor": color }`.

- [ ] **Step 2: Add test that project column mutation API is disabled**

Update `BoardColumnControllerTest`:

```java
@Test
void projectOwnerCannotManageColumnsDirectlyWhenGlobalTemplateIsEnabled() throws Exception {
    RegisteredUser owner = register("owner", "Owner");
    long projectId = createProject(owner.token(), "Delivery", "Delivery board");

    mockMvc.perform(post("/api/projects/{projectId}/columns", projectId)
            .header("Authorization", "Bearer " + owner.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Blocked",
                  "color": "#dc2626"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLOBAL_TEMPLATE_REQUIRED"));
}
```

- [ ] **Step 3: Run tests to verify failure**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=BoardTemplateControllerTest,BoardColumnControllerTest" test
```

Expected: FAIL because settings classes and global-template rules are missing.

- [ ] **Step 4: Create request DTOs**

```java
package com.sdkanban.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateBoardColumnTemplateRequest(
    @NotBlank @Pattern(regexp = "^[A-Z0-9_]{2,60}$") String templateKey,
    @NotBlank @Size(max = 80) String nameZh,
    @NotBlank @Size(max = 80) String nameEn,
    @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color,
    Integer wipLimit,
    Boolean isDone
) {
}
```

```java
package com.sdkanban.settings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateBoardColumnTemplateRequest(
    @NotBlank @Size(max = 80) String nameZh,
    @NotBlank @Size(max = 80) String nameEn,
    @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color,
    Integer wipLimit,
    Boolean isDone
) {
}
```

```java
package com.sdkanban.settings.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderBoardColumnTemplatesRequest(
    @NotEmpty List<String> templateKeys
) {
}
```

- [ ] **Step 5: Implement service interface**

```java
package com.sdkanban.settings.service;

import com.sdkanban.board.entity.BoardColumn;
import com.sdkanban.settings.dto.BoardColumnTemplateResponse;
import com.sdkanban.settings.dto.CreateBoardColumnTemplateRequest;
import com.sdkanban.settings.dto.ReorderBoardColumnTemplatesRequest;
import com.sdkanban.settings.dto.UpdateBoardColumnTemplateRequest;

import java.util.List;

public interface BoardTemplateService {
    List<BoardColumnTemplateResponse> list(Long currentUserId);
    BoardColumnTemplateResponse create(CreateBoardColumnTemplateRequest request, Long currentUserId);
    BoardColumnTemplateResponse update(String templateKey, UpdateBoardColumnTemplateRequest request, Long currentUserId);
    List<BoardColumnTemplateResponse> reorder(ReorderBoardColumnTemplatesRequest request, Long currentUserId);
    void delete(String templateKey, Long currentUserId);
    List<BoardColumn> createProjectColumns(Long projectId);
}
```

- [ ] **Step 6: Implement service**

Implement `BoardTemplateServiceImpl` with these core methods:

```java
private void requireAdmin(Long currentUserId) {
    User user = userRepository.findById(currentUserId)
        .orElseThrow(() -> BusinessException.forbidden("FORBIDDEN", "Access denied"));
    if (!user.isAdmin()) {
        throw BusinessException.forbidden("FORBIDDEN", "Access denied");
    }
}

private String displayName(BoardColumnTemplate template) {
    return template.getNameZh() + "（" + template.getNameEn() + "）";
}

private void syncProjectColumns(BoardColumnTemplate template) {
    boardColumnRepository.findByTemplateKey(template.getTemplateKey())
        .forEach(column -> column.syncFromTemplate(
            displayName(template),
            template.getColor(),
            template.getSortOrder(),
            template.getWipLimit(),
            template.isDone()
        ));
}
```

For `createProjectColumns(Long projectId)`, return:

```java
return boardColumnTemplateRepository.findByOrderBySortOrderAscIdAsc().stream()
    .map(template -> new BoardColumn(
        projectId,
        template.getTemplateKey(),
        displayName(template),
        template.getColor(),
        template.getSortOrder(),
        template.getWipLimit(),
        template.isDone()
    ))
    .toList();
```

For `delete`, use repository count before deleting:

```java
long taskCount = boardColumnRepository.countTasksByTemplateKey(templateKey);
if (taskCount > 0) {
    throw BusinessException.conflict("TEMPLATE_COLUMN_NOT_EMPTY", "该状态下仍有任务，请先迁移任务");
}
boardColumnRepository.deleteByTemplateKey(templateKey);
boardColumnTemplateRepository.delete(template);
```

- [ ] **Step 7: Add repository helpers**

Add to `BoardColumnRepository`:

```java
List<BoardColumn> findByTemplateKey(String templateKey);
Optional<BoardColumn> findByProjectIdAndTemplateKey(Long projectId, String templateKey);
void deleteByTemplateKey(String templateKey);

@Query(value = """
    SELECT COUNT(*)
    FROM tasks task
    JOIN board_columns column_table ON column_table.id = task.column_id
    WHERE column_table.template_key = :templateKey
      AND task.is_deleted = false
    """, nativeQuery = true)
long countTasksByTemplateKey(@Param("templateKey") String templateKey);
```

- [ ] **Step 8: Implement controller**

```java
package com.sdkanban.settings.controller;

import com.sdkanban.common.ApiResponse;
import com.sdkanban.settings.dto.BoardColumnTemplateResponse;
import com.sdkanban.settings.dto.CreateBoardColumnTemplateRequest;
import com.sdkanban.settings.dto.ReorderBoardColumnTemplatesRequest;
import com.sdkanban.settings.dto.UpdateBoardColumnTemplateRequest;
import com.sdkanban.settings.service.BoardTemplateService;
import com.sdkanban.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/board-templates")
public class BoardTemplateController {
    private final BoardTemplateService boardTemplateService;

    public BoardTemplateController(BoardTemplateService boardTemplateService) {
        this.boardTemplateService = boardTemplateService;
    }

    @GetMapping
    ApiResponse<List<BoardColumnTemplateResponse>> list(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(boardTemplateService.list(currentUserId(user)));
    }

    @PostMapping
    ApiResponse<BoardColumnTemplateResponse> create(@Valid @RequestBody CreateBoardColumnTemplateRequest request, @AuthenticationPrincipal User user) {
        return ApiResponse.ok(boardTemplateService.create(request, currentUserId(user)));
    }

    @PatchMapping("/{templateKey}")
    ApiResponse<BoardColumnTemplateResponse> update(@PathVariable String templateKey, @Valid @RequestBody UpdateBoardColumnTemplateRequest request, @AuthenticationPrincipal User user) {
        return ApiResponse.ok(boardTemplateService.update(templateKey, request, currentUserId(user)));
    }

    @PatchMapping("/reorder")
    ApiResponse<List<BoardColumnTemplateResponse>> reorder(@Valid @RequestBody ReorderBoardColumnTemplatesRequest request, @AuthenticationPrincipal User user) {
        return ApiResponse.ok(boardTemplateService.reorder(request, currentUserId(user)));
    }

    @DeleteMapping("/{templateKey}")
    ApiResponse<Void> delete(@PathVariable String templateKey, @AuthenticationPrincipal User user) {
        boardTemplateService.delete(templateKey, currentUserId(user));
        return ApiResponse.ok(null);
    }

    private Long currentUserId(User user) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return user.getId();
    }
}
```

- [ ] **Step 9: Disable per-project column mutation endpoints**

In `BoardColumnServiceImpl.create`, `update`, `reorder`, and `delete`, after permission check, throw:

```java
throw BusinessException.badRequest("GLOBAL_TEMPLATE_REQUIRED", "看板列由系统设置中的统一模板管理");
```

Keep `list` unchanged because project boards still need to read columns.

- [ ] **Step 10: Run tests**

```powershell
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=BoardTemplateControllerTest,BoardColumnControllerTest" test
```

Expected: PASS.

- [ ] **Step 11: Commit**

```powershell
git add src/main/java/com/sdkanban/settings src/main/java/com/sdkanban/board src/test/java/com/sdkanban/settings src/test/java/com/sdkanban/board/BoardColumnControllerTest.java
git commit -m "feat: manage global board templates"
```

---

### Task 3: Project Code, Project Color, And Template-Based Project Creation

**Files:**
- Modify: `src/main/java/com/sdkanban/project/entity/Project.java`
- Modify: `src/main/java/com/sdkanban/project/dto/CreateProjectRequest.java`
- Modify: `src/main/java/com/sdkanban/project/dto/ProjectResponse.java`
- Modify: `src/main/java/com/sdkanban/project/repository/ProjectRepository.java`
- Modify: `src/main/java/com/sdkanban/project/service/impl/ProjectServiceImpl.java`
- Modify: `src/test/java/com/sdkanban/project/ProjectControllerTest.java`
- Modify: other backend test helpers that call `POST /api/projects`

- [ ] **Step 1: Write failing project tests**

Add to `ProjectControllerTest`:

```java
@Test
void creatingProjectRequiresUniqueProjectCodeAndColor() throws Exception {
    RegisteredUser owner = register("owner", "Owner");

    mockMvc.perform(post("/api/projects")
            .header("Authorization", "Bearer " + owner.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Delivery",
                  "projectCode": "SD-KB",
                  "projectColor": "#0f766e",
                  "description": "Kanban project"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.projectCode").value("SD-KB"))
        .andExpect(jsonPath("$.data.projectColor").value("#0f766e"));

    mockMvc.perform(post("/api/projects")
            .header("Authorization", "Bearer " + owner.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "Duplicate",
                  "projectCode": "SD-KB",
                  "projectColor": "#2563eb"
                }
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PROJECT_CODE_EXISTS"));
}
```

Update `creatingProjectCreatesDefaultColumns` expectation to:

```java
assertThat(names).containsExactly("待办（Backlog）", "就绪（Ready）", "进行中（In Progress）", "测试（Testing）", "完成（Done）");
```

- [ ] **Step 2: Run project tests to verify failure**

```powershell
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=ProjectControllerTest,BoardColumnControllerTest" test
```

Expected: FAIL because request/response DTOs and project creation still lack code/color.

- [ ] **Step 3: Update project entity**

Add fields and constructor parameters:

```java
@Column(name = "project_code", nullable = false, length = 40)
private String projectCode;

@Column(name = "project_color", nullable = false, length = 20)
private String projectColor;

public Project(Long ownerId, Long creatorId, String projectCode, String projectColor, String name, String description) {
    this.ownerId = ownerId;
    this.creatorId = creatorId;
    this.projectCode = projectCode;
    this.projectColor = projectColor;
    this.name = name;
    this.description = description;
}

public String getProjectCode() {
    return projectCode;
}

public String getProjectColor() {
    return projectColor;
}
```

- [ ] **Step 4: Update DTOs**

`CreateProjectRequest`:

```java
public record CreateProjectRequest(
    @NotBlank
    @Size(max = 40)
    @Pattern(regexp = "^[A-Z0-9][A-Z0-9-]{1,39}$")
    String projectCode,

    @NotBlank
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$")
    String projectColor,

    @NotBlank
    @Size(max = 120)
    String name,

    @Size(max = 1000)
    String description
) {
}
```

`ProjectResponse`:

```java
public record ProjectResponse(
    Long id,
    String projectCode,
    String projectColor,
    String name,
    String description,
    UserSummary owner,
    UserSummary creator,
    String status,
    long memberCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
```

- [ ] **Step 5: Update repository and service**

Add to `ProjectRepository`:

```java
boolean existsByProjectCode(String projectCode);
```

Inject `BoardTemplateService` into `ProjectServiceImpl` and replace `initializeDefaultColumns` with:

```java
if (projectRepository.existsByProjectCode(normalizeProjectCode(request.projectCode()))) {
    throw BusinessException.conflict("PROJECT_CODE_EXISTS", "Project code already exists");
}

Project project = projectRepository.save(new Project(
    creator.getId(),
    creator.getId(),
    normalizeProjectCode(request.projectCode()),
    request.projectColor().trim(),
    request.name().trim(),
    normalizeDescription(request.description())
));
projectMemberRepository.save(new ProjectMember(project.getId(), creator.getId(), ProjectMember.ROLE_OWNER));
boardColumnRepository.saveAll(boardTemplateService.createProjectColumns(project.getId()));
```

Add:

```java
private String normalizeProjectCode(String projectCode) {
    if (!StringUtils.hasText(projectCode)) {
        throw BusinessException.badRequest("PROJECT_CODE_REQUIRED", "Project code is required");
    }
    return projectCode.trim().toUpperCase();
}
```

Update `toProjectResponse` to include `project.getProjectCode()` and `project.getProjectColor()`.

- [ ] **Step 6: Update backend test helpers**

Every helper that posts `/api/projects` must include:

```json
{
  "projectCode": "SD-KB-<unique>",
  "projectColor": "#0f766e",
  "name": "Delivery",
  "description": "Delivery board"
}
```

Use a unique suffix from the test method or a counter to avoid duplicate project-code conflicts inside one test.

- [ ] **Step 7: Run project-related backend tests**

```powershell
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=ProjectControllerTest,BoardApiTest,BoardColumnControllerTest,TaskControllerTest,DashboardControllerTest" test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add src/main/java/com/sdkanban/project src/test/java/com/sdkanban src/main/java/com/sdkanban/settings src/main/java/com/sdkanban/board
git commit -m "feat: require project code and color"
```

---

### Task 4: Enrich Board Cards And Personal Board Response

**Files:**
- Modify: `src/main/java/com/sdkanban/board/dto/TaskCardResponse.java`
- Modify: `src/main/java/com/sdkanban/board/dto/BoardColumnTasks.java`
- Modify: `src/main/java/com/sdkanban/board/dto/MyTaskBoardGroup.java`
- Modify: `src/main/java/com/sdkanban/board/dto/MyTaskBoardResponse.java`
- Modify: `src/main/java/com/sdkanban/board/service/impl/BoardServiceImpl.java`
- Test: `src/test/java/com/sdkanban/task/MyTaskBoardApiTest.java`
- Test: `src/test/java/com/sdkanban/board/BoardApiTest.java`

- [ ] **Step 1: Write failing API tests**

In `MyTaskBoardApiTest`, add:

```java
@Test
void myTaskBoardReturnsSingleTemplateLaneWithProjectBadges() throws Exception {
    RegisteredUser owner = register("owner", "Owner");
    RegisteredUser member = register("member", "Member");
    long projectId = createProject(owner.token(), "SD-KB", "#0f766e");
    addMember(owner.token(), projectId, member.id());
    long backlogColumnId = columnId(projectId, "BACKLOG");
    createTask(owner.token(), projectId, backlogColumnId, member.id(), "Personal card");

    mockMvc.perform(get("/api/tasks/mine/board")
            .header("Authorization", "Bearer " + member.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.groupBy").value("template"))
        .andExpect(jsonPath("$.data.groups[0].templateKey").value("BACKLOG"))
        .andExpect(jsonPath("$.data.groups[0].name").value("待办（Backlog）"))
        .andExpect(jsonPath("$.data.groups[0].tasks[0].projectCode").value("SD-KB"))
        .andExpect(jsonPath("$.data.groups[0].tasks[0].projectName").value("Delivery"))
        .andExpect(jsonPath("$.data.groups[0].tasks[0].projectColor").value("#0f766e"))
        .andExpect(jsonPath("$.data.groups[0].tasks[0].columnTemplateKey").value("BACKLOG"));
}
```

In `BoardApiTest`, assert each project board column returns `templateKey` and cards return project metadata.

- [ ] **Step 2: Run tests to verify failure**

```powershell
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=MyTaskBoardApiTest,BoardApiTest" test
```

Expected: FAIL because response DTOs lack project and template metadata.

- [ ] **Step 3: Update DTOs**

`TaskCardResponse`:

```java
public record TaskCardResponse(
    Long id,
    Long projectId,
    String projectCode,
    String projectName,
    String projectColor,
    Long sprintId,
    Long columnId,
    String columnTemplateKey,
    Long assigneeId,
    UserSummary assignee,
    String title,
    String taskType,
    String priority,
    BigDecimal storyPoints,
    LocalDate dueDate,
    Integer sortOrder,
    long checklistDoneCount,
    long checklistTotalCount
) {
    public static TaskCardResponse from(Task task, Project project, BoardColumn column, UserSummary assignee, long checklistDoneCount, long checklistTotalCount) {
        return new TaskCardResponse(
            task.getId(),
            task.getProjectId(),
            project.getProjectCode(),
            project.getName(),
            project.getProjectColor(),
            task.getSprintId(),
            task.getColumnId(),
            column.getTemplateKey(),
            task.getAssigneeId(),
            assignee,
            task.getTitle(),
            task.getTaskType(),
            task.getPriority(),
            task.getStoryPoints(),
            task.getDueDate(),
            task.getSortOrder(),
            checklistDoneCount,
            checklistTotalCount
        );
    }
}
```

`BoardColumnTasks`:

```java
public record BoardColumnTasks(
    Long id,
    String templateKey,
    String name,
    String color,
    Integer sortOrder,
    boolean isDone,
    List<TaskCardResponse> tasks
) {
}
```

`MyTaskBoardGroup`:

```java
public record MyTaskBoardGroup(
    String templateKey,
    String name,
    String color,
    Integer sortOrder,
    boolean isDone,
    List<TaskCardResponse> tasks
) {
}
```

`MyTaskBoardResponse`:

```java
public record MyTaskBoardResponse(
    String groupBy,
    List<MyTaskBoardGroup> groups
) {
}
```

- [ ] **Step 4: Update board service card context**

Replace card context with maps for users, projects, columns, and checklist counts:

```java
private record CardContext(
    Map<Long, UserSummary> usersById,
    Map<Long, Project> projectsById,
    Map<Long, BoardColumn> columnsById,
    Map<Long, TaskChecklistItemRepository.ChecklistCountView> checklistCounts
) {
}
```

Build maps from task project IDs and column IDs:

```java
Map<Long, Project> projectsById = projectRepository.findAllById(tasks.stream().map(Task::getProjectId).distinct().toList())
    .stream()
    .collect(Collectors.toMap(Project::getId, Function.identity()));
Map<Long, BoardColumn> columnsById = boardColumnRepository.findAllById(tasks.stream().map(Task::getColumnId).distinct().toList())
    .stream()
    .collect(Collectors.toMap(BoardColumn::getId, Function.identity()));
```

Update `cards`:

```java
return tasks.stream()
    .map(task -> {
        TaskChecklistItemRepository.ChecklistCountView count = cardContext.checklistCounts().get(task.getId());
        long doneCount = count == null ? 0 : count.getDoneCount();
        long totalCount = count == null ? 0 : count.getTotalCount();
        return TaskCardResponse.from(
            task,
            cardContext.projectsById().get(task.getProjectId()),
            cardContext.columnsById().get(task.getColumnId()),
            cardContext.usersById().get(task.getAssigneeId()),
            doneCount,
            totalCount
        );
    })
    .toList();
```

- [ ] **Step 5: Change personal board grouping**

In `myTaskBoard`, ignore `groupBy` and return all template columns:

```java
List<BoardColumnTemplate> templates = boardColumnTemplateRepository.findByOrderBySortOrderAscIdAsc();
Map<String, List<Task>> tasksByTemplate = tasks.stream()
    .collect(Collectors.groupingBy(task -> cardContext.columnsById().get(task.getColumnId()).getTemplateKey()));

List<MyTaskBoardGroup> groups = templates.stream()
    .map(template -> new MyTaskBoardGroup(
        template.getTemplateKey(),
        template.getDisplayName(),
        template.getColor(),
        template.getSortOrder(),
        template.isDone(),
        cards(tasksByTemplate.getOrDefault(template.getTemplateKey(), List.of()), cardContext)
    ))
    .toList();
return new MyTaskBoardResponse("template", groups);
```

- [ ] **Step 6: Run board tests**

```powershell
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=MyTaskBoardApiTest,BoardApiTest" test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/main/java/com/sdkanban/board src/test/java/com/sdkanban/task/MyTaskBoardApiTest.java src/test/java/com/sdkanban/board/BoardApiTest.java
git commit -m "feat: return template personal board data"
```

---

### Task 5: Personal Board Drag API

**Files:**
- Create: `src/main/java/com/sdkanban/task/dto/UpdatePersonalTaskPositionRequest.java`
- Modify: `src/main/java/com/sdkanban/task/controller/TaskController.java`
- Modify: `src/main/java/com/sdkanban/task/service/TaskService.java`
- Modify: `src/main/java/com/sdkanban/task/service/impl/TaskServiceImpl.java`
- Test: `src/test/java/com/sdkanban/task/MyTaskBoardApiTest.java`

- [ ] **Step 1: Write failing personal move tests**

Add:

```java
@Test
void assigneeCanMovePersonalTaskByTemplateKeyWithoutChangingProject() throws Exception {
    RegisteredUser owner = register("owner", "Owner");
    RegisteredUser member = register("member", "Member");
    long projectId = createProject(owner.token(), "SD-KB", "#0f766e");
    addMember(owner.token(), projectId, member.id());
    long backlogColumnId = columnId(projectId, "BACKLOG");
    long readyColumnId = columnId(projectId, "READY");
    long taskId = createTask(owner.token(), projectId, backlogColumnId, member.id(), "Move me");

    mockMvc.perform(patch("/api/tasks/{taskId}/personal-position", taskId)
            .header("Authorization", "Bearer " + member.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetTemplateKey": "READY",
                  "sortOrder": 0
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.projectId").value(projectId))
        .andExpect(jsonPath("$.data.columnId").value(readyColumnId))
        .andExpect(jsonPath("$.data.columnTemplateKey").value("READY"));
}

@Test
void nonAssigneeCannotMovePersonalTask() throws Exception {
    RegisteredUser owner = register("owner", "Owner");
    RegisteredUser member = register("member", "Member");
    RegisteredUser other = register("other", "Other");
    long projectId = createProject(owner.token(), "SD-KB", "#0f766e");
    addMember(owner.token(), projectId, member.id());
    addMember(owner.token(), projectId, other.id());
    long backlogColumnId = columnId(projectId, "BACKLOG");
    long taskId = createTask(owner.token(), projectId, backlogColumnId, member.id(), "Move me");

    mockMvc.perform(patch("/api/tasks/{taskId}/personal-position", taskId)
            .header("Authorization", "Bearer " + other.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "targetTemplateKey": "READY",
                  "sortOrder": 0
                }
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("TASK_ASSIGNEE_REQUIRED"));
}
```

- [ ] **Step 2: Run failing tests**

```powershell
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=MyTaskBoardApiTest" test
```

Expected: FAIL because endpoint and request DTO do not exist.

- [ ] **Step 3: Create request DTO**

```java
package com.sdkanban.task.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdatePersonalTaskPositionRequest(
    @NotBlank
    @Pattern(regexp = "^[A-Z0-9_]{2,60}$")
    String targetTemplateKey,

    @Min(0)
    Integer sortOrder
) {
}
```

- [ ] **Step 4: Add service/controller methods**

`TaskService`:

```java
TaskResponse updatePersonalPosition(Long taskId, UpdatePersonalTaskPositionRequest request, Long currentUserId);
```

`TaskController`:

```java
@PatchMapping("/tasks/{taskId}/personal-position")
ApiResponse<TaskResponse> updatePersonalPosition(
    @PathVariable Long taskId,
    @Valid @RequestBody UpdatePersonalTaskPositionRequest request,
    @AuthenticationPrincipal User user
) {
    return ApiResponse.ok(taskService.updatePersonalPosition(taskId, request, currentUserId(user)));
}
```

- [ ] **Step 5: Implement personal move**

In `TaskServiceImpl`:

```java
@Override
@Transactional
public TaskResponse updatePersonalPosition(Long taskId, UpdatePersonalTaskPositionRequest request, Long currentUserId) {
    Task task = requireTask(taskId);
    projectService.requireMember(task.getProjectId(), currentUserId);
    if (!Objects.equals(task.getAssigneeId(), currentUserId)) {
        throw BusinessException.forbidden("TASK_ASSIGNEE_REQUIRED", "Only the task assignee can move this card on the personal board");
    }
    BoardColumn targetColumn = boardColumnRepository.findByProjectIdAndTemplateKey(task.getProjectId(), request.targetTemplateKey())
        .orElseThrow(() -> BusinessException.badRequest("TARGET_TEMPLATE_COLUMN_MISSING", "该项目缺少目标状态列，请先同步看板模板"));
    change(task, currentUserId, "columnId", task.getColumnId(), targetColumn.getId(), task::changeColumnId);
    change(task, currentUserId, "sortOrder", task.getSortOrder(), request.sortOrder(), task::changeSortOrder);
    return toTaskResponse(task);
}
```

Update `toTaskResponse` to populate `columnTemplateKey` if TaskResponse is extended in this task; otherwise return existing detail response and rely on TaskCardResponse for board metadata.

- [ ] **Step 6: Run personal board API tests**

```powershell
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=MyTaskBoardApiTest,TaskActivityTest" test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/main/java/com/sdkanban/task src/test/java/com/sdkanban/task/MyTaskBoardApiTest.java
git commit -m "feat: move personal board tasks by template"
```

---

### Task 6: Project Member Management API Helpers

**Files:**
- Create: `src/main/java/com/sdkanban/user/controller/UserDirectoryController.java`
- Create: `src/main/java/com/sdkanban/user/service/UserDirectoryService.java`
- Create: `src/main/java/com/sdkanban/user/service/impl/UserDirectoryServiceImpl.java`
- Modify: `src/main/java/com/sdkanban/user/repository/UserRepository.java`
- Modify: `src/test/java/com/sdkanban/project/ProjectControllerTest.java`
- Create: `src/test/java/com/sdkanban/user/UserDirectoryControllerTest.java`

- [ ] **Step 1: Write failing user directory test**

Create:

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserDirectoryControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanData() {
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void authenticatedUsersCanSearchActiveUsersForProjectMemberAdding() throws Exception {
        RegisteredUser owner = register("owner", "Owner", "MEMBER");
        register("alice", "Alice", "MEMBER");
        RegisteredUser disabled = register("disabled", "Disabled", "MEMBER");
        jdbcTemplate.update("UPDATE users SET status = 'DISABLED' WHERE id = ?", disabled.id());

        mockMvc.perform(get("/api/users/directory?keyword=ali")
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].account").value("alice"))
            .andExpect(jsonPath("$.data[0].nickname").value("Alice"));
    }
}
```

- [ ] **Step 2: Run failing test**

```powershell
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=UserDirectoryControllerTest" test
```

Expected: FAIL because `/api/users/directory` does not exist.

- [ ] **Step 3: Add repository query**

```java
@Query("""
    select user from User user
    where user.status = 'ACTIVE'
      and (
        :keyword is null
        or lower(user.account) like lower(concat('%', :keyword, '%'))
        or lower(user.nickname) like lower(concat('%', :keyword, '%'))
        or lower(coalesce(user.email, '')) like lower(concat('%', :keyword, '%'))
      )
    order by user.nickname asc, user.account asc
    """)
List<User> searchActiveUsers(@Param("keyword") String keyword);
```

- [ ] **Step 4: Add service and controller**

`UserDirectoryService`:

```java
public interface UserDirectoryService {
    List<UserSummary> search(String keyword);
}
```

`UserDirectoryServiceImpl`:

```java
@Service
public class UserDirectoryServiceImpl implements UserDirectoryService {
    private final UserRepository userRepository;

    public UserDirectoryServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> search(String keyword) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return userRepository.searchActiveUsers(normalized).stream()
            .map(UserSummary::from)
            .toList();
    }
}
```

`UserDirectoryController`:

```java
@RestController
@RequestMapping("/api/users/directory")
public class UserDirectoryController {
    private final UserDirectoryService userDirectoryService;

    public UserDirectoryController(UserDirectoryService userDirectoryService) {
        this.userDirectoryService = userDirectoryService;
    }

    @GetMapping
    ApiResponse<List<UserSummary>> search(@RequestParam(required = false) String keyword, @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new BadCredentialsException("Authentication required");
        }
        return ApiResponse.ok(userDirectoryService.search(keyword));
    }
}
```

- [ ] **Step 5: Add project-member behavior tests**

In `ProjectControllerTest`, keep existing add/remove/transfer tests and add:

```java
@Test
void projectMemberListReturnsRoleAndJoinedAtForDetailPage() throws Exception {
    RegisteredUser owner = register("owner", "Owner");
    RegisteredUser member = register("member", "Member");
    long projectId = createProject(owner.token(), "SD-KB", "#0f766e");
    addMember(owner.token(), projectId, member.id());

    mockMvc.perform(get("/api/projects/{projectId}/members", projectId)
            .header("Authorization", "Bearer " + owner.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].role").value("owner"))
        .andExpect(jsonPath("$.data[1].role").value("member"))
        .andExpect(jsonPath("$.data[1].user.account").value("member"))
        .andExpect(jsonPath("$.data[1].joinedAt").exists());
}
```

- [ ] **Step 6: Run tests**

```powershell
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=UserDirectoryControllerTest,ProjectControllerTest" test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add src/main/java/com/sdkanban/user src/test/java/com/sdkanban/user/UserDirectoryControllerTest.java src/test/java/com/sdkanban/project/ProjectControllerTest.java
git commit -m "feat: expose active user directory"
```

---

### Task 7: Frontend API Types And Stores

**Files:**
- Create: `web/src/api/settings.ts`
- Create: `web/src/api/user-directory.ts`
- Create: `web/src/stores/settings.ts`
- Modify: `web/src/api/projects.ts`
- Modify: `web/src/api/board.ts`
- Modify: `web/src/api/tasks.ts`
- Modify: `web/src/stores/board.ts`
- Test: `web/tests/settings-store.spec.ts`
- Test: `web/tests/board-store.spec.ts`

- [ ] **Step 1: Write failing frontend API/store tests**

Create `settings-store.spec.ts`:

```ts
import { describe, expect, it, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSettingsStore } from '../src/stores/settings'
import * as settingsApi from '../src/api/settings'

describe('settings store', () => {
  it('loads board templates', async () => {
    setActivePinia(createPinia())
    vi.spyOn(settingsApi, 'fetchBoardTemplates').mockResolvedValue([
      {
        id: 1,
        templateKey: 'BACKLOG',
        nameZh: '待办',
        nameEn: 'Backlog',
        displayName: '待办（Backlog）',
        color: '#64748b',
        sortOrder: 0,
        wipLimit: null,
        isDone: false,
      },
    ])

    const store = useSettingsStore()
    await store.loadBoardTemplates()

    expect(store.boardTemplates[0].displayName).toBe('待办（Backlog）')
  })
})
```

Update `board-store.spec.ts` to expect `movePersonalTask(taskId, 'READY', 0)` calls the new API.

- [ ] **Step 2: Run failing tests**

```powershell
npm test -- settings-store.spec.ts board-store.spec.ts
```

Expected: FAIL because new API and store files are missing.

- [ ] **Step 3: Add settings API and store**

`settings.ts`:

```ts
import { deleteData, getData, patchData, postData } from './http'

export interface BoardColumnTemplate {
  id: number
  templateKey: string
  nameZh: string
  nameEn: string
  displayName: string
  color: string
  sortOrder: number
  wipLimit: number | null
  isDone: boolean
}

export interface SaveBoardColumnTemplateRequest {
  templateKey?: string
  nameZh: string
  nameEn: string
  color: string
  wipLimit?: number | null
  isDone: boolean
}

export function fetchBoardTemplates(): Promise<BoardColumnTemplate[]> {
  return getData<BoardColumnTemplate[]>('/admin/board-templates')
}

export function createBoardTemplate(request: SaveBoardColumnTemplateRequest): Promise<BoardColumnTemplate> {
  return postData<BoardColumnTemplate, SaveBoardColumnTemplateRequest>('/admin/board-templates', request)
}

export function updateBoardTemplate(templateKey: string, request: SaveBoardColumnTemplateRequest): Promise<BoardColumnTemplate> {
  return patchData<BoardColumnTemplate, SaveBoardColumnTemplateRequest>(`/admin/board-templates/${templateKey}`, request)
}

export function reorderBoardTemplates(templateKeys: string[]): Promise<BoardColumnTemplate[]> {
  return patchData<BoardColumnTemplate[], { templateKeys: string[] }>('/admin/board-templates/reorder', { templateKeys })
}

export function deleteBoardTemplate(templateKey: string): Promise<void> {
  return deleteData<void>(`/admin/board-templates/${templateKey}`)
}
```

`settings.ts` store:

```ts
import { defineStore } from 'pinia'
import {
  createBoardTemplate,
  deleteBoardTemplate,
  fetchBoardTemplates,
  reorderBoardTemplates,
  updateBoardTemplate,
  type BoardColumnTemplate,
  type SaveBoardColumnTemplateRequest,
} from '../api/settings'

export const useSettingsStore = defineStore('settings', {
  state: () => ({
    boardTemplates: [] as BoardColumnTemplate[],
    loading: false,
    error: null as string | null,
  }),
  actions: {
    async loadBoardTemplates() {
      this.loading = true
      this.error = null
      try {
        this.boardTemplates = await fetchBoardTemplates()
      } catch (error) {
        this.error = '看板模板加载失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    async saveBoardTemplate(templateKey: string | null, request: SaveBoardColumnTemplateRequest) {
      const saved = templateKey
        ? await updateBoardTemplate(templateKey, request)
        : await createBoardTemplate(request)
      await this.loadBoardTemplates()
      return saved
    },
    async reorder(templateKeys: string[]) {
      this.boardTemplates = await reorderBoardTemplates(templateKeys)
    },
    async remove(templateKey: string) {
      await deleteBoardTemplate(templateKey)
      this.boardTemplates = this.boardTemplates.filter((template) => template.templateKey !== templateKey)
    },
  },
})
```

- [ ] **Step 4: Add user directory API**

```ts
import { getData } from './http'
import type { UserSummary } from './auth'

export function searchActiveUsers(keyword = ''): Promise<UserSummary[]> {
  const query = keyword.trim() ? `?keyword=${encodeURIComponent(keyword.trim())}` : ''
  return getData<UserSummary[]>(`/users/directory${query}`)
}
```

- [ ] **Step 5: Update project, board, and task types**

`projects.ts` add:

```ts
projectCode: string
projectColor: string
```

to `Project`, and to `CreateProjectRequest`:

```ts
projectCode: string
projectColor: string
```

Add:

```ts
export function addProjectMember(projectId: number | string, userId: number): Promise<ProjectMember> {
  return postData<ProjectMember, { userId: number }>(`/projects/${projectId}/members`, { userId })
}

export function removeProjectMember(projectId: number | string, userId: number): Promise<void> {
  return deleteData<void>(`/projects/${projectId}/members/${userId}`)
}
```

`board.ts` add to `TaskCard`:

```ts
projectCode: string
projectName: string
projectColor: string
columnTemplateKey: string
```

Add to `BoardColumn`:

```ts
templateKey: string
```

Change `MyTaskBoardGroup.id` to:

```ts
templateKey: string
color: string
sortOrder: number
isDone: boolean
```

`tasks.ts` add:

```ts
export function updatePersonalTaskPosition(taskId: number, payload: { targetTemplateKey: string; sortOrder: number }) {
  return patchData<TaskResponse, { targetTemplateKey: string; sortOrder: number }>(`/tasks/${taskId}/personal-position`, payload)
}
```

- [ ] **Step 6: Update board store**

Add:

```ts
async movePersonalTask(taskId: number, targetTemplateKey: string, sortOrder: number) {
  const previousBoard = this.myTaskBoard ? structuredClone(this.myTaskBoard) : null
  const sourceGroup = this.myTaskBoard?.groups.find((group) => group.tasks.some((task) => task.id === taskId))
  const targetGroup = this.myTaskBoard?.groups.find((group) => group.templateKey === targetTemplateKey)
  const task = sourceGroup?.tasks.find((candidate) => candidate.id === taskId)
  if (!sourceGroup || !targetGroup || !task) {
    return
  }
  sourceGroup.tasks = sourceGroup.tasks.filter((candidate) => candidate.id !== taskId)
  targetGroup.tasks.splice(sortOrder, 0, { ...task, columnTemplateKey: targetTemplateKey, sortOrder })
  try {
    await updatePersonalTaskPosition(taskId, { targetTemplateKey, sortOrder })
  } catch (error) {
    this.myTaskBoard = previousBoard
    throw error
  }
}
```

- [ ] **Step 7: Run frontend API/store tests**

```powershell
npm test -- settings-store.spec.ts board-store.spec.ts
```

Expected: PASS.

- [ ] **Step 8: Commit**

```powershell
git add web/src/api web/src/stores web/tests/settings-store.spec.ts web/tests/board-store.spec.ts
git commit -m "feat: add board template frontend APIs"
```

---

### Task 8: Project UI And Member Management

**Files:**
- Create: `web/src/components/project/ProjectMemberManager.vue`
- Modify: `web/src/views/ProjectListView.vue`
- Modify: `web/src/views/ProjectDetailView.vue`
- Modify: `web/src/stores/projects.ts`
- Modify: `web/src/styles/main.css`
- Test: `web/tests/project-list-view.spec.ts`
- Test: `web/tests/project-detail-view.spec.ts`

- [ ] **Step 1: Write failing UI tests**

Create or update `project-list-view.spec.ts`:

```ts
it('requires project code and color when creating a project', async () => {
  const wrapper = mount(ProjectListView, {
    global: { plugins: [createTestingPinia()] },
  })

  expect(wrapper.text()).toContain('项目编号')
  expect(wrapper.find('input[aria-label="项目编号"]').exists()).toBe(true)
  expect(wrapper.find('input[aria-label="项目颜色"]').exists()).toBe(true)
})
```

Create `project-detail-view.spec.ts`:

```ts
it('shows project metadata and member management', async () => {
  const wrapper = mount(ProjectDetailView, {
    global: {
      plugins: [createTestingPinia({
        initialState: {
          projects: {
            currentProject: {
              id: 1,
              projectCode: 'SD-KB',
              projectColor: '#0f766e',
              name: 'Delivery',
              description: 'Work',
              owner: { id: 1, account: 'owner', nickname: 'Owner', email: 'owner@example.com' },
              creator: { id: 1, account: 'owner', nickname: 'Owner', email: 'owner@example.com' },
              status: 'ACTIVE',
              memberCount: 1,
              createdAt: '2026-05-31T00:00:00',
              updatedAt: '2026-05-31T00:00:00',
            },
          },
        },
      })],
    },
  })

  expect(wrapper.text()).toContain('SD-KB')
  expect(wrapper.text()).toContain('项目成员')
  expect(wrapper.text()).toContain('添加成员')
})
```

- [ ] **Step 2: Run failing tests**

```powershell
npm test -- project-list-view.spec.ts project-detail-view.spec.ts
```

Expected: FAIL because UI fields and member manager are missing.

- [ ] **Step 3: Update project store**

Add state and actions:

```ts
members: [] as ProjectMember[],
memberActionError: null as string | null,

async fetchMembers(projectId: number | string) {
  this.members = await fetchProjectMembers(projectId)
},
async addMember(projectId: number | string, userId: number) {
  const member = await addProjectMember(projectId, userId)
  this.members = [...this.members, member]
  if (this.currentProject) {
    this.currentProject = { ...this.currentProject, memberCount: this.currentProject.memberCount + 1 }
  }
},
async removeMember(projectId: number | string, userId: number) {
  await removeProjectMember(projectId, userId)
  this.members = this.members.filter((member) => member.user.id !== userId)
  if (this.currentProject) {
    this.currentProject = { ...this.currentProject, memberCount: Math.max(0, this.currentProject.memberCount - 1) }
  }
}
```

- [ ] **Step 4: Update project create form**

In `ProjectListView.vue` form state:

```ts
const form = reactive({
  projectCode: '',
  projectColor: '#0f766e',
  name: '',
  description: '',
})
```

Submit:

```ts
const project = await projects.createProject({
  projectCode: form.projectCode,
  projectColor: form.projectColor,
  name: form.name,
  description: form.description,
})
```

Template fields:

```vue
<label>
  项目编号
  <input v-model="form.projectCode" aria-label="项目编号" required maxlength="40" placeholder="SD-KB" />
</label>
<label>
  项目颜色
  <input v-model="form.projectColor" aria-label="项目颜色" type="color" required />
</label>
```

- [ ] **Step 5: Create member manager component**

`ProjectMemberManager.vue`:

```vue
<script setup lang="ts">
import { ref } from 'vue'
import type { ProjectMember } from '../../api/projects'
import type { UserSummary } from '../../api/auth'
import { searchActiveUsers } from '../../api/user-directory'

defineProps<{
  members: ProjectMember[]
  ownerId: number
  currentUserId: number | null
}>()

const emit = defineEmits<{
  addMember: [userId: number]
  removeMember: [userId: number]
}>()

const keyword = ref('')
const candidates = ref<UserSummary[]>([])
const searching = ref(false)

async function searchUsers() {
  searching.value = true
  try {
    candidates.value = await searchActiveUsers(keyword.value)
  } finally {
    searching.value = false
  }
}
</script>

<template>
  <section class="panel-block member-manager">
    <header class="panel-header">
      <h2>项目成员</h2>
    </header>
    <form class="member-search" @submit.prevent="searchUsers">
      <input v-model="keyword" aria-label="搜索用户" placeholder="搜索账号、昵称或邮箱" />
      <button class="secondary-button" type="submit" :disabled="searching">搜索</button>
    </form>
    <div class="candidate-list">
      <button
        v-for="user in candidates"
        :key="user.id"
        class="secondary-button"
        type="button"
        @click="emit('addMember', user.id)"
      >
        添加 {{ user.nickname }}
      </button>
    </div>
    <ul class="member-list">
      <li v-for="member in members" :key="member.user.id">
        <span>
          <strong>{{ member.user.nickname }}</strong>
          <small>{{ member.user.email || member.user.account }}</small>
        </span>
        <span>{{ member.role === 'owner' ? '负责人' : '成员' }}</span>
        <button
          v-if="member.role !== 'owner'"
          class="danger-button"
          type="button"
          @click="emit('removeMember', member.user.id)"
        >
          移除
        </button>
      </li>
    </ul>
  </section>
</template>
```

- [ ] **Step 6: Update project detail**

On mount, load detail and members:

```ts
onMounted(async () => {
  await projects.fetchProject(projectId.value)
  await projects.fetchMembers(projectId.value)
})
```

Add handlers:

```ts
async function addMember(userId: number) {
  await projects.addMember(projectId.value, userId)
}

async function removeMember(userId: number) {
  await projects.removeMember(projectId.value, userId)
}
```

Render project metadata and member manager:

```vue
<article class="panel-block">
  <h2>项目编号</h2>
  <p>
    <span class="project-color-dot" :style="{ background: projects.currentProject.projectColor }"></span>
    {{ projects.currentProject.projectCode }}
  </p>
</article>
<ProjectMemberManager
  :members="projects.members"
  :owner-id="projects.currentProject.owner.id"
  :current-user-id="auth.user?.id ?? null"
  @add-member="addMember"
  @remove-member="removeMember"
/>
```

- [ ] **Step 7: Add styles**

Add classes:

```css
.project-color-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 8px;
  vertical-align: middle;
}

.member-manager {
  grid-column: 1 / -1;
}

.member-search,
.candidate-list,
.member-list li {
  display: flex;
  gap: 10px;
  align-items: center;
}

.member-list {
  list-style: none;
  padding: 0;
  margin: 16px 0 0;
  display: grid;
  gap: 10px;
}
```

- [ ] **Step 8: Run frontend UI tests**

```powershell
npm test -- project-list-view.spec.ts project-detail-view.spec.ts
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add web/src/views/ProjectListView.vue web/src/views/ProjectDetailView.vue web/src/components/project web/src/stores/projects.ts web/src/styles/main.css web/tests/project-list-view.spec.ts web/tests/project-detail-view.spec.ts
git commit -m "feat: add project metadata and members UI"
```

---

### Task 9: Frontend Settings Page And Personal Board Drag UI

**Files:**
- Create: `web/src/views/BoardTemplateSettingsView.vue`
- Modify: `web/src/router/index.ts`
- Modify: `web/src/App.vue`
- Modify: `web/src/views/MyTaskBoardView.vue`
- Modify: `web/src/components/board/BoardColumn.vue`
- Modify: `web/src/components/board/TaskCard.vue`
- Modify: `web/src/styles/main.css`
- Test: `web/tests/app.spec.ts`
- Test: `web/tests/my-task-board-view.spec.ts`
- Test: `web/tests/task-card.spec.ts`
- Test: `web/tests/board-template-settings-view.spec.ts`

- [ ] **Step 1: Write failing tests**

`app.spec.ts`:

```ts
it('shows system settings link for admins', async () => {
  const wrapper = mount(App, {
    global: {
      plugins: [router, createTestingPinia({
        initialState: {
          auth: {
            token: 'token',
            user: { id: 1, account: 'admin', nickname: 'Admin', email: null, role: 'ADMIN' },
          },
        },
      })],
    },
  })
  expect(wrapper.text()).toContain('系统设置')
})
```

`task-card.spec.ts`:

```ts
it('renders project badge when project metadata is available', () => {
  const wrapper = mount(TaskCard, {
    props: {
      task: {
        id: 1,
        projectId: 10,
        projectCode: 'SD-KB',
        projectName: 'Delivery',
        projectColor: '#0f766e',
        sprintId: null,
        columnId: 11,
        columnTemplateKey: 'BACKLOG',
        assigneeId: 1,
        assignee: null,
        title: 'Build board',
        taskType: 'TASK',
        priority: 'MEDIUM',
        storyPoints: null,
        dueDate: null,
        sortOrder: 0,
        checklistDoneCount: 0,
        checklistTotalCount: 0,
      },
    },
  })
  expect(wrapper.text()).toContain('SD-KB')
  expect(wrapper.attributes('style') ?? '').not.toContain('#0f766e')
})
```

`my-task-board-view.spec.ts`:

```ts
it('moves personal task cards by target template key', async () => {
  const board = useBoardStore()
  board.myTaskBoard = {
    groupBy: 'template',
    groups: [
      { templateKey: 'BACKLOG', name: '待办（Backlog）', color: '#64748b', sortOrder: 0, isDone: false, tasks: [taskFactory({ id: 1, columnTemplateKey: 'BACKLOG' })] },
      { templateKey: 'READY', name: '就绪（Ready）', color: '#0ea5e9', sortOrder: 1, isDone: false, tasks: [] },
    ],
  }
  const moveSpy = vi.spyOn(board, 'movePersonalTask').mockResolvedValue()
  const wrapper = mount(MyTaskBoardView, { global: { plugins: [pinia] } })
  await wrapper.find('[data-template-key="READY"]').trigger('drop', {
    dataTransfer: { getData: () => '1' },
    preventDefault: vi.fn(),
  })
  expect(moveSpy).toHaveBeenCalledWith(1, 'READY', 0)
})
```

- [ ] **Step 2: Run failing tests**

```powershell
npm test -- app.spec.ts my-task-board-view.spec.ts task-card.spec.ts board-template-settings-view.spec.ts
```

Expected: FAIL because settings page and personal drop behavior are missing.

- [ ] **Step 3: Add settings route and nav**

In router:

```ts
import BoardTemplateSettingsView from '../views/BoardTemplateSettingsView.vue'

{
  path: '/admin/settings/board-template',
  name: 'board-template-settings',
  component: BoardTemplateSettingsView,
  meta: { requiresAuth: true, requiresAdmin: true },
}
```

In `App.vue` nav:

```vue
<RouterLink v-if="auth.isAdmin" to="/admin/settings/board-template">系统设置</RouterLink>
```

- [ ] **Step 4: Create settings page**

`BoardTemplateSettingsView.vue`:

```vue
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useSettingsStore } from '../stores/settings'

const settings = useSettingsStore()
const editingKey = ref<string | null>(null)
const form = reactive({
  templateKey: '',
  nameZh: '',
  nameEn: '',
  color: '#64748b',
  wipLimit: '',
  isDone: false,
})

onMounted(() => settings.loadBoardTemplates())

function edit(templateKey: string) {
  const template = settings.boardTemplates.find((candidate) => candidate.templateKey === templateKey)
  if (!template) return
  editingKey.value = template.templateKey
  form.templateKey = template.templateKey
  form.nameZh = template.nameZh
  form.nameEn = template.nameEn
  form.color = template.color
  form.wipLimit = template.wipLimit === null ? '' : String(template.wipLimit)
  form.isDone = template.isDone
}

async function submit() {
  await settings.saveBoardTemplate(editingKey.value, {
    templateKey: form.templateKey,
    nameZh: form.nameZh,
    nameEn: form.nameEn,
    color: form.color,
    wipLimit: form.wipLimit === '' ? null : Number(form.wipLimit),
    isDone: form.isDone,
  })
}
</script>

<template>
  <main class="page-surface">
    <header class="page-header">
      <div>
        <p class="eyebrow">Settings</p>
        <h1>看板模板</h1>
      </div>
    </header>
    <form class="panel-block template-form" @submit.prevent="submit">
      <label>模板键<input v-model="form.templateKey" :disabled="Boolean(editingKey)" required /></label>
      <label>中文名<input v-model="form.nameZh" required /></label>
      <label>英文名<input v-model="form.nameEn" required /></label>
      <label>颜色<input v-model="form.color" type="color" required /></label>
      <label>WIP 限制<input v-model="form.wipLimit" type="number" min="1" /></label>
      <label class="checkbox-row"><input v-model="form.isDone" type="checkbox" /> 完成列</label>
      <button class="primary-button" type="submit">保存模板列</button>
    </form>
    <section class="template-list">
      <article v-for="template in settings.boardTemplates" :key="template.templateKey" class="template-row">
        <span class="project-color-dot" :style="{ background: template.color }"></span>
        <strong>{{ template.displayName }}</strong>
        <small>{{ template.templateKey }}</small>
        <button class="secondary-button" type="button" @click="edit(template.templateKey)">编辑</button>
        <button class="danger-button" type="button" @click="settings.remove(template.templateKey)">删除</button>
      </article>
    </section>
  </main>
</template>
```

- [ ] **Step 5: Update BoardColumn for reusable personal drop**

Add props:

```ts
const props = defineProps<{
  column: BoardColumn | MyTaskBoardGroup
  showCreateButton?: boolean
}>()
```

Use template key:

```vue
<section
  class="board-column"
  :data-template-key="column.templateKey"
  @dragover="allowDrop"
  @drop="dropTask"
>
```

Emit:

```ts
const emit = defineEmits<{
  openTask: [taskId: number]
  moveTask: [taskId: number, columnId: number, sortOrder: number, templateKey: string]
  createTask: [columnId: number]
}>()

emit('moveTask', taskId, 'id' in props.column ? props.column.id : 0, props.column.tasks.length, props.column.templateKey)
```

- [ ] **Step 6: Update MyTaskBoardView**

Remove group selector and use BoardColumn:

```vue
<section class="board-lane" aria-label="个人任务看板">
  <BoardColumn
    v-for="group in board.myTaskBoard?.groups ?? []"
    :key="group.templateKey"
    :column="group"
    :show-create-button="false"
    @open-task="openTask"
    @move-task="movePersonalTask"
  />
</section>
```

Handler:

```ts
async function movePersonalTask(taskId: number, _columnId: number, sortOrder: number, templateKey: string) {
  await board.movePersonalTask(taskId, templateKey, sortOrder)
}
```

- [ ] **Step 7: Update TaskCard project badge**

Add near card title:

```vue
<span v-if="task.projectCode" class="project-badge" :style="{ '--project-color': task.projectColor }">
  {{ task.projectCode }}
</span>
```

Add CSS:

```css
.project-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  font-weight: 700;
  color: var(--color-heading);
}

.project-badge::before {
  content: "";
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--project-color);
}
```

- [ ] **Step 8: Run frontend tests**

```powershell
npm test -- app.spec.ts my-task-board-view.spec.ts task-card.spec.ts board-template-settings-view.spec.ts
```

Expected: PASS.

- [ ] **Step 9: Commit**

```powershell
git add web/src/views web/src/router/index.ts web/src/App.vue web/src/components/board web/src/styles/main.css web/tests
git commit -m "feat: add settings and personal board drag UI"
```

---

### Task 10: Full Verification, Browser Check, And Documentation

**Files:**
- Modify: `README.md`
- Modify: `README_CN.md`
- Modify tests only if full-suite failures reveal missing assertions from previous tasks.

- [ ] **Step 1: Update README files**

Add these points:

```markdown
- Default ports: backend `8101`, frontend `8102`.
- Initial administrator provisioning is handled outside the application package.
- Projects require a unique project code and project color.
- All projects use the global board template managed in System Settings.
- Personal task board shows only tasks assigned to the current user and supports dragging cards between global template statuses.
- Project owners add project members from the project detail page; tasks can only be assigned to project members.
```

- [ ] **Step 2: Run backend full suite**

```powershell
mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-DargLine=-Xmx768m -XX:MaxMetaspaceSize=256m" test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run frontend full suite**

```powershell
npm test
```

Expected: all Vitest files pass.

- [ ] **Step 4: Run frontend production build**

```powershell
npm run build
```

Expected: Vite build succeeds and writes `web/dist`.

- [ ] **Step 5: Start app for browser verification**

```powershell
Start-Process -FilePath powershell -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File','scripts\dev-backend.ps1') -WorkingDirectory 'D:\root\dev\Java\workspace\sd_kanban' -WindowStyle Hidden -RedirectStandardOutput 'backend-8101.out.log' -RedirectStandardError 'backend-8101.err.log'
Start-Process -FilePath powershell -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File','scripts\dev-frontend.ps1') -WorkingDirectory 'D:\root\dev\Java\workspace\sd_kanban' -WindowStyle Hidden -RedirectStandardOutput 'frontend-8102.out.log' -RedirectStandardError 'frontend-8102.err.log'
```

Wait until:

```powershell
Get-NetTCPConnection -LocalPort 8101,8102 -State Listen
```

shows both ports.

- [ ] **Step 6: Browser verification checklist**

Open `http://127.0.0.1:8102`.

Verify:

- Login with an administrator account created by the deployment or database bootstrap process.
- Admin can open 系统设置 -> 看板模板.
- Default template labels show `待办（Backlog）`, `就绪（Ready）`, `进行中（In Progress）`, `测试（Testing）`, `完成（Done）`.
- Create a project with code `SD-DEMO` and a color.
- Project detail shows code/color and member management.
- Add an existing active user as a project member.
- Create a task assigned to that member.
- Login as that member or use an existing member account.
- Open 我的任务 and drag the task from 待办（Backlog） to 就绪（Ready）.
- Confirm the card still displays `SD-DEMO` and the project color.
- Open the project board and confirm the same task moved to the matching project status column.

- [ ] **Step 7: Stop app after verification**

```powershell
$ports = 8101,8102
$pids = Get-NetTCPConnection -LocalPort $ports -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($pidValue in $pids) {
    Stop-Process -Id $pidValue -Force -ErrorAction SilentlyContinue
}
```

- [ ] **Step 8: Commit docs and verification updates**

```powershell
git add README.md README_CN.md
git commit -m "docs: update board template usage"
```

- [ ] **Step 9: Final status check**

```powershell
git status --short --branch
git log --oneline -5
```

Expected: branch is clean after commits.

---

## Self-Review

### Spec Coverage

- Global board template: Task 1 and Task 2.
- Template synchronization to projects: Task 2 and Task 3.
- Default Chinese/English labels: Task 1 and Task 10.
- Project code/color: Task 3 and Task 8.
- One-lane personal board: Task 4, Task 5, Task 7, Task 9.
- Personal drag preserves project binding: Task 5 and browser verification in Task 10.
- Project member management: Task 6 and Task 8.
- Task assignment limited to project members: already enforced in `TaskServiceImpl.validateAssignee`; Task 6 keeps this visible through UI.
- Admin-only system settings: Task 2 and Task 9.
- Delete template column blocked while tasks exist: Task 2.

### Placeholder Scan

This plan uses concrete file paths, request/response shapes, test names, commands, and commit messages. It intentionally avoids open-ended implementation markers.

### Type Consistency

- Backend uses `templateKey`, `projectCode`, and `projectColor`.
- Frontend mirrors these names in `Project`, `TaskCard`, `BoardColumn`, and `MyTaskBoardGroup`.
- Personal-board move API consistently uses `targetTemplateKey` and `sortOrder`.
