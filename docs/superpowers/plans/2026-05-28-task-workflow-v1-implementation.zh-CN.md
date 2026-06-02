# Task Workflow V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让任务在项目看板中完成创建、查看、编辑、清空字段、分配、标记完成、归档、删除和过滤的日常工作闭环。

**Architecture:** 后端沿用 controller、service、service/impl、repository、dto、entity 分层；归档和删除使用 `tasks.is_archived` 与 `tasks.is_deleted` 软状态，所有日常看板查询排除这两类任务。前端沿用 Vue 3 + Pinia + Axios，`TaskDrawer` 承担任务工作台交互，`ProjectBoardView` 负责把成员、列、筛选和看板刷新串起来。

**Tech Stack:** Java 17、Spring Boot 3、Maven、MySQL、JPA、JdbcTemplate、Vue 3、Pinia、Axios、Vitest、Vue Test Utils、Playwright。

---

## File Structure

后端文件：

- Modify: `src/main/java/com/sdkanban/task/controller/TaskController.java`
  - 增加 `PATCH /api/tasks/{taskId}/archive` 和 `DELETE /api/tasks/{taskId}`。
- Modify: `src/main/java/com/sdkanban/task/service/TaskService.java`
  - 增加 `archive`、`delete` 服务方法。
- Modify: `src/main/java/com/sdkanban/task/service/impl/TaskServiceImpl.java`
  - 实现清空字段、归档、删除、活动记录。
- Modify: `src/main/java/com/sdkanban/task/dto/UpdateTaskRequest.java`
  - 增加 `clearFields`。
- Modify: `src/main/java/com/sdkanban/task/entity/Task.java`
  - 增加 `archive()`、`delete()` 状态方法。
- Modify: `src/main/java/com/sdkanban/task/repository/TaskRepository.java`
  - 项目看板和个人任务看板排除归档任务。
- Modify: `src/main/java/com/sdkanban/board/dto/TaskCardResponse.java`
  - 看板卡片返回负责人摘要，同时保留 `assigneeId`。
- Modify: `src/main/java/com/sdkanban/board/service/impl/BoardServiceImpl.java`
  - 装配卡片负责人信息。
- Test: `src/test/java/com/sdkanban/task/TaskControllerTest.java`
  - 覆盖清空字段、归档、删除。
- Test: `src/test/java/com/sdkanban/task/MyTaskBoardApiTest.java`
  - 覆盖个人任务看板排除归档任务。
- Test: `src/test/java/com/sdkanban/board/BoardApiTest.java`
  - 覆盖项目看板卡片返回负责人摘要。

前端文件：

- Modify: `web/src/api/tasks.ts`
  - 增加 `UpdateTaskRequest`、`archiveTask`、`deleteTask`。
- Modify: `web/src/api/board.ts`
  - `TaskCard` 增加 `assignee`。
- Modify: `web/src/stores/tasks.ts`
  - 增加保存、归档、删除的错误状态和 action。
- Modify: `web/src/stores/board.ts`
  - 增加标记完成、从看板移除任务、保留筛选刷新。
- Modify: `web/src/components/board/TaskCard.vue`
  - 展示负责人和逾期状态。
- Modify: `web/src/components/board/BoardFilters.vue`
  - 增加负责人筛选。
- Modify: `web/src/components/task/TaskDrawer.vue`
  - 增加查看/编辑模式、保存、完成、归档、删除。
- Modify: `web/src/views/ProjectBoardView.vue`
  - 连接成员列表、筛选、抽屉 action 和看板刷新。
- Modify: `web/src/styles/main.css`
  - 补齐编辑表单、动作栏、卡片状态样式。
- Test: `web/tests/board-store.spec.ts`
  - 覆盖完成和移除。
- Test: `web/tests/task-drawer.spec.ts`
  - 覆盖编辑、清空字段、失败保留草稿、删除确认。
- Test: `web/tests/project-board-view.spec.ts`
  - 覆盖负责人筛选和抽屉 action 刷新。
- Test: `web/e2e/sd-kanban.spec.ts`
  - 扩展真实流程：创建、编辑、完成、归档。

测试数据库说明：

- 后端测试命令使用 `DB_NAME=sd_kanban_test`，避免清理用户正在浏览的本地 `sd_kanban` 数据。
- Maven 本地仓库固定为 `D:\root\dev\Java\maven\repository`。

---

### Task 1: Backend Clear Fields

**Files:**
- Modify: `src/test/java/com/sdkanban/task/TaskControllerTest.java`
- Modify: `src/main/java/com/sdkanban/task/dto/UpdateTaskRequest.java`
- Modify: `src/main/java/com/sdkanban/task/service/impl/TaskServiceImpl.java`

- [ ] **Step 1: Write the failing clear-fields test**

Add this test to `TaskControllerTest`:

```java
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
```

Add this companion validation test:

```java
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
```

