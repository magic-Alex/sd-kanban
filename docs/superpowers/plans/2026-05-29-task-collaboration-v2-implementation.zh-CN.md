# Task Collaboration V2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建任务协作增强 V2：检查清单、归档任务列表与恢复、可读操作历史、@成员和站内通知。

**Architecture:** 后端继续使用 Spring Boot controller/service/service.impl/repository/dto/entity 分层，新增检查清单和通知两个小领域，任务归档恢复与活动文案仍放在 task 领域内。前端继续使用 Vue 3 + Pinia + Axios，任务详情抽屉承载检查清单和动态，项目看板页增加已归档视图，应用外壳增加通知入口。

**Tech Stack:** Java 17、Spring Boot 3、Maven、MySQL、Flyway、JPA、JdbcTemplate、Vue 3、Pinia、Axios、Vitest、Vue Test Utils、Playwright。

---

## Scope Check

本计划覆盖 4 个紧密相关的协作能力：检查清单、归档恢复、活动文案、站内通知。它们共享任务详情、任务活动、任务卡片和项目成员数据，所以作为一个 V2 计划推进；实现时按垂直切片拆任务，每个任务都有独立测试和提交点。

本计划不实现 WIP 限制、泳道、任务依赖、燃尽图、累计流图或周期时间统计。

## File Structure

后端新增文件：

- Create: `src/main/resources/db/migration/V4__task_collaboration_v2.sql`
  - 新增 `task_checklist_items` 和 `notifications` 表、索引、外键。
- Create: `src/main/java/com/sdkanban/task/entity/TaskChecklistItem.java`
  - 检查清单条目实体。
- Create: `src/main/java/com/sdkanban/task/repository/TaskChecklistItemRepository.java`
  - 检查清单查询、计数和排序查询。
- Create: `src/main/java/com/sdkanban/task/dto/TaskChecklistItemResponse.java`
- Create: `src/main/java/com/sdkanban/task/dto/CreateTaskChecklistItemRequest.java`
- Create: `src/main/java/com/sdkanban/task/dto/UpdateTaskChecklistItemRequest.java`
- Create: `src/main/java/com/sdkanban/task/dto/ReorderTaskChecklistItemsRequest.java`
- Create: `src/main/java/com/sdkanban/task/service/TaskChecklistService.java`
- Create: `src/main/java/com/sdkanban/task/service/impl/TaskChecklistServiceImpl.java`
- Create: `src/main/java/com/sdkanban/task/controller/TaskChecklistController.java`
- Create: `src/main/java/com/sdkanban/notification/entity/Notification.java`
- Create: `src/main/java/com/sdkanban/notification/repository/NotificationRepository.java`
- Create: `src/main/java/com/sdkanban/notification/dto/NotificationResponse.java`
- Create: `src/main/java/com/sdkanban/notification/dto/UnreadNotificationCountResponse.java`
- Create: `src/main/java/com/sdkanban/notification/service/NotificationService.java`
- Create: `src/main/java/com/sdkanban/notification/service/impl/NotificationServiceImpl.java`
- Create: `src/main/java/com/sdkanban/notification/controller/NotificationController.java`

后端修改文件：

- Modify: `src/test/java/com/sdkanban/schema/SchemaMigrationTest.java`
  - 断言新表、新列和外键存在。
- Modify: `src/main/java/com/sdkanban/task/entity/Task.java`
  - 增加 `restore()`。
- Modify: `src/main/java/com/sdkanban/task/entity/TaskActivity.java`
  - 增加 getter，供活动响应转换使用。
- Modify: `src/main/java/com/sdkanban/task/repository/TaskRepository.java`
  - 新增归档任务查询和按 ID/项目查询。
- Modify: `src/main/java/com/sdkanban/task/repository/TaskActivityRepository.java`
  - 新增按任务查询动态。
- Modify: `src/main/java/com/sdkanban/task/repository/TaskCommentRepository.java`
  - 新增按任务查询评论。
- Modify: `src/main/java/com/sdkanban/task/dto/TaskResponse.java`
  - 保留任务主体响应，检查清单由独立接口加载。
- Create: `src/main/java/com/sdkanban/task/dto/TaskActivityResponse.java`
- Modify: `src/main/java/com/sdkanban/task/service/TaskService.java`
  - 增加归档列表、恢复、评论列表、动态列表方法。
- Modify: `src/main/java/com/sdkanban/task/service/impl/TaskServiceImpl.java`
  - 实现归档恢复、@解析、通知生成、活动文案转换。
- Modify: `src/main/java/com/sdkanban/task/controller/TaskController.java`
  - 增加归档列表、恢复、评论列表、动态列表接口。
- Modify: `src/main/java/com/sdkanban/board/dto/TaskCardResponse.java`
  - 增加 `checklistDoneCount`、`checklistTotalCount`。
- Modify: `src/main/java/com/sdkanban/board/dto/BoardColumnTasks.java`
  - 本批不改列级字段，只让列内任务卡片携带检查清单计数。
- Modify: `src/main/java/com/sdkanban/board/service/impl/BoardServiceImpl.java`
  - 批量加载检查清单计数并装配任务卡片。

后端测试文件：

- Create: `src/test/java/com/sdkanban/task/TaskChecklistControllerTest.java`
- Create: `src/test/java/com/sdkanban/notification/NotificationControllerTest.java`
- Modify: `src/test/java/com/sdkanban/task/TaskControllerTest.java`
- Modify: `src/test/java/com/sdkanban/board/BoardApiTest.java`
- Modify: `src/test/java/com/sdkanban/task/MyTaskBoardApiTest.java`

前端新增文件：

- Create: `web/src/api/checklist.ts`
- Create: `web/src/api/notifications.ts`
- Create: `web/src/stores/notifications.ts`
- Create: `web/src/components/task/TaskChecklist.vue`
- Create: `web/src/components/task/ArchivedTaskList.vue`
- Create: `web/src/components/notification/NotificationPanel.vue`
- Create: `web/tests/notifications-store.spec.ts`

前端修改文件：

- Modify: `web/src/api/tasks.ts`
  - 增加归档列表、恢复、评论列表、动态列表、活动展示文案字段。
- Modify: `web/src/api/board.ts`
  - `TaskCard` 增加检查清单计数。
- Modify: `web/src/stores/tasks.ts`
  - 加载检查清单、评论、动态；支持恢复归档任务。
- Modify: `web/src/stores/board.ts`
  - 归档恢复后刷新看板；保持现有乐观移动逻辑。
- Modify: `web/src/components/task/TaskDrawer.vue`
  - 接入检查清单、恢复按钮、中文动态文案。
- Modify: `web/src/components/board/TaskCard.vue`
  - 展示检查清单进度。
- Modify: `web/src/views/ProjectBoardView.vue`
  - 增加“当前看板 / 已归档”切换和归档列表。
- Modify: `web/src/views/MyTaskBoardView.vue`
  - 传入检查清单相关 props，保持个人看板详情可用。
- Modify: `web/src/App.vue`
  - 增加通知入口和通知面板。
- Modify: `web/src/styles/main.css`
  - 增加检查清单、归档列表、通知面板样式。

前端测试文件：

- Modify: `web/tests/tasks-store.spec.ts`
- Modify: `web/tests/board-store.spec.ts`
- Modify: `web/tests/task-drawer.spec.ts`
- Modify: `web/tests/project-board-view.spec.ts`
- Modify: `web/e2e/sd-kanban.spec.ts`

测试数据库说明：

- 后端测试命令使用 `DB_NAME=sd_kanban_test`，避免影响本机正在浏览的 `sd_kanban` 数据。
- Maven 本地仓库固定为 `D:\root\dev\Java\maven\repository`。

---

### Task 1: Schema and Persistence Foundations

**Files:**
- Create: `src/main/resources/db/migration/V4__task_collaboration_v2.sql`
- Create: `src/main/java/com/sdkanban/task/entity/TaskChecklistItem.java`
- Create: `src/main/java/com/sdkanban/task/repository/TaskChecklistItemRepository.java`
- Create: `src/main/java/com/sdkanban/notification/entity/Notification.java`
- Create: `src/main/java/com/sdkanban/notification/repository/NotificationRepository.java`
- Modify: `src/test/java/com/sdkanban/schema/SchemaMigrationTest.java`

- [ ] **Step 1: Write the failing schema assertions**

Add the new table names to `flywayCreatesCoreKanbanTables()` in `SchemaMigrationTest`:

```java
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
```

Add column assertions to `coreTablesExposeDesignSpecColumns()`:

```java
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
```

Add foreign key assertions inside `schemaHasRequiredForeignKeysAndProjectScopedIndexes()`:

```java
Map.entry("fk_task_checklist_items_task_project", "task_checklist_items.task_id,project_id->tasks.id,project_id"),
Map.entry("fk_task_checklist_items_created_by", "task_checklist_items.created_by->users.id"),
Map.entry("fk_task_checklist_items_completed_by", "task_checklist_items.completed_by->users.id"),
Map.entry("fk_notifications_recipient_id", "notifications.recipient_id->users.id"),
Map.entry("fk_notifications_actor_id", "notifications.actor_id->users.id"),
Map.entry("fk_notifications_project_id", "notifications.project_id->projects.id"),
Map.entry("fk_notifications_task_project", "notifications.task_id,project_id->tasks.id,project_id")
```

Add index expectations:

```java
assertIndexes(Map.of(
    "sprints", List.of("uk_sprints_id_project"),
    "board_columns", List.of("uk_board_columns_id_project", "uk_board_columns_project_sort"),
    "tasks", List.of("uk_tasks_id_project"),
    "task_tags", List.of("uk_task_tags_id_project", "uk_task_tags_project_name"),
    "task_checklist_items", List.of("idx_task_checklist_items_task_project"),
    "notifications", List.of("idx_notifications_recipient_read_created")
));
```

- [ ] **Step 2: Run schema test and verify failure**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=SchemaMigrationTest" test
```

Expected: FAIL because `task_checklist_items` and `notifications` do not exist.

- [ ] **Step 3: Add Flyway migration**

Create `src/main/resources/db/migration/V4__task_collaboration_v2.sql`:

```sql
CREATE TABLE IF NOT EXISTS task_checklist_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    is_done BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    completed_by BIGINT NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task_checklist_items_task_project (task_id, project_id),
    KEY idx_task_checklist_items_project_id (project_id),
    KEY idx_task_checklist_items_created_by (created_by),
    KEY idx_task_checklist_items_completed_by (completed_by),
    CONSTRAINT fk_task_checklist_items_task_project FOREIGN KEY (task_id, project_id) REFERENCES tasks (id, project_id) ON DELETE CASCADE,
    CONSTRAINT fk_task_checklist_items_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_task_checklist_items_completed_by FOREIGN KEY (completed_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    project_id BIGINT NULL,
    task_id BIGINT NULL,
    type VARCHAR(60) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    KEY idx_notifications_recipient_read_created (recipient_id, is_read, created_at),
    KEY idx_notifications_task_project (task_id, project_id),
    KEY idx_notifications_project_id (project_id),
    KEY idx_notifications_actor_id (actor_id),
    CONSTRAINT fk_notifications_recipient_id FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_actor_id FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_project_id FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_task_project FOREIGN KEY (task_id, project_id) REFERENCES tasks (id, project_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **Step 4: Add checklist entity and repository**

Create `TaskChecklistItem.java` with these fields and behavior:

```java
@Entity
@Table(name = "task_checklist_items")
public class TaskChecklistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "is_done", nullable = false)
    private boolean done;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "completed_by")
    private Long completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TaskChecklistItem() {
    }

    public TaskChecklistItem(Long taskId, Long projectId, String title, Integer sortOrder, Long createdBy) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.title = title;
        this.sortOrder = sortOrder;
        this.createdBy = createdBy;
    }

    public void rename(String title) {
        this.title = title;
    }

    public void markDone(Long userId) {
        this.done = true;
        this.completedBy = userId;
        this.completedAt = LocalDateTime.now();
    }

    public void markOpen() {
        this.done = false;
        this.completedBy = null;
        this.completedAt = null;
    }

    public void changeSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
```

Create `TaskChecklistItemRepository.java`:

```java
public interface TaskChecklistItemRepository extends JpaRepository<TaskChecklistItem, Long> {
    List<TaskChecklistItem> findByTaskIdAndProjectIdOrderBySortOrderAscIdAsc(Long taskId, Long projectId);

    Optional<TaskChecklistItem> findByIdAndTaskIdAndProjectId(Long id, Long taskId, Long projectId);

    @Query("""
        select coalesce(max(item.sortOrder), -1)
        from TaskChecklistItem item
        where item.taskId = :taskId
          and item.projectId = :projectId
        """)
    int maxSortOrder(@Param("taskId") Long taskId, @Param("projectId") Long projectId);

    @Query("""
        select item.taskId as taskId,
               sum(case when item.done = true then 1 else 0 end) as doneCount,
               count(item) as totalCount
        from TaskChecklistItem item
        where item.taskId in :taskIds
        group by item.taskId
        """)
    List<ChecklistCountView> countByTaskIds(@Param("taskIds") Collection<Long> taskIds);

    interface ChecklistCountView {
        Long getTaskId();

        Long getDoneCount();

        Long getTotalCount();
    }
}
```

- [ ] **Step 5: Add notification entity and repository**

Create `Notification.java`:

```java
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "actor_id")
    private Long actorId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(nullable = false, length = 60)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected Notification() {
    }

    public Notification(Long recipientId, Long actorId, Long projectId, Long taskId, String type, String title, String content) {
        this.recipientId = recipientId;
        this.actorId = actorId;
        this.projectId = projectId;
        this.taskId = taskId;
        this.type = type;
        this.title = title;
        this.content = content;
    }

    public void markRead() {
        if (!read) {
            this.read = true;
            this.readAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getRecipientId() { return recipientId; }
    public Long getActorId() { return actorId; }
    public Long getProjectId() { return projectId; }
    public Long getTaskId() { return taskId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
}
```

Create `NotificationRepository.java`:

```java
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientIdOrderByCreatedAtDescIdDesc(Long recipientId);

    List<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDescIdDesc(Long recipientId);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    long countByRecipientIdAndReadFalse(Long recipientId);
}
```

- [ ] **Step 6: Run schema test and verify pass**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=SchemaMigrationTest" test
```

Expected: PASS.

- [ ] **Step 7: Commit persistence foundations**

Run:

```powershell
git add src/main/resources/db/migration/V4__task_collaboration_v2.sql src/main/java/com/sdkanban/task/entity/TaskChecklistItem.java src/main/java/com/sdkanban/task/repository/TaskChecklistItemRepository.java src/main/java/com/sdkanban/notification/entity/Notification.java src/main/java/com/sdkanban/notification/repository/NotificationRepository.java src/test/java/com/sdkanban/schema/SchemaMigrationTest.java
git commit -m "feat: add task collaboration persistence"
```

Expected: commit succeeds.

---

### Task 2: Checklist Backend API

**Files:**
- Create: `src/test/java/com/sdkanban/task/TaskChecklistControllerTest.java`
- Create: `src/main/java/com/sdkanban/task/dto/TaskChecklistItemResponse.java`
- Create: `src/main/java/com/sdkanban/task/dto/CreateTaskChecklistItemRequest.java`
- Create: `src/main/java/com/sdkanban/task/dto/UpdateTaskChecklistItemRequest.java`
- Create: `src/main/java/com/sdkanban/task/dto/ReorderTaskChecklistItemsRequest.java`
- Create: `src/main/java/com/sdkanban/task/service/TaskChecklistService.java`
- Create: `src/main/java/com/sdkanban/task/service/impl/TaskChecklistServiceImpl.java`
- Create: `src/main/java/com/sdkanban/task/controller/TaskChecklistController.java`
- Modify: `src/main/java/com/sdkanban/task/repository/TaskActivityRepository.java`
- Modify: `src/main/java/com/sdkanban/task/entity/TaskActivity.java`

- [ ] **Step 1: Write failing checklist controller tests**

Create `TaskChecklistControllerTest.java` using the fixture style from `TaskControllerTest`. Include these tests:

```java
@Test
void projectMemberCanCreateAndListChecklistItems() throws Exception {
    Fixture fixture = fixtureWithOwnerAndMember();
    long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Checklist task");

    mockMvc.perform(post("/api/tasks/{taskId}/checklist", taskId)
            .header("Authorization", "Bearer " + fixture.member().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "Write API tests"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("Write API tests"))
        .andExpect(jsonPath("$.data.done").value(false))
        .andExpect(jsonPath("$.data.sortOrder").value(0));

    mockMvc.perform(get("/api/tasks/{taskId}/checklist", taskId)
            .header("Authorization", "Bearer " + fixture.member().token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].title").value("Write API tests"));
}

@Test
void projectMemberCanToggleRenameDeleteAndReorderChecklistItems() throws Exception {
    Fixture fixture = fixtureWithOwnerAndMember();
    long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Checklist task");
    long firstItemId = createChecklistItem(fixture.member().token(), taskId, "First");
    long secondItemId = createChecklistItem(fixture.member().token(), taskId, "Second");

    mockMvc.perform(patch("/api/tasks/{taskId}/checklist/{itemId}/toggle", taskId, firstItemId)
            .header("Authorization", "Bearer " + fixture.member().token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.done").value(true));

    mockMvc.perform(patch("/api/tasks/{taskId}/checklist/{itemId}", taskId, firstItemId)
            .header("Authorization", "Bearer " + fixture.member().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "First renamed"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("First renamed"));

    mockMvc.perform(patch("/api/tasks/{taskId}/checklist/reorder", taskId)
            .header("Authorization", "Bearer " + fixture.member().token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "itemIds": [%d, %d]
                }
                """.formatted(secondItemId, firstItemId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(secondItemId))
        .andExpect(jsonPath("$.data[1].id").value(firstItemId));

    mockMvc.perform(delete("/api/tasks/{taskId}/checklist/{itemId}", taskId, firstItemId)
            .header("Authorization", "Bearer " + fixture.member().token()))
        .andExpect(status().isOk());

    assertThat(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM task_checklist_items WHERE id = ?",
        Integer.class,
        firstItemId
    )).isZero();
}
```

Also add:

```java
@Test
void nonMemberCannotAccessChecklistItems() throws Exception {
    Fixture fixture = fixtureWithOwnerAndMember();
    RegisteredUser outsider = register("outsider", "Outsider");
    long taskId = createTask(fixture.member().token(), fixture.projectId(), firstColumnId(fixture.projectId()), "Private checklist");

    mockMvc.perform(get("/api/tasks/{taskId}/checklist", taskId)
            .header("Authorization", "Bearer " + outsider.token()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("PROJECT_MEMBER_REQUIRED"));
}
```

- [ ] **Step 2: Run checklist tests and verify failure**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=TaskChecklistControllerTest" test
```

Expected: FAIL because checklist controller classes do not exist.

- [ ] **Step 3: Add checklist DTOs**

Create the request and response records:

```java
public record CreateTaskChecklistItemRequest(
    @NotBlank
    @Size(max = 200)
    String title
) {
}

public record UpdateTaskChecklistItemRequest(
    @NotBlank
    @Size(max = 200)
    String title
) {
}

public record ReorderTaskChecklistItemsRequest(
    @NotEmpty
    List<Long> itemIds
) {
}
```

Create `TaskChecklistItemResponse`:

```java
public record TaskChecklistItemResponse(
    Long id,
    Long taskId,
    Long projectId,
    String title,
    boolean done,
    Integer sortOrder,
    UserSummary createdBy,
    UserSummary completedBy,
    LocalDateTime completedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TaskChecklistItemResponse from(
        TaskChecklistItem item,
        UserSummary createdBy,
        UserSummary completedBy
    ) {
        return new TaskChecklistItemResponse(
            item.getId(),
            item.getTaskId(),
            item.getProjectId(),
            item.getTitle(),
            item.isDone(),
            item.getSortOrder(),
            createdBy,
            completedBy,
            item.getCompletedAt(),
            item.getCreatedAt(),
            item.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 4: Add service interface and controller**

Create `TaskChecklistService.java`:

```java
public interface TaskChecklistService {
    List<TaskChecklistItemResponse> list(Long taskId, Long currentUserId);

    TaskChecklistItemResponse create(Long taskId, CreateTaskChecklistItemRequest request, Long currentUserId);

    TaskChecklistItemResponse update(Long taskId, Long itemId, UpdateTaskChecklistItemRequest request, Long currentUserId);

    TaskChecklistItemResponse toggle(Long taskId, Long itemId, Long currentUserId);

    List<TaskChecklistItemResponse> reorder(Long taskId, ReorderTaskChecklistItemsRequest request, Long currentUserId);

    void delete(Long taskId, Long itemId, Long currentUserId);
}
```

Create `TaskChecklistController.java`:

```java
@RestController
@RequestMapping("/api/tasks/{taskId}/checklist")
@Conditional(ProjectPersistenceAvailableCondition.class)
public class TaskChecklistController {
    private final TaskChecklistService taskChecklistService;

    public TaskChecklistController(TaskChecklistService taskChecklistService) {
        this.taskChecklistService = taskChecklistService;
    }

    @GetMapping
    ApiResponse<List<TaskChecklistItemResponse>> list(@PathVariable Long taskId, @AuthenticationPrincipal User user) {
        return ApiResponse.ok(taskChecklistService.list(taskId, currentUserId(user)));
    }

    @PostMapping
    ApiResponse<TaskChecklistItemResponse> create(
        @PathVariable Long taskId,
        @Valid @RequestBody CreateTaskChecklistItemRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskChecklistService.create(taskId, request, currentUserId(user)));
    }

    @PatchMapping("/{itemId}")
    ApiResponse<TaskChecklistItemResponse> update(
        @PathVariable Long taskId,
        @PathVariable Long itemId,
        @Valid @RequestBody UpdateTaskChecklistItemRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskChecklistService.update(taskId, itemId, request, currentUserId(user)));
    }

    @PatchMapping("/{itemId}/toggle")
    ApiResponse<TaskChecklistItemResponse> toggle(
        @PathVariable Long taskId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskChecklistService.toggle(taskId, itemId, currentUserId(user)));
    }

    @PatchMapping("/reorder")
    ApiResponse<List<TaskChecklistItemResponse>> reorder(
        @PathVariable Long taskId,
        @Valid @RequestBody ReorderTaskChecklistItemsRequest request,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(taskChecklistService.reorder(taskId, request, currentUserId(user)));
    }

    @DeleteMapping("/{itemId}")
    ApiResponse<Void> delete(
        @PathVariable Long taskId,
        @PathVariable Long itemId,
        @AuthenticationPrincipal User user
    ) {
        taskChecklistService.delete(taskId, itemId, currentUserId(user));
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

- [ ] **Step 5: Implement checklist service**

Create `TaskChecklistServiceImpl.java` with this service shape:

```java
@Service
@Conditional(ProjectPersistenceAvailableCondition.class)
public class TaskChecklistServiceImpl implements TaskChecklistService {
    private final TaskRepository taskRepository;
    private final TaskChecklistItemRepository checklistItemRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final ProjectService projectService;
    private final UserRepository userRepository;

    public TaskChecklistServiceImpl(
        TaskRepository taskRepository,
        TaskChecklistItemRepository checklistItemRepository,
        TaskActivityRepository taskActivityRepository,
        ProjectService projectService,
        UserRepository userRepository
    ) {
        this.taskRepository = taskRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.taskActivityRepository = taskActivityRepository;
        this.projectService = projectService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskChecklistItemResponse> list(Long taskId, Long currentUserId) {
        Task task = requireTask(taskId);
        projectService.requireMember(task.getProjectId(), currentUserId);
        return checklistItemRepository.findByTaskIdAndProjectIdOrderBySortOrderAscIdAsc(taskId, task.getProjectId())
            .stream()
            .map(this::toResponse)
            .toList();
    }
}
```

Implement create/update/toggle/reorder/delete with these rules:

```java
private Task requireTask(Long taskId) {
    return taskRepository.findById(taskId)
        .filter(task -> !task.isDeleted())
        .orElseThrow(() -> BusinessException.notFound("TASK_NOT_FOUND", "Task not found"));
}

private TaskChecklistItem requireItem(Task task, Long itemId) {
    return checklistItemRepository.findByIdAndTaskIdAndProjectId(itemId, task.getId(), task.getProjectId())
        .orElseThrow(() -> BusinessException.notFound("CHECKLIST_ITEM_NOT_FOUND", "Checklist item not found"));
}

private String requiredTitle(String title) {
    if (!StringUtils.hasText(title)) {
        throw BusinessException.badRequest("CHECKLIST_TITLE_REQUIRED", "Checklist title is required");
    }
    return title.trim();
}

private void recordActivity(Task task, Long actorId, String actionType, String fieldName, String oldValue, String newValue) {
    taskActivityRepository.save(new TaskActivity(task.getId(), task.getProjectId(), actorId, actionType, fieldName, oldValue, newValue));
}
```

Use action types:

- `CHECKLIST_ITEM_CREATED`
- `CHECKLIST_ITEM_UPDATED`
- `CHECKLIST_ITEM_COMPLETED`
- `CHECKLIST_ITEM_REOPENED`
- `CHECKLIST_ITEMS_REORDERED`
- `CHECKLIST_ITEM_DELETED`

- [ ] **Step 6: Add missing getters on checklist and activity entities**

Add getters used by response conversion in `TaskChecklistItem`:

```java
public Long getId() { return id; }
public Long getTaskId() { return taskId; }
public Long getProjectId() { return projectId; }
public String getTitle() { return title; }
public boolean isDone() { return done; }
public Integer getSortOrder() { return sortOrder; }
public Long getCreatedBy() { return createdBy; }
public Long getCompletedBy() { return completedBy; }
public LocalDateTime getCompletedAt() { return completedAt; }
public LocalDateTime getCreatedAt() { return createdAt; }
public LocalDateTime getUpdatedAt() { return updatedAt; }
```

Add getters to `TaskActivity`:

```java
public Long getId() { return id; }
public Long getTaskId() { return taskId; }
public Long getProjectId() { return projectId; }
public Long getActorId() { return actorId; }
public String getActionType() { return actionType; }
public String getFieldName() { return fieldName; }
public String getOldValue() { return oldValue; }
public String getNewValue() { return newValue; }
public LocalDateTime getCreatedAt() { return createdAt; }
```

- [ ] **Step 7: Run checklist backend tests**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=TaskChecklistControllerTest" test
```

Expected: PASS.

- [ ] **Step 8: Commit checklist backend**

Run:

```powershell
git add src/main/java/com/sdkanban/task src/test/java/com/sdkanban/task/TaskChecklistControllerTest.java
git commit -m "feat: add task checklist api"
```

Expected: commit succeeds.

---

### Task 3: Archived Tasks and Restore Backend

**Files:**
- Modify: `src/test/java/com/sdkanban/task/TaskControllerTest.java`
- Modify: `src/main/java/com/sdkanban/task/entity/Task.java`
- Modify: `src/main/java/com/sdkanban/task/repository/TaskRepository.java`
- Modify: `src/main/java/com/sdkanban/task/service/TaskService.java`
- Modify: `src/main/java/com/sdkanban/task/service/impl/TaskServiceImpl.java`
- Modify: `src/main/java/com/sdkanban/task/controller/TaskController.java`

- [ ] **Step 1: Write failing archived list and restore tests**

Add to `TaskControllerTest`:

```java
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
```

Add a forbidden restore test:

```java
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
```

- [ ] **Step 2: Run task controller tests and verify failure**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=TaskControllerTest" test
```

Expected: FAIL because archived list and restore endpoints do not exist.

- [ ] **Step 3: Add repository queries**

Add to `TaskRepository.java`:

```java
Optional<Task> findByIdAndProjectId(Long id, Long projectId);

@Query("""
    select task
    from Task task
    where task.projectId = :projectId
      and task.deleted = false
      and task.archived = true
      and (:assigneeId is null or (:assigneeId = 0 and task.assigneeId is null) or task.assigneeId = :assigneeId)
      and (:taskType is null or task.taskType = :taskType)
      and (:priority is null or task.priority = :priority)
      and (
          :keyword is null
          or lower(task.title) like concat('%', :keyword, '%')
          or lower(coalesce(task.description, '')) like concat('%', :keyword, '%')
      )
    order by task.updatedAt desc, task.id desc
    """)
List<Task> findArchivedTasks(
    @Param("projectId") Long projectId,
    @Param("assigneeId") Long assigneeId,
    @Param("taskType") String taskType,
    @Param("priority") String priority,
    @Param("keyword") String keyword
);
```

- [ ] **Step 4: Add task restore method**

Add to `Task.java`:

```java
public void restore() {
    this.archived = false;
}
```

Add to `TaskService.java`:

```java
List<TaskResponse> archivedTasks(Long projectId, Long assigneeId, String type, String priority, String keyword, Long currentUserId);

TaskResponse restore(Long taskId, Long currentUserId);
```

- [ ] **Step 5: Implement archived list and restore in service**

Add to `TaskServiceImpl.java`:

```java
@Override
@Transactional(readOnly = true)
public List<TaskResponse> archivedTasks(Long projectId, Long assigneeId, String type, String priority, String keyword, Long currentUserId) {
    projectService.requireMember(projectId, currentUserId);
    return taskRepository.findArchivedTasks(projectId, assigneeId, normalize(type), normalize(priority), keyword(keyword)).stream()
        .map(this::toTaskResponse)
        .toList();
}

@Override
@Transactional
public TaskResponse restore(Long taskId, Long currentUserId) {
    Task task = taskRepository.findById(taskId)
        .filter(candidate -> !candidate.isDeleted())
        .orElseThrow(() -> BusinessException.notFound("TASK_NOT_FOUND", "Task not found"));
    requireDestructiveTaskActor(task, currentUserId);
    if (boardColumnRepository.findByIdAndProjectId(task.getColumnId(), task.getProjectId()).isEmpty()) {
        Long fallbackColumnId = boardColumnRepository.findByProjectIdOrderBySortOrderAscIdAsc(task.getProjectId()).stream()
            .findFirst()
            .map(BoardColumn::getId)
            .orElseThrow(() -> BusinessException.notFound("BOARD_COLUMN_NOT_FOUND", "Board column not found"));
        task.changeColumnId(fallbackColumnId);
    }
    task.changeSortOrder(taskRepository.maxSortOrderInColumn(task.getProjectId(), task.getColumnId()) + 1);
    if (task.isArchived()) {
        task.restore();
        recordActivity(task, currentUserId, "TASK_RESTORED", null, null, null);
    }
    return toTaskResponse(task);
}
```

- [ ] **Step 6: Add controller endpoints**

Add to `TaskController.java`:

```java
@GetMapping("/projects/{projectId}/tasks/archived")
ApiResponse<List<TaskResponse>> archivedTasks(
    @PathVariable Long projectId,
    @RequestParam(required = false) Long assigneeId,
    @RequestParam(required = false, name = "type") String type,
    @RequestParam(required = false) String priority,
    @RequestParam(required = false) String keyword,
    @AuthenticationPrincipal User user
) {
    return ApiResponse.ok(taskService.archivedTasks(projectId, assigneeId, type, priority, keyword, currentUserId(user)));
}

@PatchMapping("/tasks/{taskId}/restore")
ApiResponse<TaskResponse> restore(
    @PathVariable Long taskId,
    @AuthenticationPrincipal User user
) {
    return ApiResponse.ok(taskService.restore(taskId, currentUserId(user)));
}
```

- [ ] **Step 7: Run restore tests**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=TaskControllerTest" test
```

Expected: PASS.

- [ ] **Step 8: Commit archived restore backend**

Run:

```powershell
git add src/main/java/com/sdkanban/task src/test/java/com/sdkanban/task/TaskControllerTest.java
git commit -m "feat: add archived task restore api"
```

Expected: commit succeeds.

---

### Task 4: Notification Backend and Mention Parsing

**Files:**
- Create: `src/test/java/com/sdkanban/notification/NotificationControllerTest.java`
- Modify: `src/test/java/com/sdkanban/task/TaskControllerTest.java`
- Create: `src/main/java/com/sdkanban/notification/dto/NotificationResponse.java`
- Create: `src/main/java/com/sdkanban/notification/dto/UnreadNotificationCountResponse.java`
- Create: `src/main/java/com/sdkanban/notification/service/NotificationService.java`
- Create: `src/main/java/com/sdkanban/notification/service/impl/NotificationServiceImpl.java`
- Create: `src/main/java/com/sdkanban/notification/controller/NotificationController.java`
- Modify: `src/main/java/com/sdkanban/task/service/impl/TaskServiceImpl.java`

- [ ] **Step 1: Write notification controller tests**

Create `NotificationControllerTest.java`:

```java
@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {
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
    void userCanListAndReadOwnNotifications() throws Exception {
        RegisteredUser owner = register("owner", "Owner");
        long projectId = createProject(owner.token(), "Delivery", "Delivery board");
        long taskId = createTask(owner.token(), projectId, firstColumnId(projectId), "Notify me");
        jdbcTemplate.update(
            """
            INSERT INTO notifications (recipient_id, actor_id, project_id, task_id, type, title, content)
            VALUES (?, ?, ?, ?, 'MENTION', 'New mention', 'Owner mentioned you')
            """,
            owner.id(), owner.id(), projectId, taskId
        );

        mockMvc.perform(get("/api/notifications/unread-count")
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.count").value(1));

        String response = mockMvc.perform(get("/api/notifications?status=unread")
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].type").value("MENTION"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        long notificationId = objectMapper.readTree(response).path("data").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", notificationId)
                .header("Authorization", "Bearer " + owner.token()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.read").value(true));
    }
}
```

- [ ] **Step 2: Write task notification tests**

Add to `TaskControllerTest`:

```java
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
```

- [ ] **Step 3: Run notification tests and verify failure**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=NotificationControllerTest,TaskControllerTest" test
```

Expected: FAIL because notification service/controller and task hooks are not implemented.

- [ ] **Step 4: Add notification DTOs and service**

Create `NotificationResponse.java`:

```java
public record NotificationResponse(
    Long id,
    UserSummary actor,
    Long projectId,
    Long taskId,
    String type,
    String title,
    String content,
    boolean read,
    LocalDateTime createdAt,
    LocalDateTime readAt
) {
    public static NotificationResponse from(Notification notification, UserSummary actor) {
        return new NotificationResponse(
            notification.getId(),
            actor,
            notification.getProjectId(),
            notification.getTaskId(),
            notification.getType(),
            notification.getTitle(),
            notification.getContent(),
            notification.isRead(),
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }
}
```

Create `UnreadNotificationCountResponse.java`:

```java
public record UnreadNotificationCountResponse(long count) {
}
```

Create `NotificationService.java`:

```java
public interface NotificationService {
    List<NotificationResponse> list(String status, Long currentUserId);

    UnreadNotificationCountResponse unreadCount(Long currentUserId);

    NotificationResponse markRead(Long notificationId, Long currentUserId);

    void markAllRead(Long currentUserId);

    void notifyUsers(Collection<Long> recipientIds, Long actorId, Long projectId, Long taskId, String type, String title, String content);
}
```

- [ ] **Step 5: Implement notification service and controller**

`NotificationServiceImpl.notifyUsers` must de-duplicate recipients and exclude `actorId`:

```java
Set<Long> recipients = recipientIds.stream()
    .filter(Objects::nonNull)
    .filter(recipientId -> !Objects.equals(recipientId, actorId))
    .collect(Collectors.toCollection(LinkedHashSet::new));
notificationRepository.saveAll(recipients.stream()
    .map(recipientId -> new Notification(recipientId, actorId, projectId, taskId, type, title, content))
    .toList());
```

Create `NotificationController.java`:

```java
@RestController
@RequestMapping("/api/notifications")
@Conditional(ProjectPersistenceAvailableCondition.class)
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    ApiResponse<List<NotificationResponse>> list(
        @RequestParam(defaultValue = "all") String status,
        @AuthenticationPrincipal User user
    ) {
        return ApiResponse.ok(notificationService.list(status, currentUserId(user)));
    }

    @GetMapping("/unread-count")
    ApiResponse<UnreadNotificationCountResponse> unreadCount(@AuthenticationPrincipal User user) {
        return ApiResponse.ok(notificationService.unreadCount(currentUserId(user)));
    }

    @PatchMapping("/{notificationId}/read")
    ApiResponse<NotificationResponse> markRead(@PathVariable Long notificationId, @AuthenticationPrincipal User user) {
        return ApiResponse.ok(notificationService.markRead(notificationId, currentUserId(user)));
    }

    @PatchMapping("/read-all")
    ApiResponse<Void> markAllRead(@AuthenticationPrincipal User user) {
        notificationService.markAllRead(currentUserId(user));
        return ApiResponse.ok(null);
    }
}
```

- [ ] **Step 6: Wire notification hooks into task service**

Inject `NotificationService` and add these helpers to `TaskServiceImpl`:

```java
private void notifyAssignee(Task task, Long actorId, Long assigneeId) {
    notificationService.notifyUsers(
        List.of(assigneeId),
        actorId,
        task.getProjectId(),
        task.getId(),
        "TASK_ASSIGNED",
        "任务分配给你",
        "任务「" + task.getTitle() + "」已分配给你"
    );
}

private void notifyTaskStakeholders(Task task, Long actorId, String type, String title, String content) {
    notificationService.notifyUsers(
        Stream.of(task.getAssigneeId(), task.getCreatorId()).filter(Objects::nonNull).toList(),
        actorId,
        task.getProjectId(),
        task.getId(),
        type,
        title,
        content
    );
}
```

In `addComment`, after saving comment, call mention parsing:

```java
private Set<Long> mentionedMemberIds(Long projectId, String content) {
    Set<String> nicknames = Pattern.compile("@([\\p{L}\\p{N}_\\-\\u4e00-\\u9fa5]+)")
        .matcher(content)
        .results()
        .map(match -> match.group(1))
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (nicknames.isEmpty()) {
        return Set.of();
    }
    return projectMemberRepository.findByIdProjectIdOrderByCreatedAtAsc(projectId).stream()
        .map(ProjectMember::getUserId)
        .map(userRepository::findById)
        .flatMap(Optional::stream)
        .filter(user -> nicknames.contains(user.getNickname()))
        .map(User::getId)
        .collect(Collectors.toCollection(LinkedHashSet::new));
}
```

Generate `MENTION` notifications:

```java
notificationService.notifyUsers(
    mentionedMemberIds(task.getProjectId(), comment.getContent()),
    currentUserId,
    task.getProjectId(),
    task.getId(),
    "MENTION",
    "有人在评论中提到了你",
    "任务「" + task.getTitle() + "」的评论提到了你"
);
```

When assignee changes from a different value to a non-null new value, call `notifyAssignee`.

When archive and restore succeed, call `notifyTaskStakeholders` with `TASK_ARCHIVED` or `TASK_RESTORED`.

- [ ] **Step 7: Run notification backend tests**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=NotificationControllerTest,TaskControllerTest" test
```

Expected: PASS.

- [ ] **Step 8: Commit notifications backend**

Run:

```powershell
git add src/main/java/com/sdkanban/notification src/main/java/com/sdkanban/task src/test/java/com/sdkanban/notification/NotificationControllerTest.java src/test/java/com/sdkanban/task/TaskControllerTest.java
git commit -m "feat: add task notifications"
```

Expected: commit succeeds.

---

### Task 5: Activity Responses and Board Checklist Counts

**Files:**
- Modify: `src/test/java/com/sdkanban/board/BoardApiTest.java`
- Modify: `src/test/java/com/sdkanban/task/MyTaskBoardApiTest.java`
- Modify: `src/test/java/com/sdkanban/task/TaskControllerTest.java`
- Create: `src/main/java/com/sdkanban/task/dto/TaskActivityResponse.java`
- Modify: `src/main/java/com/sdkanban/task/repository/TaskActivityRepository.java`
- Modify: `src/main/java/com/sdkanban/task/repository/TaskCommentRepository.java`
- Modify: `src/main/java/com/sdkanban/task/service/TaskService.java`
- Modify: `src/main/java/com/sdkanban/task/service/impl/TaskServiceImpl.java`
- Modify: `src/main/java/com/sdkanban/task/controller/TaskController.java`
- Modify: `src/main/java/com/sdkanban/board/dto/TaskCardResponse.java`
- Modify: `src/main/java/com/sdkanban/board/service/impl/BoardServiceImpl.java`

- [ ] **Step 1: Write failing board checklist count tests**

Add to `BoardApiTest`:

```java
@Test
void projectBoardCardsIncludeChecklistProgress() throws Exception {
    Fixture fixture = fixtureWithProject();
    long taskId = createTask(fixture.ownerToken(), fixture.projectId(), firstColumnId(fixture.projectId()), "Checklist card");
    createChecklistItem(fixture.ownerToken(), taskId, "Done item");
    long openItemId = createChecklistItem(fixture.ownerToken(), taskId, "Open item");

    long doneItemId = jdbcTemplate.queryForObject(
        "SELECT id FROM task_checklist_items WHERE task_id = ? AND title = 'Done item'",
        Long.class,
        taskId
    );
    mockMvc.perform(patch("/api/tasks/{taskId}/checklist/{itemId}/toggle", taskId, doneItemId)
            .header("Authorization", "Bearer " + fixture.ownerToken()))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/projects/{projectId}/board", fixture.projectId())
            .header("Authorization", "Bearer " + fixture.ownerToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.columns[0].tasks[0].checklistDoneCount").value(1))
        .andExpect(jsonPath("$.data.columns[0].tasks[0].checklistTotalCount").value(2));
}
```

Add matching assertion to `MyTaskBoardApiTest` for `/api/tasks/mine/board`.

- [ ] **Step 2: Write failing comments and activities endpoint test**

Add to `TaskControllerTest`:

```java
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
        .andExpect(jsonPath("$.data[0].displayText").exists());
}
```

- [ ] **Step 3: Run focused backend tests and verify failure**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=BoardApiTest,MyTaskBoardApiTest,TaskControllerTest" test
```

Expected: FAIL because checklist counts and activity endpoints are missing.

- [ ] **Step 4: Add repository methods**

Add to `TaskActivityRepository.java`:

```java
List<TaskActivity> findByTaskIdAndProjectIdOrderByCreatedAtDescIdDesc(Long taskId, Long projectId);
```

Add to `TaskCommentRepository.java`:

```java
List<TaskComment> findByTaskIdOrderByCreatedAtDescIdDesc(Long taskId);
```

- [ ] **Step 5: Add activity response**

Create `TaskActivityResponse.java`:

```java
public record TaskActivityResponse(
    Long id,
    Long taskId,
    UserSummary actor,
    String actionType,
    String fieldName,
    String oldValue,
    String newValue,
    String displayText,
    LocalDateTime createdAt
) {
}
```

In `TaskService.java`, add:

```java
List<TaskCommentResponse> comments(Long taskId, Long currentUserId);

List<TaskActivityResponse> activities(Long taskId, Long currentUserId);
```

- [ ] **Step 6: Implement comments and activity display text**

In `TaskServiceImpl.activities`, convert action types:

```java
private String displayText(TaskActivity activity, UserSummary actor) {
    String actorName = actor == null ? "系统" : actor.nickname();
    return switch (activity.getActionType()) {
        case "TASK_CREATED" -> actorName + " 创建了任务";
        case "TASK_UPDATED" -> actorName + " 更新了 " + displayField(activity.getFieldName()) + "：" + valueText(activity.getOldValue()) + " -> " + valueText(activity.getNewValue());
        case "COMMENT_ADDED" -> actorName + " 评论了任务";
        case "TASK_ARCHIVED" -> actorName + " 归档了任务";
        case "TASK_RESTORED" -> actorName + " 恢复了任务";
        case "CHECKLIST_ITEM_CREATED" -> actorName + " 添加了检查项：" + valueText(activity.getNewValue());
        case "CHECKLIST_ITEM_COMPLETED" -> actorName + " 完成了检查项：" + valueText(activity.getNewValue());
        case "CHECKLIST_ITEM_REOPENED" -> actorName + " 重新打开了检查项：" + valueText(activity.getNewValue());
        case "CHECKLIST_ITEM_DELETED" -> actorName + " 删除了检查项：" + valueText(activity.getOldValue());
        default -> actorName + " 更新了任务";
    };
}
```

Use these field labels:

```java
private String displayField(String fieldName) {
    return switch (String.valueOf(fieldName)) {
        case "title" -> "标题";
        case "description" -> "描述";
        case "taskType" -> "类型";
        case "priority" -> "优先级";
        case "assigneeId" -> "负责人";
        case "columnId" -> "看板列";
        case "dueDate" -> "截止日期";
        case "storyPoints" -> "故事点";
        case "estimatedHours" -> "预计工时";
        case "acceptanceCriteria" -> "验收标准";
        default -> String.valueOf(fieldName);
    };
}
```

- [ ] **Step 7: Add controller endpoints**

Add to `TaskController.java`:

```java
@GetMapping("/tasks/{taskId}/comments")
ApiResponse<List<TaskCommentResponse>> comments(@PathVariable Long taskId, @AuthenticationPrincipal User user) {
    return ApiResponse.ok(taskService.comments(taskId, currentUserId(user)));
}

@GetMapping("/tasks/{taskId}/activities")
ApiResponse<List<TaskActivityResponse>> activities(@PathVariable Long taskId, @AuthenticationPrincipal User user) {
    return ApiResponse.ok(taskService.activities(taskId, currentUserId(user)));
}
```

- [ ] **Step 8: Add checklist counts to task cards**

Modify `TaskCardResponse`:

```java
public record TaskCardResponse(
    Long id,
    Long projectId,
    Long sprintId,
    Long columnId,
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
}
```

In `BoardServiceImpl.cards`, build a count map:

```java
Map<Long, ChecklistCountView> checklistCounts = taskChecklistItemRepository.countByTaskIds(
    tasks.stream().map(Task::getId).toList()
).stream().collect(Collectors.toMap(ChecklistCountView::getTaskId, Function.identity()));
```

Pass counts into `TaskCardResponse.from(task, assignee, doneCount, totalCount)`.

- [ ] **Step 9: Run backend board/activity tests**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=BoardApiTest,MyTaskBoardApiTest,TaskControllerTest" test
```

Expected: PASS.

- [ ] **Step 10: Commit activity and card counts**

Run:

```powershell
git add src/main/java/com/sdkanban/task src/main/java/com/sdkanban/board src/test/java/com/sdkanban/task src/test/java/com/sdkanban/board/BoardApiTest.java
git commit -m "feat: expose task collaboration details"
```

Expected: commit succeeds.

---

### Task 6: Frontend API and Stores

**Files:**
- Create: `web/src/api/checklist.ts`
- Create: `web/src/api/notifications.ts`
- Create: `web/src/stores/notifications.ts`
- Modify: `web/src/api/tasks.ts`
- Modify: `web/src/api/board.ts`
- Modify: `web/src/stores/tasks.ts`
- Modify: `web/src/stores/board.ts`
- Modify: `web/tests/tasks-store.spec.ts`
- Create: `web/tests/notifications-store.spec.ts`

- [ ] **Step 1: Write failing store tests**

Add to `web/tests/tasks-store.spec.ts`:

```ts
it('loads task detail side data when opening a task', async () => {
  vi.mocked(fetchTask).mockResolvedValue(taskA)
  vi.mocked(fetchTaskComments).mockResolvedValue([{ id: 1, taskId: taskA.id, author: taskA.creator, content: 'Hello', createdAt: '2026-05-29T10:00:00', updatedAt: '2026-05-29T10:00:00' }])
  vi.mocked(fetchTaskActivities).mockResolvedValue([{ id: 2, taskId: taskA.id, actor: taskA.creator, actionType: 'TASK_CREATED', fieldName: null, oldValue: null, newValue: null, displayText: 'Alex 创建了任务', createdAt: '2026-05-29T10:00:01' }])
  vi.mocked(fetchChecklistItems).mockResolvedValue([{ id: 3, taskId: taskA.id, projectId: taskA.projectId, title: 'Check API', done: false, sortOrder: 0, createdBy: taskA.creator, completedBy: null, completedAt: null, createdAt: '2026-05-29T10:00:02', updatedAt: '2026-05-29T10:00:02' }])

  const tasks = useTasksStore()
  await tasks.openTask(taskA.id)

  expect(tasks.activeTask).toEqual(taskA)
  expect(tasks.comments).toHaveLength(1)
  expect(tasks.activities[0].displayText).toBe('Alex 创建了任务')
  expect(tasks.checklistItems[0].title).toBe('Check API')
})

it('restores the active archived task and closes the drawer', async () => {
  vi.mocked(restoreTask).mockResolvedValue(taskA)
  const tasks = useTasksStore()
  tasks.activeTask = taskA
  tasks.drawerOpen = true

  await tasks.restoreActiveTask()

  expect(restoreTask).toHaveBeenCalledWith(taskA.id)
  expect(tasks.drawerOpen).toBe(false)
})
```

Create `web/tests/notifications-store.spec.ts`:

```ts
describe('notifications store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(fetchNotifications).mockReset()
    vi.mocked(fetchUnreadNotificationCount).mockReset()
    vi.mocked(markNotificationRead).mockReset()
    vi.mocked(markAllNotificationsRead).mockReset()
  })

  it('loads notifications and unread count', async () => {
    vi.mocked(fetchNotifications).mockResolvedValue([{ id: 1, actor: null, projectId: 7, taskId: 12, type: 'MENTION', title: '有人提到了你', content: '任务评论提到了你', read: false, createdAt: '2026-05-29T10:00:00', readAt: null }])
    vi.mocked(fetchUnreadNotificationCount).mockResolvedValue({ count: 1 })

    const notifications = useNotificationsStore()
    await notifications.load()
    await notifications.loadUnreadCount()

    expect(notifications.items).toHaveLength(1)
    expect(notifications.unreadCount).toBe(1)
  })
})
```

- [ ] **Step 2: Run frontend store tests and verify failure**

Run:

```powershell
npm test -- tasks-store.spec.ts notifications-store.spec.ts
```

Working directory: `web`

Expected: FAIL because new API functions and notification store do not exist.

- [ ] **Step 3: Add checklist API**

Create `web/src/api/checklist.ts`:

```ts
import type { UserSummary } from './auth'
import { getData, http, postData } from './http'

export interface TaskChecklistItem {
  id: number
  taskId: number
  projectId: number
  title: string
  done: boolean
  sortOrder: number
  createdBy: UserSummary
  completedBy: UserSummary | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
}

export function fetchChecklistItems(taskId: number): Promise<TaskChecklistItem[]> {
  return getData<TaskChecklistItem[]>(`/tasks/${taskId}/checklist`)
}

export function createChecklistItem(taskId: number, title: string): Promise<TaskChecklistItem> {
  return postData<TaskChecklistItem, { title: string }>(`/tasks/${taskId}/checklist`, { title })
}

export async function updateChecklistItem(taskId: number, itemId: number, title: string): Promise<TaskChecklistItem> {
  const response = await http.patch(`/tasks/${taskId}/checklist/${itemId}`, { title })
  return response.data.data
}

export async function toggleChecklistItem(taskId: number, itemId: number): Promise<TaskChecklistItem> {
  const response = await http.patch(`/tasks/${taskId}/checklist/${itemId}/toggle`)
  return response.data.data
}

export async function reorderChecklistItems(taskId: number, itemIds: number[]): Promise<TaskChecklistItem[]> {
  const response = await http.patch(`/tasks/${taskId}/checklist/reorder`, { itemIds })
  return response.data.data
}

export async function deleteChecklistItem(taskId: number, itemId: number): Promise<void> {
  await http.delete(`/tasks/${taskId}/checklist/${itemId}`)
}
```

- [ ] **Step 4: Add notification API and store**

Create `web/src/api/notifications.ts`:

```ts
import type { UserSummary } from './auth'
import { getData, http } from './http'

export interface NotificationItem {
  id: number
  actor: UserSummary | null
  projectId: number | null
  taskId: number | null
  type: string
  title: string
  content: string
  read: boolean
  createdAt: string
  readAt: string | null
}

export interface UnreadNotificationCount {
  count: number
}

export function fetchNotifications(status: 'all' | 'unread' = 'all'): Promise<NotificationItem[]> {
  return getData<NotificationItem[]>(`/notifications?status=${encodeURIComponent(status)}`)
}

export function fetchUnreadNotificationCount(): Promise<UnreadNotificationCount> {
  return getData<UnreadNotificationCount>('/notifications/unread-count')
}

export async function markNotificationRead(notificationId: number): Promise<NotificationItem> {
  const response = await http.patch(`/notifications/${notificationId}/read`)
  return response.data.data
}

export async function markAllNotificationsRead(): Promise<void> {
  await http.patch('/notifications/read-all')
}
```

Create `web/src/stores/notifications.ts`:

```ts
export const useNotificationsStore = defineStore('notifications', {
  state: () => ({
    items: [] as NotificationItem[],
    unreadCount: 0,
    loading: false,
    error: null as string | null,
  }),
  actions: {
    async load(status: 'all' | 'unread' = 'all') {
      this.loading = true
      this.error = null
      try {
        this.items = await fetchNotifications(status)
      } catch (error) {
        this.error = '通知加载失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    async loadUnreadCount() {
      const result = await fetchUnreadNotificationCount()
      this.unreadCount = result.count
    },
  },
})
```

- [ ] **Step 5: Extend tasks API and board types**

Add to `web/src/api/tasks.ts`:

```ts
export interface TaskActivity {
  id: number
  taskId: number
  actor: UserSummary | null
  actionType: string
  fieldName: string | null
  oldValue: string | null
  newValue: string | null
  displayText: string
  createdAt: string
}

export function fetchTaskComments(taskId: number): Promise<TaskComment[]> {
  return getData<TaskComment[]>(`/tasks/${taskId}/comments`)
}

export function fetchTaskActivities(taskId: number): Promise<TaskActivity[]> {
  return getData<TaskActivity[]>(`/tasks/${taskId}/activities`)
}

export function fetchArchivedTasks(projectId: number | string, filters: BoardQuery = {}): Promise<TaskResponse[]> {
  return getData<TaskResponse[]>(`/projects/${projectId}/tasks/archived${queryString(filters)}`)
}

export async function restoreTask(taskId: number): Promise<TaskResponse> {
  const response = await http.patch(`/tasks/${taskId}/restore`)
  return response.data.data
}
```

If `queryString` is local to `board.ts`, duplicate the small helper in `tasks.ts` to keep APIs independent.

Extend `TaskCard` in `web/src/api/board.ts`:

```ts
checklistDoneCount: number
checklistTotalCount: number
```

- [ ] **Step 6: Extend tasks store**

In `web/src/stores/tasks.ts`, add state:

```ts
checklistItems: [] as TaskChecklistItem[],
```

In `openTask`, after `fetchTask(taskId)` succeeds, load side data:

```ts
const [comments, activities, checklistItems] = await Promise.all([
  fetchTaskComments(taskId),
  fetchTaskActivities(taskId),
  fetchChecklistItems(taskId),
])
if (this.drawerOpen && this.openTaskRequestId === requestId) {
  this.activeTask = task
  this.comments = comments
  this.activities = activities
  this.checklistItems = checklistItems
}
```

Add actions:

```ts
async restoreActiveTask() {
  if (!this.activeTask) {
    return
  }
  const taskId = this.activeTask.id
  this.actionLoading = true
  this.actionError = null
  try {
    await restoreTask(taskId)
    if (this.isCurrentActionTask(taskId)) {
      this.actionLoading = false
      this.closeDrawer()
    }
  } catch (error) {
    if (!this.isCurrentActionTask(taskId)) {
      throw error
    }
    this.actionError = '任务恢复失败，请重试'
    this.actionLoading = false
    throw error
  }
}
```

Add checklist actions: `addChecklistItem`, `renameChecklistItem`, `toggleChecklistItem`, `removeChecklistItem`, `moveChecklistItem`.

- [ ] **Step 7: Run frontend store tests**

Run:

```powershell
npm test -- tasks-store.spec.ts notifications-store.spec.ts
```

Working directory: `web`

Expected: PASS.

- [ ] **Step 8: Commit frontend API and stores**

Run:

```powershell
git add web/src/api web/src/stores web/tests/tasks-store.spec.ts web/tests/notifications-store.spec.ts
git commit -m "feat: add collaboration frontend stores"
```

Expected: commit succeeds.

---

### Task 7: Task Drawer Checklist and Activity UI

**Files:**
- Create: `web/src/components/task/TaskChecklist.vue`
- Modify: `web/src/components/task/TaskDrawer.vue`
- Modify: `web/src/components/board/TaskCard.vue`
- Modify: `web/tests/task-drawer.spec.ts`

- [ ] **Step 1: Write failing drawer tests**

Add to `web/tests/task-drawer.spec.ts`:

```ts
it('renders and updates checklist items', async () => {
  const addChecklistItem = vi.fn()
  const toggleChecklistItem = vi.fn()
  mount(TaskDrawer, {
    attachTo: document.body,
    props: drawerProps({
      checklistItems: [
        { id: 1, taskId: 12, projectId: 7, title: 'Write tests', done: true, sortOrder: 0, createdBy: userFixture(), completedBy: userFixture(), completedAt: '2026-05-29T10:00:00', createdAt: '2026-05-29T09:00:00', updatedAt: '2026-05-29T10:00:00' },
        { id: 2, taskId: 12, projectId: 7, title: 'Build UI', done: false, sortOrder: 1, createdBy: userFixture(), completedBy: null, completedAt: null, createdAt: '2026-05-29T09:10:00', updatedAt: '2026-05-29T09:10:00' },
      ],
      addChecklistItem,
      toggleChecklistItem,
    }),
  })

  expect(document.body.textContent).toContain('检查清单 1/2')
  expect(document.body.textContent).toContain('Write tests')
  ;(document.body.querySelector('[aria-label="切换检查项 Build UI"]') as HTMLInputElement).click()
  await flushPromises()

  expect(toggleChecklistItem).toHaveBeenCalledWith(2)
})
```

Add an activity display assertion:

```ts
expect(document.body.textContent).toContain('Alex 创建了任务')
expect(document.body.textContent).not.toContain('TASK_CREATED')
```

- [ ] **Step 2: Run drawer test and verify failure**

Run:

```powershell
npm test -- task-drawer.spec.ts
```

Working directory: `web`

Expected: FAIL because checklist props and component do not exist.

- [ ] **Step 3: Create TaskChecklist component**

Create `TaskChecklist.vue`:

```vue
<script setup lang="ts">
import { computed, ref } from 'vue'
import type { TaskChecklistItem } from '../../api/checklist'

const props = defineProps<{
  items: TaskChecklistItem[]
  actionLoading?: boolean
  addItem: (title: string) => Promise<void> | void
  toggleItem: (itemId: number) => Promise<void> | void
  renameItem: (itemId: number, title: string) => Promise<void> | void
  deleteItem: (itemId: number) => Promise<void> | void
}>()

const draftTitle = ref('')
const doneCount = computed(() => props.items.filter((item) => item.done).length)

async function submitItem() {
  const title = draftTitle.value.trim()
  if (!title) {
    return
  }
  await props.addItem(title)
  draftTitle.value = ''
}
</script>

<template>
  <section class="drawer-section checklist-section">
    <div class="section-title-row">
      <h2>检查清单 {{ doneCount }}/{{ items.length }}</h2>
    </div>
    <form class="checklist-form" @submit.prevent="submitItem">
      <input v-model="draftTitle" aria-label="新增检查项标题" placeholder="新增检查项" />
      <button class="secondary-button" type="submit" :disabled="actionLoading || !draftTitle.trim()">添加</button>
    </form>
    <ul class="checklist-list">
      <li v-for="item in items" :key="item.id" class="checklist-item">
        <label>
          <input
            type="checkbox"
            :checked="item.done"
            :aria-label="`切换检查项 ${item.title}`"
            :disabled="actionLoading"
            @change="toggleItem(item.id)"
          />
          <span :class="{ done: item.done }">{{ item.title }}</span>
        </label>
        <button class="icon-text-button" type="button" :disabled="actionLoading" @click="deleteItem(item.id)">删除</button>
      </li>
    </ul>
  </section>
</template>
```

- [ ] **Step 4: Wire TaskDrawer props and rendering**

Add props to `TaskDrawer.vue`:

```ts
checklistItems: TaskChecklistItem[]
addChecklistItem: (title: string) => Promise<void> | void
toggleChecklistItem: (itemId: number) => Promise<void> | void
renameChecklistItem: (itemId: number, title: string) => Promise<void> | void
deleteChecklistItem: (itemId: number) => Promise<void> | void
restoreTask?: () => Promise<void> | void
archived?: boolean
```

Render under task information:

```vue
<TaskChecklist
  :items="checklistItems"
  :action-loading="actionLoading"
  :add-item="addChecklistItem"
  :toggle-item="toggleChecklistItem"
  :rename-item="renameChecklistItem"
  :delete-item="deleteChecklistItem"
/>
```

Change activity list body:

```vue
<p>{{ activity.displayText }}</p>
```

- [ ] **Step 5: Show card progress**

In `TaskCard.vue`, add:

```vue
<span v-if="task.checklistTotalCount > 0" class="task-meta-pill">
  清单 {{ task.checklistDoneCount }}/{{ task.checklistTotalCount }}
</span>
```

- [ ] **Step 6: Run drawer tests**

Run:

```powershell
npm test -- task-drawer.spec.ts
```

Working directory: `web`

Expected: PASS.

- [ ] **Step 7: Commit drawer checklist UI**

Run:

```powershell
git add web/src/components/task/TaskChecklist.vue web/src/components/task/TaskDrawer.vue web/src/components/board/TaskCard.vue web/tests/task-drawer.spec.ts
git commit -m "feat: add checklist task drawer ui"
```

Expected: commit succeeds.

---

### Task 8: Archived Task View and Restore UI

**Files:**
- Create: `web/src/components/task/ArchivedTaskList.vue`
- Modify: `web/src/views/ProjectBoardView.vue`
- Modify: `web/src/stores/tasks.ts`
- Modify: `web/tests/project-board-view.spec.ts`

- [ ] **Step 1: Write failing project board archived view tests**

Add to `project-board-view.spec.ts`:

```ts
it('switches to archived tasks and restores one task', async () => {
  vi.mocked(fetchArchivedTasks).mockResolvedValue([{ ...createdTask, id: 77, title: 'Archived task' }])
  vi.mocked(restoreTask).mockResolvedValue({ ...createdTask, id: 77, title: 'Archived task' })
  const wrapper = mount(ProjectBoardView, { attachTo: document.body })
  await flushPromises()

  await wrapper.get('[aria-label="查看已归档任务"]').trigger('click')
  await flushPromises()

  expect(fetchArchivedTasks).toHaveBeenCalledWith('7', {})
  expect(document.body.textContent).toContain('Archived task')

  await wrapper.get('[aria-label="恢复任务 Archived task"]').trigger('click')
  await flushPromises()

  expect(restoreTask).toHaveBeenCalledWith(77)
  expect(fetchProjectBoard).toHaveBeenCalled()
})
```

- [ ] **Step 2: Run project board test and verify failure**

Run:

```powershell
npm test -- project-board-view.spec.ts
```

Working directory: `web`

Expected: FAIL because archived view is not implemented.

- [ ] **Step 3: Create archived task list component**

Create `ArchivedTaskList.vue`:

```vue
<script setup lang="ts">
import type { ProjectMember } from '../../api/projects'
import type { TaskResponse } from '../../api/tasks'

defineProps<{
  tasks: TaskResponse[]
  members: ProjectMember[]
  loading?: boolean
  error?: string | null
}>()

const emit = defineEmits<{
  openTask: [taskId: number]
  restoreTask: [taskId: number]
  applyFilters: [filters: { keyword?: string; assigneeId?: string; type?: string; priority?: string }]
}>()
</script>

<template>
  <section class="archived-task-panel" aria-label="已归档任务">
    <form class="board-filters" @submit.prevent="emit('applyFilters', {})">
      <button class="secondary-button" type="submit">刷新</button>
    </form>
    <p v-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="loading" class="muted">正在加载已归档任务...</p>
    <ul class="archived-task-list">
      <li v-for="task in tasks" :key="task.id">
        <button type="button" class="link-button" @click="emit('openTask', task.id)">{{ task.title }}</button>
        <span>{{ task.priority }}</span>
        <span>{{ task.assignee?.nickname ?? '未分配' }}</span>
        <button class="secondary-button" type="button" :aria-label="`恢复任务 ${task.title}`" @click="emit('restoreTask', task.id)">
          恢复
        </button>
      </li>
    </ul>
  </section>
</template>
```

- [ ] **Step 4: Wire ProjectBoardView mode switching**

In `ProjectBoardView.vue`, add state:

```ts
const boardMode = ref<'board' | 'archived'>('board')
const archivedTasks = ref<TaskResponse[]>([])
const archivedLoading = ref(false)
const archivedError = ref<string | null>(null)
```

Add actions:

```ts
async function loadArchivedTasks(value: BoardQuery = {}) {
  archivedLoading.value = true
  archivedError.value = null
  try {
    archivedTasks.value = await fetchArchivedTasks(projectId, value)
  } catch (error) {
    archivedError.value = '已归档任务加载失败'
  } finally {
    archivedLoading.value = false
  }
}

async function restoreArchivedTask(taskId: number) {
  await restoreTask(taskId)
  archivedTasks.value = archivedTasks.value.filter((task) => task.id !== taskId)
  await board.refreshProjectBoard()
}
```

Add buttons in header:

```vue
<div class="segmented-control" aria-label="看板视图">
  <button type="button" :class="{ active: boardMode === 'board' }" @click="boardMode = 'board'">当前看板</button>
  <button type="button" aria-label="查看已归档任务" :class="{ active: boardMode === 'archived' }" @click="boardMode = 'archived'; loadArchivedTasks()">已归档</button>
</div>
```

Render board or archived list with `v-if`.

- [ ] **Step 5: Run project board test**

Run:

```powershell
npm test -- project-board-view.spec.ts
```

Working directory: `web`

Expected: PASS.

- [ ] **Step 6: Commit archived view UI**

Run:

```powershell
git add web/src/components/task/ArchivedTaskList.vue web/src/views/ProjectBoardView.vue web/src/stores/tasks.ts web/tests/project-board-view.spec.ts
git commit -m "feat: add archived task view"
```

Expected: commit succeeds.

---

### Task 9: Notification Panel UI

**Files:**
- Create: `web/src/components/notification/NotificationPanel.vue`
- Modify: `web/src/App.vue`
- Modify: `web/src/styles/main.css`
- Modify: `web/tests/app.spec.ts`

- [ ] **Step 1: Write failing app notification test**

Add to `web/tests/app.spec.ts`:

```ts
it('shows notification unread count and opens the notification panel', async () => {
  setActivePinia(createPinia())
  const notifications = useNotificationsStore()
  notifications.unreadCount = 2
  notifications.items = [
    { id: 1, actor: null, projectId: 7, taskId: 12, type: 'MENTION', title: '有人提到了你', content: '任务评论提到了你', read: false, createdAt: '2026-05-29T10:00:00', readAt: null },
  ]

  const wrapper = mount(App, {
    global: {
      plugins: [router],
      stubs: { RouterView: true, RouterLink: true },
    },
  })

  await wrapper.get('[aria-label="通知"]').trigger('click')

  expect(document.body.textContent).toContain('2')
  expect(document.body.textContent).toContain('有人提到了你')
})
```

- [ ] **Step 2: Run app test and verify failure**

Run:

```powershell
npm test -- app.spec.ts
```

Working directory: `web`

Expected: FAIL because notification panel is not rendered.

- [ ] **Step 3: Create NotificationPanel component**

Create `NotificationPanel.vue`:

```vue
<script setup lang="ts">
import type { NotificationItem } from '../../api/notifications'

defineProps<{
  open: boolean
  items: NotificationItem[]
  loading?: boolean
}>()

const emit = defineEmits<{
  close: []
  markRead: [notificationId: number]
  markAllRead: []
  openTask: [taskId: number]
}>()
</script>

<template>
  <aside v-if="open" class="notification-panel" aria-label="通知列表">
    <header>
      <h2>通知</h2>
      <button class="secondary-button" type="button" @click="emit('markAllRead')">全部已读</button>
      <button class="secondary-button" type="button" @click="emit('close')">关闭</button>
    </header>
    <p v-if="loading" class="muted">正在加载通知...</p>
    <ul v-else class="notification-list">
      <li v-for="item in items" :key="item.id" :class="{ unread: !item.read }">
        <button class="link-button" type="button" @click="item.taskId && emit('openTask', item.taskId)">
          {{ item.title }}
        </button>
        <p>{{ item.content }}</p>
        <button v-if="!item.read" class="secondary-button" type="button" @click="emit('markRead', item.id)">标记已读</button>
      </li>
    </ul>
  </aside>
</template>
```

- [ ] **Step 4: Wire App notification state**

In `App.vue`, import store and component:

```ts
import { ref, onMounted } from 'vue'
import NotificationPanel from './components/notification/NotificationPanel.vue'
import { useNotificationsStore } from './stores/notifications'

const notifications = useNotificationsStore()
const notificationPanelOpen = ref(false)

onMounted(() => {
  notifications.loadUnreadCount()
})

async function openNotifications() {
  notificationPanelOpen.value = true
  await notifications.load('all')
  await notifications.loadUnreadCount()
}
```

Add sidebar button:

```vue
<button class="notification-button" type="button" aria-label="通知" @click="openNotifications">
  通知
  <span v-if="notifications.unreadCount > 0">{{ notifications.unreadCount }}</span>
</button>
```

Render panel:

```vue
<NotificationPanel
  :open="notificationPanelOpen"
  :items="notifications.items"
  :loading="notifications.loading"
  @close="notificationPanelOpen = false"
  @mark-read="notifications.markRead"
  @mark-all-read="notifications.markAllRead"
/>
```

- [ ] **Step 5: Add store methods for read actions**

In `notifications.ts`, add:

```ts
async markRead(notificationId: number) {
  const updated = await markNotificationRead(notificationId)
  this.items = this.items.map((item) => item.id === notificationId ? updated : item)
  await this.loadUnreadCount()
},
async markAllRead() {
  await markAllNotificationsRead()
  this.items = this.items.map((item) => ({ ...item, read: true, readAt: item.readAt ?? new Date().toISOString() }))
  this.unreadCount = 0
}
```

- [ ] **Step 6: Run app and notification store tests**

Run:

```powershell
npm test -- app.spec.ts notifications-store.spec.ts
```

Working directory: `web`

Expected: PASS.

- [ ] **Step 7: Commit notification UI**

Run:

```powershell
git add web/src/components/notification/NotificationPanel.vue web/src/App.vue web/src/styles/main.css web/src/stores/notifications.ts web/tests/app.spec.ts web/tests/notifications-store.spec.ts
git commit -m "feat: add notification panel"
```

Expected: commit succeeds.

---

### Task 10: End-to-End Flow and Final Verification

**Files:**
- Modify: `web/e2e/sd-kanban.spec.ts`
- Modify: `README_CN.md` only if the visible user workflow changes enough to document it.

- [ ] **Step 1: Extend Playwright E2E**

Add a flow to `web/e2e/sd-kanban.spec.ts` after the existing task creation flow:

```ts
await page.getByRole('button', { name: /新增任务/ }).click()
await page.getByLabel('任务标题').fill('协作增强 E2E')
await page.getByRole('button', { name: /创建任务/ }).click()
await page.getByText('协作增强 E2E').click()

await page.getByLabel('新增检查项标题').fill('补充检查清单')
await page.getByRole('button', { name: '添加' }).click()
await expect(page.getByText('检查清单 0/1')).toBeVisible()
await page.getByLabel('切换检查项 补充检查清单').check()
await expect(page.getByText('检查清单 1/1')).toBeVisible()

await page.getByLabel('新增评论').fill('@Admin 请查看这个任务')
await page.getByRole('button', { name: /添加评论/ }).click()
await expect(page.getByText('@Admin 请查看这个任务')).toBeVisible()

await page.getByRole('button', { name: /归档/ }).click()
await page.getByLabel('查看已归档任务').click()
await expect(page.getByText('协作增强 E2E')).toBeVisible()
await page.getByLabel('恢复任务 协作增强 E2E').click()
await page.getByRole('button', { name: '当前看板' }).click()
await expect(page.getByText('协作增强 E2E')).toBeVisible()
```

Use the labels defined in Tasks 7 and 8: `新增检查项标题`、`添加`、`切换检查项 补充检查清单`、`查看已归档任务`、`恢复任务 协作增强 E2E`、`当前看板`.

- [ ] **Step 2: Run complete frontend unit tests**

Run:

```powershell
npm test
```

Working directory: `web`

Expected: PASS.

- [ ] **Step 3: Run frontend build**

Run:

```powershell
npm run build
```

Working directory: `web`

Expected: PASS and Vite emits `dist`.

- [ ] **Step 4: Run selected backend tests**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=SchemaMigrationTest,TaskChecklistControllerTest,NotificationControllerTest,TaskControllerTest,BoardApiTest,MyTaskBoardApiTest" test
```

Expected: PASS.

- [ ] **Step 5: Run Playwright E2E**

Start backend and frontend using the project scripts or the existing E2E helper pattern. Use ports that are not occupied. Then run:

```powershell
npm run test:e2e
```

Working directory: `web`

Expected: PASS.

- [ ] **Step 6: Check git status**

Run:

```powershell
git status --short --branch
```

Expected: output shows the current branch with only intended files changed, or clean after the next commit.

- [ ] **Step 7: Commit final E2E and docs touchups**

Run:

```powershell
git add web/e2e/sd-kanban.spec.ts README_CN.md
git commit -m "test: cover task collaboration workflow"
```

If `README_CN.md` is unchanged, run:

```powershell
git add web/e2e/sd-kanban.spec.ts
git commit -m "test: cover task collaboration workflow"
```

Expected: commit succeeds.

---

## Verification Matrix

Run these commands before claiming the feature is complete:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn "-Dmaven.repo.local=D:\root\dev\Java\maven\repository" "-Dtest=SchemaMigrationTest,TaskChecklistControllerTest,NotificationControllerTest,TaskControllerTest,BoardApiTest,MyTaskBoardApiTest" test
```

Expected: selected backend tests pass.

```powershell
npm test
```

Working directory: `web`

Expected: all Vitest tests pass.

```powershell
npm run build
```

Working directory: `web`

Expected: Vite build succeeds.

```powershell
npm run test:e2e
```

Working directory: `web`

Expected: Playwright scenario passes against local backend and frontend.

## Acceptance Mapping

- 检查清单维护：Task 2、Task 6、Task 7、Task 10。
- 任务卡片检查清单进度：Task 5、Task 7。
- 已归档任务列表和恢复：Task 3、Task 6、Task 8、Task 10。
- 评论 @ 成员通知：Task 4、Task 9、Task 10。
- 分配、归档、恢复通知：Task 4、Task 9。
- 中文可读动态：Task 5、Task 7。
- 后端测试覆盖：Task 1 到 Task 5。
- 前端测试覆盖：Task 6 到 Task 9。
- E2E 覆盖：Task 10。