- [ ] **Step 2: Run the backend test and verify RED**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn -Dmaven.repo.local="D:\root\dev\Java\maven\repository" -Dtest=TaskControllerTest test
```

Expected: `projectMemberCanClearNullableTaskFields` fails because the nullable task fields still contain values, and `unknownClearFieldIsRejected` fails because `clearFields` is not validated.

- [ ] **Step 3: Implement `clearFields` in the request DTO**

Replace `UpdateTaskRequest` with:

```java
package com.sdkanban.task.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateTaskRequest(
    @Size(max = 200)
    String title,
    String description,
    @Size(max = 32)
    String taskType,
    @Size(max = 32)
    String priority,
    BigDecimal storyPoints,
    BigDecimal estimatedHours,
    LocalDate dueDate,
    String acceptanceCriteria,
    Long assigneeId,
    Long sprintId,
    Long columnId,
    List<String> clearFields
) {
}
```

- [ ] **Step 4: Implement clear-field handling in `TaskServiceImpl`**

Add this constant near `DEFAULT_TAG_COLOR`:

```java
private static final Set<String> CLEARABLE_FIELDS = Set.of(
    "description",
    "storyPoints",
    "estimatedHours",
    "dueDate",
    "acceptanceCriteria",
    "assigneeId",
    "sprintId"
);
```

Add this call at the start of `update`, after membership validation and before normal field updates:

```java
applyClearFields(task, request, currentUserId);
```

Add this method:

```java
private void applyClearFields(Task task, UpdateTaskRequest request, Long currentUserId) {
    if (request.clearFields() == null || request.clearFields().isEmpty()) {
        return;
    }
    for (String field : request.clearFields()) {
        if (!CLEARABLE_FIELDS.contains(field)) {
            throw BusinessException.badRequest("TASK_CLEAR_FIELD_NOT_ALLOWED", "Task field cannot be cleared");
        }
        switch (field) {
            case "description" -> change(task, currentUserId, "description", task.getDescription(), null, task::changeDescription);
            case "storyPoints" -> change(task, currentUserId, "storyPoints", task.getStoryPoints(), null, task::changeStoryPoints);
            case "estimatedHours" -> change(task, currentUserId, "estimatedHours", task.getEstimatedHours(), null, task::changeEstimatedHours);
            case "dueDate" -> change(task, currentUserId, "dueDate", task.getDueDate(), null, task::changeDueDate);
            case "acceptanceCriteria" -> change(task, currentUserId, "acceptanceCriteria", task.getAcceptanceCriteria(), null, task::changeAcceptanceCriteria);
            case "assigneeId" -> change(task, currentUserId, "assigneeId", task.getAssigneeId(), null, task::changeAssigneeId);
            case "sprintId" -> change(task, currentUserId, "sprintId", task.getSprintId(), null, task::changeSprintId);
            default -> throw BusinessException.badRequest("TASK_CLEAR_FIELD_NOT_ALLOWED", "Task field cannot be cleared");
        }
    }
}
```

Keep the existing non-null update blocks after `applyClearFields`; this makes a non-null request value win when the same field also appears in `clearFields`.

- [ ] **Step 5: Run the backend test and verify GREEN**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn -Dmaven.repo.local="D:\root\dev\Java\maven\repository" -Dtest=TaskControllerTest test
```

Expected: `TaskControllerTest` passes.

- [ ] **Step 6: Commit**

```powershell
git add src/test/java/com/sdkanban/task/TaskControllerTest.java src/main/java/com/sdkanban/task/dto/UpdateTaskRequest.java src/main/java/com/sdkanban/task/service/impl/TaskServiceImpl.java
git commit -m "feat: support clearing task fields"
```

---

### Task 2: Backend Archive, Delete, Filters, And Card Assignee

**Files:**
- Modify: `src/test/java/com/sdkanban/task/TaskControllerTest.java`
- Modify: `src/test/java/com/sdkanban/task/MyTaskBoardApiTest.java`
- Modify: `src/test/java/com/sdkanban/board/BoardApiTest.java`
- Modify: `src/main/java/com/sdkanban/task/controller/TaskController.java`
- Modify: `src/main/java/com/sdkanban/task/service/TaskService.java`
- Modify: `src/main/java/com/sdkanban/task/service/impl/TaskServiceImpl.java`
- Modify: `src/main/java/com/sdkanban/task/entity/Task.java`
- Modify: `src/main/java/com/sdkanban/task/repository/TaskRepository.java`
- Modify: `src/main/java/com/sdkanban/board/dto/TaskCardResponse.java`
- Modify: `src/main/java/com/sdkanban/board/service/impl/BoardServiceImpl.java`

- [ ] **Step 1: Write failing archive and delete tests**

In `TaskControllerTest`, add this static import:

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
```

Add these tests:

```java
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
```

- [ ] **Step 2: Write failing my-task archive filter test**

In `MyTaskBoardApiTest`, add this static import:

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
```

Add this test:

```java
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
        .andExpect(jsonPath("$.data.groups.length()").value(0));
}
```

- [ ] **Step 3: Write failing board card assignee test**

In `BoardApiTest.projectBoardReturnsColumnsAndFilteredProjectTasks`, after the existing task assertions add:

```java
.andExpect(jsonPath("$.data.columns[0].tasks[0].assignee.id").value(fixture.member().id()))
.andExpect(jsonPath("$.data.columns[0].tasks[0].assignee.nickname").value("member"))
```

- [ ] **Step 4: Run backend tests and verify RED**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn -Dmaven.repo.local="D:\root\dev\Java\maven\repository" -Dtest=TaskControllerTest,MyTaskBoardApiTest,BoardApiTest test
```

Expected: archive and delete endpoints are missing, archived tasks still appear in some board queries, and card assignee JSON is missing.

- [ ] **Step 5: Implement controller and service contract**

In `TaskController`, add the delete import:

```java
import org.springframework.web.bind.annotation.DeleteMapping;
```

Add endpoints after `updatePosition`:

```java
@PatchMapping("/tasks/{taskId}/archive")
ApiResponse<TaskResponse> archive(
    @PathVariable Long taskId,
    @AuthenticationPrincipal User user
) {
    return ApiResponse.ok(taskService.archive(taskId, currentUserId(user)));
}

@DeleteMapping("/tasks/{taskId}")
ApiResponse<Void> delete(
    @PathVariable Long taskId,
    @AuthenticationPrincipal User user
) {
    taskService.delete(taskId, currentUserId(user));
    return ApiResponse.ok(null);
}
```

In `TaskService`, add:

```java
TaskResponse archive(Long taskId, Long currentUserId);

void delete(Long taskId, Long currentUserId);
```

- [ ] **Step 6: Implement entity state methods**

In `Task`, add:

```java
public void archive() {
    this.archived = true;
}

public void delete() {
    this.deleted = true;
}
```

- [ ] **Step 7: Implement archive and delete service behavior**

In `TaskServiceImpl`, add:

```java
@Override
@Transactional
public TaskResponse archive(Long taskId, Long currentUserId) {
    Task task = requireTask(taskId);
    projectService.requireMember(task.getProjectId(), currentUserId);
    if (!task.isArchived()) {
        task.archive();
        recordActivity(task, currentUserId, "TASK_ARCHIVED", null, null, null);
    }
    return toTaskResponse(task);
}

@Override
@Transactional
public void delete(Long taskId, Long currentUserId) {
    Task task = requireTask(taskId);
    projectService.requireMember(task.getProjectId(), currentUserId);
    if (!task.isDeleted()) {
        task.delete();
        recordActivity(task, currentUserId, "TASK_DELETED", null, null, null);
    }
}
```

- [ ] **Step 8: Filter archived tasks in repository queries**

In `TaskRepository.findProjectBoardTasks`, add:

```java
and task.archived = false
```

directly after `and task.deleted = false`.

Replace the personal-board method with:

```java
List<Task> findByAssigneeIdAndDeletedFalseAndArchivedFalseOrderByProjectIdAscColumnIdAscSortOrderAscIdAsc(Long assigneeId);
```

In `BoardServiceImpl.myTaskBoard`, call:

```java
List<Task> tasks = taskRepository.findByAssigneeIdAndDeletedFalseAndArchivedFalseOrderByProjectIdAscColumnIdAscSortOrderAscIdAsc(currentUserId);
```

- [ ] **Step 9: Add assignee summaries to card responses**

Replace `TaskCardResponse` with:

```java
package com.sdkanban.board.dto;

import com.sdkanban.task.entity.Task;
import com.sdkanban.user.dto.UserSummary;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    Integer sortOrder
) {
    public static TaskCardResponse from(Task task, UserSummary assignee) {
        return new TaskCardResponse(
            task.getId(),
            task.getProjectId(),
            task.getSprintId(),
            task.getColumnId(),
            task.getAssigneeId(),
            assignee,
            task.getTitle(),
            task.getTaskType(),
            task.getPriority(),
            task.getStoryPoints(),
            task.getDueDate(),
            task.getSortOrder()
        );
    }
}
```

In `BoardServiceImpl`, add imports:

```java
import com.sdkanban.user.dto.UserSummary;
import com.sdkanban.user.entity.User;
import com.sdkanban.user.repository.UserRepository;
import java.util.Objects;
```

Inject `UserRepository`:

```java
private final UserRepository userRepository;
```

Update the constructor to accept and assign `userRepository`.

Replace `cards` with:

```java
private List<TaskCardResponse> cards(List<Task> tasks) {
    Map<Long, UserSummary> usersById = userRepository.findAllById(tasks.stream()
            .map(Task::getAssigneeId)
            .filter(Objects::nonNull)
            .distinct()
            .toList())
        .stream()
        .collect(Collectors.toMap(User::getId, UserSummary::from));

    return tasks.stream()
        .map(task -> TaskCardResponse.from(task, usersById.get(task.getAssigneeId())))
        .toList();
}
```

In `projectBoard`, replace the collector mapping with `Collectors.toList()` of tasks, then call `cards` when building each column:

```java
Map<Long, List<Task>> tasksByColumn = tasks.stream()
    .collect(Collectors.groupingBy(
        Task::getColumnId,
        LinkedHashMap::new,
        Collectors.toList()
    ));
```

and:

```java
cards(tasksByColumn.getOrDefault(column.getId(), List.of()))
```

- [ ] **Step 10: Run backend tests and verify GREEN**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn -Dmaven.repo.local="D:\root\dev\Java\maven\repository" -Dtest=TaskControllerTest,MyTaskBoardApiTest,BoardApiTest test
```

Expected: all selected backend tests pass.

- [ ] **Step 11: Commit**

```powershell
git add src/test/java/com/sdkanban/task/TaskControllerTest.java src/test/java/com/sdkanban/task/MyTaskBoardApiTest.java src/test/java/com/sdkanban/board/BoardApiTest.java src/main/java/com/sdkanban/task src/main/java/com/sdkanban/board
git commit -m "feat: archive and delete tasks"
```

---

### Task 3: Frontend API, Board Store, Cards, And Filters

**Files:**
- Modify: `web/src/api/tasks.ts`
- Modify: `web/src/api/board.ts`
- Modify: `web/src/stores/tasks.ts`
- Modify: `web/src/stores/board.ts`
- Modify: `web/src/components/board/TaskCard.vue`
- Modify: `web/src/components/board/BoardFilters.vue`
- Modify: `web/tests/board-store.spec.ts`
- Modify: `web/tests/project-board-view.spec.ts`

- [ ] **Step 1: Write failing board-store tests**

In `web/tests/board-store.spec.ts`, update mocked imports:

```ts
import { archiveTask, createTask, deleteTask, updateTaskPosition } from '../src/api/tasks'
```

Update the mock:

```ts
vi.mock('../src/api/tasks', () => ({
  archiveTask: vi.fn(),
  createTask: vi.fn(),
  deleteTask: vi.fn(),
  updateTaskPosition: vi.fn(),
}))
```

Add `assignee` to each `TaskCard` object in `projectBoard`:

```ts
assignee: { id: 3, account: 'member', nickname: 'Member', email: 'member@example.com', avatarUrl: null },
```

Add tests:

```ts
it('marks a task complete by moving it to the first done column', async () => {
  vi.mocked(updateTaskPosition).mockResolvedValue({ ...projectBoard.columns[0].tasks[0], columnId: 2, sortOrder: 0 })
  const board = useBoardStore()
  board.projectBoard = structuredClone(projectBoard)

  await board.markTaskComplete(12)

  expect(updateTaskPosition).toHaveBeenCalledWith(12, { columnId: 2, sortOrder: 0 })
  expect(board.projectBoard?.columns[0].tasks).toHaveLength(0)
  expect(board.projectBoard?.columns[1].tasks[0].id).toBe(12)
})

it('removes archived and deleted tasks from the current board', () => {
  const board = useBoardStore()
  board.projectBoard = structuredClone(projectBoard)

  board.removeTaskFromBoard(12)

  expect(board.projectBoard?.columns[0].tasks).toHaveLength(0)
})
```

- [ ] **Step 2: Run frontend tests and verify RED**

Run:

```powershell
cd web; npm test -- board-store.spec.ts
```

Expected: TypeScript or Vitest fails because `markTaskComplete` and `removeTaskFromBoard` do not exist.

- [ ] **Step 3: Implement task API functions and types**

In `web/src/api/tasks.ts`, change imports:

```ts
import { deleteData, getData, http, postData } from './http'
```

If `deleteData` does not exist in `http.ts`, use `http.delete` directly in `deleteTask`.

Add:

```ts
export interface UpdateTaskRequest {
  title?: string
  description?: string | null
  taskType?: string
  priority?: string
  storyPoints?: number | null
  estimatedHours?: number | null
  dueDate?: string | null
  acceptanceCriteria?: string | null
  assigneeId?: number | null
  sprintId?: number | null
  columnId?: number
  clearFields?: string[]
}
```

Replace `updateTask` with:

```ts
export async function updateTask(taskId: number, request: UpdateTaskRequest): Promise<TaskResponse> {
  const response = await http.patch(`/tasks/${taskId}`, request)
  return response.data.data
}
```

Add:

```ts
export async function archiveTask(taskId: number): Promise<TaskResponse> {
  const response = await http.patch(`/tasks/${taskId}/archive`)
  return response.data.data
}

export async function deleteTask(taskId: number): Promise<void> {
  await http.delete(`/tasks/${taskId}`)
}
```

- [ ] **Step 4: Update board card type**

In `web/src/api/board.ts`, import `UserSummary`:

```ts
import type { UserSummary } from './auth'
```

Add `assignee` to `TaskCard`:

```ts
assignee: UserSummary | null
```

- [ ] **Step 5: Implement board store helpers**

In `web/src/stores/board.ts`, add action state:

```ts
lastFilters: {} as BoardQuery,
lastProjectId: null as number | string | null,
```

Inside `loadProjectBoard`, set:

```ts
this.lastProjectId = projectId
this.lastFilters = { ...filters }
```

Add actions:

```ts
async refreshProjectBoard() {
  if (this.lastProjectId === null) {
    return
  }
  await this.loadProjectBoard(this.lastProjectId, this.lastFilters)
},
async markTaskComplete(taskId: number) {
  const doneColumn = this.projectBoard?.columns.find((column) => column.isDone)
  if (!doneColumn) {
    throw new Error('项目暂无完成列')
  }
  await this.moveTask(taskId, doneColumn.id, doneColumn.tasks.length)
},
removeTaskFromBoard(taskId: number) {
  this.removeTask(taskId)
},
```

- [ ] **Step 6: Enhance TaskCard display**

In `TaskCard.vue`, add:

```ts
import { computed } from 'vue'

const overdue = computed(() => {
  if (!props.task.dueDate) {
    return false
  }
  return props.task.dueDate < new Date().toISOString().slice(0, 10)
})
```

Update template meta:

```vue
<div class="task-card-main">
  <strong>{{ task.title }}</strong>
  <span>{{ task.taskType }} · {{ task.priority }}</span>
</div>
<div class="task-card-meta">
  <small>{{ task.assignee?.nickname ?? '未分配' }}</small>
  <small v-if="task.storyPoints !== null">{{ task.storyPoints }} SP</small>
  <small v-if="task.dueDate" :class="{ overdue }">{{ task.dueDate }}</small>
</div>
```

- [ ] **Step 7: Add assignee filter UI**

In `BoardFilters.vue`, import `ProjectMember`:

```ts
import type { ProjectMember } from '../../api/projects'
```

Add prop:

```ts
members: ProjectMember[]
```

Add select before type:

```vue
<select v-model="filters.assigneeId" aria-label="任务负责人筛选">
  <option value="">全部负责人</option>
  <option value="unassigned">未分配</option>
  <option v-for="member in members" :key="member.user.id" :value="String(member.user.id)">
    {{ member.user.nickname }}
  </option>
</select>
```

In `apply`, convert `"unassigned"` to a backend-compatible empty-assignee marker only after backend support exists. For V1 use:

```ts
const value = { ...filters }
if (value.assigneeId === 'unassigned') {
  value.assigneeId = '0'
}
```

Then in backend TaskRepository filters, support `0` as unassigned:

```java
and (:assigneeId is null or (:assigneeId = 0 and task.assigneeId is null) or task.assigneeId = :assigneeId)
```

- [ ] **Step 8: Connect members to filters in `ProjectBoardView`**

Replace:

```vue
<BoardFilters v-model="filters" @apply="applyFilters" />
```

with:

```vue
<BoardFilters v-model="filters" :members="members" @apply="applyFilters" />
```

- [ ] **Step 9: Run frontend tests and verify GREEN**

Run:

```powershell
cd web; npm test -- board-store.spec.ts project-board-view.spec.ts
```

Expected: selected frontend tests pass.

- [ ] **Step 10: Commit**

```powershell
git add web/src/api/tasks.ts web/src/api/board.ts web/src/stores/tasks.ts web/src/stores/board.ts web/src/components/board/TaskCard.vue web/src/components/board/BoardFilters.vue web/src/views/ProjectBoardView.vue web/tests/board-store.spec.ts web/tests/project-board-view.spec.ts
git commit -m "feat: enhance board task state"
```

---

### Task 4: Task Drawer Edit And Actions

**Files:**
- Modify: `web/tests/task-drawer.spec.ts`
- Modify: `web/src/components/task/TaskDrawer.vue`
- Modify: `web/src/stores/tasks.ts`
- Modify: `web/src/views/ProjectBoardView.vue`
- Modify: `web/src/styles/main.css`

- [ ] **Step 1: Write failing TaskDrawer edit test**

Add this test to `web/tests/task-drawer.spec.ts`:

```ts
it('saves edits and sends clearFields for emptied nullable fields', async () => {
  const saveTask = vi.fn(async () => undefined)
  mount(TaskDrawer, {
    attachTo: document.body,
    props: {
      open: true,
      task: {
        id: 12,
        projectId: 7,
        sprintId: null,
        columnId: 1,
        assignee: { id: 3, account: 'member', nickname: 'Member', email: null, avatarUrl: null },
        creator: { id: 1, account: 'alex', nickname: 'Alex', email: 'alex@example.com', avatarUrl: null },
        title: 'Build board',
        description: 'Remove this',
        taskType: 'STORY',
        priority: 'HIGH',
        storyPoints: 5,
        estimatedHours: 8,
        dueDate: '2026-06-01',
        acceptanceCriteria: 'Remove criteria',
        sortOrder: 0,
        tags: [],
        createdAt: '2026-05-21T10:00:00',
        updatedAt: '2026-05-21T10:00:00',
      },
      comments: [],
      activities: [],
      members: [
        { user: { id: 3, account: 'member', nickname: 'Member', email: null, avatarUrl: null }, role: 'member', joinedAt: '2026-05-21T10:00:00' },
      ],
      columns: [{ id: 1, name: 'Backlog', color: '#64748b', sortOrder: 0, isDone: false, tasks: [] }],
      addComment: async () => undefined,
      saveTask,
      completeTask: async () => undefined,
      archiveTask: async () => undefined,
      deleteTask: async () => undefined,
    },
  })

  await document.body.querySelector<HTMLButtonElement>('[aria-label="编辑任务"]')!.click()
  const title = document.body.querySelector<HTMLInputElement>('[aria-label="编辑任务标题"]')!
  title.value = 'Build board V2'
  title.dispatchEvent(new Event('input'))
  const description = document.body.querySelector<HTMLTextAreaElement>('[aria-label="编辑任务描述"]')!
  description.value = ''
  description.dispatchEvent(new Event('input'))
  const assignee = document.body.querySelector<HTMLSelectElement>('[aria-label="编辑任务负责人"]')!
  assignee.value = ''
  assignee.dispatchEvent(new Event('change'))
  await document.body.querySelector<HTMLButtonElement>('[aria-label="保存任务"]')!.click()
  await flushPromises()

  expect(saveTask).toHaveBeenCalledWith(expect.objectContaining({
    title: 'Build board V2',
    assigneeId: null,
    clearFields: expect.arrayContaining(['description', 'assigneeId']),
  }))
})
```

- [ ] **Step 2: Write failing action tests**

Add:

```ts
it('confirms before deleting a task', async () => {
  vi.spyOn(window, 'confirm').mockReturnValue(true)
  const deleteTask = vi.fn(async () => undefined)
  mount(TaskDrawer, {
    attachTo: document.body,
    props: {
      open: true,
      task: taskFixture(),
      comments: [],
      activities: [],
      members: [],
      columns: [],
      addComment: async () => undefined,
      saveTask: async () => undefined,
      completeTask: async () => undefined,
      archiveTask: async () => undefined,
      deleteTask,
    },
  })

  await document.body.querySelector<HTMLButtonElement>('[aria-label="删除任务"]')!.click()
  await flushPromises()

  expect(deleteTask).toHaveBeenCalled()
})
```

Add this helper at the bottom of the test file:

```ts
function taskFixture() {
  return {
    id: 12,
    projectId: 7,
    sprintId: null,
    columnId: 1,
    assignee: null,
    creator: { id: 1, account: 'alex', nickname: 'Alex', email: 'alex@example.com', avatarUrl: null },
    title: 'Build board',
    description: null,
    taskType: 'STORY',
    priority: 'HIGH',
    storyPoints: null,
    estimatedHours: null,
    dueDate: null,
    acceptanceCriteria: null,
    sortOrder: 0,
    tags: [],
    createdAt: '2026-05-21T10:00:00',
    updatedAt: '2026-05-21T10:00:00',
  }
}
```

- [ ] **Step 3: Run TaskDrawer tests and verify RED**

Run:

```powershell
cd web; npm test -- task-drawer.spec.ts
```

Expected: tests fail because edit/action props and controls are missing.

- [ ] **Step 4: Implement task store action state**

In `web/src/stores/tasks.ts`, update imports:

```ts
import {
  addTaskComment,
  archiveTask,
  deleteTask,
  fetchTask,
  updateTask,
  type TaskActivity,
  type TaskComment,
  type TaskResponse,
  type UpdateTaskRequest,
} from '../api/tasks'
```

Add state:

```ts
actionLoading: false,
actionError: null as string | null,
```

Replace `saveTask` with:

```ts
async saveTask(update: UpdateTaskRequest) {
  if (!this.activeTask) {
    return
  }
  this.actionLoading = true
  this.actionError = null
  try {
    this.activeTask = await updateTask(this.activeTask.id, update)
  } catch (error) {
    this.actionError = '任务保存失败，请重试'
    throw error
  } finally {
    this.actionLoading = false
  }
},
```

Add:

```ts
async archiveActiveTask() {
  if (!this.activeTask) {
    return
  }
  this.actionLoading = true
  this.actionError = null
  try {
    await archiveTask(this.activeTask.id)
    this.closeDrawer()
  } catch (error) {
    this.actionError = '任务归档失败，请重试'
    throw error
  } finally {
    this.actionLoading = false
  }
},
async deleteActiveTask() {
  if (!this.activeTask) {
    return
  }
  this.actionLoading = true
  this.actionError = null
  try {
    await deleteTask(this.activeTask.id)
    this.closeDrawer()
  } catch (error) {
    this.actionError = '任务删除失败，请重试'
    throw error
  } finally {
    this.actionLoading = false
  }
},
```

- [ ] **Step 5: Implement TaskDrawer editing**

In `TaskDrawer.vue`, add props:

```ts
import { computed, reactive, ref, watch } from 'vue'
import type { BoardColumn } from '../../api/board'
import type { ProjectMember } from '../../api/projects'
import type { TaskActivity, TaskComment, TaskResponse, UpdateTaskRequest } from '../../api/tasks'

const props = defineProps<{
  open: boolean
  task: TaskResponse | null
  comments: TaskComment[]
  activities: TaskActivity[]
  members: ProjectMember[]
  columns: BoardColumn[]
  actionLoading?: boolean
  actionError?: string | null
  addComment: (content: string) => Promise<void> | void
  saveTask: (request: UpdateTaskRequest) => Promise<void> | void
  completeTask: () => Promise<void> | void
  archiveTask: () => Promise<void> | void
  deleteTask: () => Promise<void> | void
}>()
```

Add edit state:

```ts
const editing = ref(false)
const editError = ref<string | null>(null)
const draft = reactive({
  title: '',
  description: '',
  taskType: 'TASK',
  priority: 'MEDIUM',
  assigneeId: '',
  storyPoints: '',
  estimatedHours: '',
  dueDate: '',
  acceptanceCriteria: '',
})

const doneColumnAvailable = computed(() => props.columns.some((column) => column.isDone))

function resetDraft() {
  if (!props.task) {
    return
  }
  draft.title = props.task.title
  draft.description = props.task.description ?? ''
  draft.taskType = props.task.taskType
  draft.priority = props.task.priority
  draft.assigneeId = props.task.assignee?.id ? String(props.task.assignee.id) : ''
  draft.storyPoints = props.task.storyPoints === null ? '' : String(props.task.storyPoints)
  draft.estimatedHours = props.task.estimatedHours === null ? '' : String(props.task.estimatedHours)
  draft.dueDate = props.task.dueDate ?? ''
  draft.acceptanceCriteria = props.task.acceptanceCriteria ?? ''
}

function nullableNumber(value: string) {
  return value === '' ? null : Number(value)
}

function nullableText(value: string) {
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

function buildUpdateRequest(): UpdateTaskRequest {
  const clearFields: string[] = []
  const request: UpdateTaskRequest = {
    title: draft.title.trim(),
    taskType: draft.taskType,
    priority: draft.priority,
    assigneeId: draft.assigneeId ? Number(draft.assigneeId) : null,
    description: nullableText(draft.description),
    storyPoints: nullableNumber(draft.storyPoints),
    estimatedHours: nullableNumber(draft.estimatedHours),
    dueDate: draft.dueDate || null,
    acceptanceCriteria: nullableText(draft.acceptanceCriteria),
  }

  if (request.description === null) clearFields.push('description')
  if (request.assigneeId === null) clearFields.push('assigneeId')
  if (request.storyPoints === null) clearFields.push('storyPoints')
  if (request.estimatedHours === null) clearFields.push('estimatedHours')
  if (request.dueDate === null) clearFields.push('dueDate')
  if (request.acceptanceCriteria === null) clearFields.push('acceptanceCriteria')
  if (clearFields.length > 0) request.clearFields = clearFields
  return request
}
```

Add action handlers:

```ts
async function saveEdits() {
  if (!draft.title.trim()) {
    editError.value = '任务标题不能为空'
    return
  }
  editError.value = null
  try {
    await props.saveTask(buildUpdateRequest())
    editing.value = false
  } catch (error) {
    editError.value = '任务保存失败，请重试'
  }
}

async function archiveCurrentTask() {
  try {
    await props.archiveTask()
  } catch (error) {
    editError.value = '任务归档失败，请重试'
  }
}

async function deleteCurrentTask() {
  if (!window.confirm('确认删除该任务？删除后将从看板中隐藏。')) {
    return
  }
  try {
    await props.deleteTask()
  } catch (error) {
    editError.value = '任务删除失败，请重试'
  }
}

watch(() => props.task?.id, resetDraft, { immediate: true })
watch(() => props.open, (open) => {
  if (open) {
    editing.value = false
    editError.value = null
    resetDraft()
  }
})
```

In the header actions, add buttons:

```vue
<div class="drawer-actions">
  <button class="secondary-button" type="button" aria-label="编辑任务" @click="editing = true">编辑</button>
  <button class="secondary-button" type="button" :disabled="!doneColumnAvailable || actionLoading" @click="completeTask">标记完成</button>
  <button class="secondary-button" type="button" aria-label="归档任务" :disabled="actionLoading" @click="archiveCurrentTask">归档</button>
  <button class="danger-button" type="button" aria-label="删除任务" :disabled="actionLoading" @click="deleteCurrentTask">删除</button>
  <button class="secondary-button" type="button" @click="emit('close')">关闭</button>
</div>
```

Add an edit form section shown when `editing`:

```vue
<form v-if="editing" class="task-edit-form" aria-label="编辑任务表单" @submit.prevent="saveEdits">
  <label class="full-field">
    任务标题
    <input v-model="draft.title" aria-label="编辑任务标题" maxlength="200" />
  </label>
  <label class="full-field">
    描述
    <textarea v-model="draft.description" aria-label="编辑任务描述" rows="3" />
  </label>
  <label>
    任务类型
    <select v-model="draft.taskType" aria-label="编辑任务类型">
      <option value="TASK">任务</option>
      <option value="STORY">故事</option>
      <option value="BUG">缺陷</option>
    </select>
  </label>
  <label>
    优先级
    <select v-model="draft.priority" aria-label="编辑任务优先级">
      <option value="HIGH">高</option>
      <option value="MEDIUM">中</option>
      <option value="LOW">低</option>
    </select>
  </label>
  <label>
    负责人
    <select v-model="draft.assigneeId" aria-label="编辑任务负责人">
      <option value="">未分配</option>
      <option v-for="member in members" :key="member.user.id" :value="String(member.user.id)">
        {{ member.user.nickname }}
      </option>
    </select>
  </label>
  <label>
    故事点
    <input v-model="draft.storyPoints" aria-label="编辑故事点" type="number" min="0" step="0.5" />
  </label>
  <label>
    预计工时
    <input v-model="draft.estimatedHours" aria-label="编辑预计工时" type="number" min="0" step="0.5" />
  </label>
  <label>
    截止日期
    <input v-model="draft.dueDate" aria-label="编辑截止日期" type="date" />
  </label>
  <label class="full-field">
    验收标准
    <textarea v-model="draft.acceptanceCriteria" aria-label="编辑验收标准" rows="3" />
  </label>
  <p v-if="editError || actionError" class="form-error full-field" aria-live="polite">
    {{ editError || actionError }}
  </p>
  <footer class="modal-actions full-field">
    <button class="secondary-button" type="button" @click="editing = false">取消</button>
    <button class="primary-button" type="submit" aria-label="保存任务" :disabled="actionLoading || !draft.title.trim()">
      {{ actionLoading ? '保存中...' : '保存任务' }}
    </button>
  </footer>
</form>
```

- [ ] **Step 6: Wire TaskDrawer actions in ProjectBoardView**

Add methods:

```ts
async function saveActiveTask(request: UpdateTaskRequest) {
  await tasks.saveTask(request)
  await board.refreshProjectBoard()
}

async function completeActiveTask() {
  if (!tasks.activeTask) {
    return
  }
  await board.markTaskComplete(tasks.activeTask.id)
  await tasks.openTask(tasks.activeTask.id)
}

async function archiveActiveTask() {
  const taskId = tasks.activeTask?.id
  await tasks.archiveActiveTask()
  if (taskId) {
    board.removeTaskFromBoard(taskId)
  }
}

async function deleteActiveTask() {
  const taskId = tasks.activeTask?.id
  await tasks.deleteActiveTask()
  if (taskId) {
    board.removeTaskFromBoard(taskId)
  }
}
```

Pass props:

```vue
<TaskDrawer
  :open="tasks.drawerOpen"
  :task="tasks.activeTask"
  :comments="tasks.comments"
  :activities="tasks.activities"
  :members="members"
  :columns="board.projectBoard?.columns ?? []"
  :action-loading="tasks.actionLoading"
  :action-error="tasks.actionError"
  :add-comment="tasks.addComment"
  :save-task="saveActiveTask"
  :complete-task="completeActiveTask"
  :archive-task="archiveActiveTask"
  :delete-task="deleteActiveTask"
  @close="tasks.closeDrawer"
/>
```

- [ ] **Step 7: Add CSS for drawer forms and danger action**

Add to `web/src/styles/main.css`:

```css
.drawer-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.danger-button {
  color: #991b1b;
  background: #fee2e2;
}

.task-edit-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  border-bottom: 1px solid #edf1f5;
  padding-bottom: 16px;
}

.overdue {
  color: #991b1b;
  background: #fee2e2 !important;
}
```

In the existing mobile media query, add `.task-edit-form` to the one-column selector.

- [ ] **Step 8: Run TaskDrawer tests and verify GREEN**

Run:

```powershell
cd web; npm test -- task-drawer.spec.ts project-board-view.spec.ts
```

Expected: selected frontend tests pass.

- [ ] **Step 9: Commit**

```powershell
git add web/src/components/task/TaskDrawer.vue web/src/stores/tasks.ts web/src/views/ProjectBoardView.vue web/src/styles/main.css web/tests/task-drawer.spec.ts web/tests/project-board-view.spec.ts
git commit -m "feat: edit tasks from drawer"
```

---

### Task 5: End-To-End Workflow And Final Verification

**Files:**
- Modify: `web/e2e/sd-kanban.spec.ts`
- Verify: backend, frontend, browser

- [ ] **Step 1: Extend the Playwright scenario**

In `web/e2e/sd-kanban.spec.ts`, after opening task detail from "我的任务", add:

```ts
await page.getByRole('button', { name: '编辑' }).click()
await page.getByLabel('编辑任务标题').fill(`${taskTitle} updated`)
await page.getByLabel('编辑任务描述').fill('')
await page.getByLabel('编辑预计工时').fill('6')
await page.getByRole('button', { name: '保存任务' }).click()
await expect(page.getByText(`${taskTitle} updated`)).toBeVisible()
await expect(page.getByText('6')).toBeVisible()

await page.getByRole('button', { name: '标记完成' }).click()
await page.getByRole('button', { name: '关闭' }).click()
await page.goto(`${frontendUrl}/projects/${projectId}/board`)
await expect(page.locator('.board-column', { hasText: 'Done' }).getByText(`${taskTitle} updated`)).toBeVisible()

await page.getByText(`${taskTitle} updated`).click()
await page.getByRole('button', { name: '归档' }).click()
await expect(page.getByText(`${taskTitle} updated`)).not.toBeVisible()
```

- [ ] **Step 2: Run full frontend verification**

Run:

```powershell
cd web; npm test
```

Expected: all Vitest suites pass.

Run:

```powershell
cd web; npm run build
```

Expected: Vite production build passes.

- [ ] **Step 3: Run selected backend verification on test database**

Run:

```powershell
$env:DB_NAME='sd_kanban_test'; mvn -Dmaven.repo.local="D:\root\dev\Java\maven\repository" -Dtest=TaskControllerTest,MyTaskBoardApiTest,BoardApiTest,DashboardControllerTest test
```

Expected: selected backend tests pass and use `sd_kanban_test`.

- [ ] **Step 4: Run Playwright against local services**

Start or keep backend on `8101` and frontend on `8102`, then run:

```powershell
cd web; npm run test:e2e
```

Expected: the workflow creates a project and task, edits the task, completes it, archives it, verifies it disappears from the active board, and cleans E2E data.

- [ ] **Step 5: Browser smoke test**

Open:

```text
http://localhost:8102/projects/10745/board
```

Manual smoke:

- Create a task.
- Open the task.
- Edit title, assignee, priority, due date, and clear description.
- Mark complete.
- Archive a task.
- Delete a task after confirmation.
- Use assignee filter with all负责人、未分配、具体成员。

- [ ] **Step 6: Final status and commit**

Run:

```powershell
git status --short
```

Expected: only intended files are modified.

Commit:

```powershell
git add src/main src/test web/src web/tests web/e2e
git commit -m "feat: complete task workflow"
```

---

## Self-Review

Spec coverage:

- 创建任务：已有能力保留，Task 5 覆盖完整流程。
- 打开详情：已有能力保留，Task 4 保持抽屉。
- 编辑任务信息：Task 1、Task 3、Task 4 覆盖。
- 分配和取消负责人：Task 1 清空，Task 4 编辑。
- 优先级、类型、故事点、工时、截止日期、验收标准：Task 4 覆盖。
- 标记完成：Task 3 store，Task 4 抽屉入口，Task 5 E2E。
- 归档和删除：Task 2 后端，Task 3/4 前端，Task 5 E2E。
- 评论和动态：Task 4 保留现有评论和动态展示，Task 1/2 继续记录活动。
- 项目看板和个人任务看板隐藏归档/删除：Task 2 覆盖。
- 卡片负责人和逾期提示：Task 2、Task 3 覆盖。
- 自动化测试：每个任务均有 RED/GREEN 验证命令。

Type consistency:

- 后端更新请求统一使用 `clearFields`。
- 前端更新请求统一使用 `UpdateTaskRequest`。
- 看板卡片同时保留 `assigneeId` 与 `assignee`。
- store action 名称为 `markTaskComplete`、`removeTaskFromBoard`、`refreshProjectBoard`。

Execution note:

- 后端测试使用 `sd_kanban_test`，避免清理当前人工浏览数据。
- Playwright 使用 E2E 清理逻辑，只清理本场景创建的账户和项目。
