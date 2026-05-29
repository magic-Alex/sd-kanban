# 任务协作增强 V2 设计

## 背景

SD Kanban 已经具备项目、项目负责人、项目成员、项目总体看板、个人任务看板、任务创建、任务编辑、评论、归档、删除和基础动态记录能力。当前缺口主要集中在任务协作的细节层面：任务无法拆成可勾选的执行步骤，归档任务缺少集中查看和恢复入口，动态记录偏技术字段，不够适合用户阅读，评论也还不能通过 @ 成员形成明确提醒。

本设计聚焦第一批任务协作增强，不包含 WIP 限制、泳道、任务依赖、燃尽图、累计流图或周期时间统计。这些能力会在后续版本单独设计，避免一次改动跨越过多看板、统计和流程模型。

## 目标

1. 用户可以在任务详情中维护检查清单，拆分小步骤并跟踪完成进度。
2. 用户可以查看项目内已归档任务，按常用条件搜索，并将归档任务恢复到看板。
3. 用户可以在任务详情中看到更易读的操作历史。
4. 用户可以在评论中 @ 项目成员，被 @ 成员可以在站内通知中看到提醒。
5. 任务分配、任务归档、任务恢复等关键事件会生成站内通知。
6. 新增功能继续遵循现有后端目录结构：`controller`、`service`、`service.impl`、`repository`、`dto`、`entity`。

## 非目标

- 不实现邮件、短信、企业微信、钉钉或其他外部通知。
- 不实现 WebSocket 或 SSE 实时推送，通知列表通过接口主动拉取。
- 不实现多级子任务，检查清单只支持任务下的一层条目。
- 不实现检查清单分组、负责人、截止日期或独立评论。
- 不实现归档任务批量恢复。
- 不实现任务依赖关系、泳道视图、WIP 限制、燃尽图、累计流图和周期时间统计。
- 不引入新的复杂权限模型，继续沿用项目负责人、任务创建人、任务负责人和项目成员的现有边界。

## 用户体验设计

### 任务检查清单

任务详情抽屉中新增“检查清单”区域，位置放在任务信息和评论之间。用户可以：

- 新增检查项。
- 编辑检查项标题。
- 勾选或取消勾选完成状态。
- 删除检查项。
- 通过上移、下移或拖拽调整顺序，第一版可以优先实现按钮排序。

检查清单展示完成进度，例如 `3/5`。任务卡片上展示轻量进度，格式为 `清单 3/5`。当任务没有检查项时，任务卡片不显示该信息。

### 归档任务视图

项目看板页面增加“当前看板 / 已归档”切换。当前看板保持现有列式看板体验，已归档视图展示归档任务列表。

已归档视图支持：

- 关键词搜索，匹配标题和描述。
- 按负责人筛选。
- 按任务类型筛选。
- 按优先级筛选。
- 打开任务详情查看内容、评论、检查清单和动态。
- 恢复任务。

恢复规则：

1. 项目负责人、任务创建人、任务负责人可以恢复任务。
2. 恢复后 `is_archived=false`。
3. 优先回到归档前所在列。
4. 如果原列不存在，则回到项目当前第一列。
5. 恢复后的排序放在目标列末尾。

### 操作历史增强

现有任务动态继续使用 `task_activities` 表。后端返回活动记录时补充面向展示的文案字段，前端优先展示该文案，不再直接暴露技术字段拼接。

示例文案：

- `Alex 创建了任务`
- `Alex 将负责人从 未分配 改为 李四`
- `Alex 将任务移动到 开发中`
- `Alex 添加了检查项：补充接口测试`
- `Alex 完成了检查项：上线前回归`
- `Alex 归档了任务`
- `Alex 恢复了任务`
- `Alex 评论了任务`
- `Alex 提到了 李四`

字段值需要尽量转成人可读名称。用户、看板列、任务类型、优先级等字段不应只显示数据库 ID。

### @ 成员和站内通知

评论输入框支持通过文本方式 @ 项目成员。第一版识别格式为 `@昵称`，只匹配当前项目成员的昵称。若昵称重复，则可以匹配多个成员，并为多个成员生成通知。

通知入口放在应用顶部导航区域，展示未读数量。通知列表支持：

- 查看未读和全部通知。
- 点击通知打开对应任务详情。
- 单条标记已读。
- 全部标记已读。

第一版通知类型：

- `MENTION`：评论中 @ 成员。
- `TASK_ASSIGNED`：任务负责人变更为某成员。
- `TASK_ARCHIVED`：任务被归档，通知任务负责人和创建人，操作者本人除外。
- `TASK_RESTORED`：任务被恢复，通知任务负责人和创建人，操作者本人除外。

为了避免打扰，操作者本人不收到自己触发的通知。

## 后端设计

### 数据模型

新增 `task_checklist_items`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键 |
| `task_id` | BIGINT | 任务 ID |
| `project_id` | BIGINT | 项目 ID，用于跨项目校验和查询 |
| `title` | VARCHAR(200) | 检查项标题 |
| `is_done` | BOOLEAN | 是否完成 |
| `sort_order` | INT | 排序 |
| `created_by` | BIGINT | 创建人 |
| `completed_by` | BIGINT NULL | 完成人 |
| `completed_at` | TIMESTAMP NULL | 完成时间 |
| `created_at` | TIMESTAMP | 创建时间 |
| `updated_at` | TIMESTAMP | 更新时间 |

新增 `notifications`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT | 主键 |
| `recipient_id` | BIGINT | 接收人 |
| `actor_id` | BIGINT NULL | 触发人 |
| `project_id` | BIGINT NULL | 关联项目 |
| `task_id` | BIGINT NULL | 关联任务 |
| `type` | VARCHAR(60) | 通知类型 |
| `title` | VARCHAR(200) | 通知标题 |
| `content` | VARCHAR(1000) | 通知内容 |
| `is_read` | BOOLEAN | 是否已读 |
| `created_at` | TIMESTAMP | 创建时间 |
| `read_at` | TIMESTAMP NULL | 已读时间 |

`tasks` 表不需要新增字段。归档恢复继续使用 `is_archived`。恢复时保留任务原本 `column_id`，如果该列不存在再选择项目第一列。

### 服务边界

新增任务检查清单能力：

- `TaskChecklistItem` entity
- `TaskChecklistItemRepository`
- `TaskChecklistService`
- `TaskChecklistServiceImpl`
- `TaskChecklistController`
- 检查清单相关 DTO

新增通知能力：

- `Notification` entity
- `NotificationRepository`
- `NotificationService`
- `NotificationServiceImpl`
- `NotificationController`
- 通知相关 DTO

扩展任务能力：

- `TaskService.restore(taskId, currentUserId)`
- `TaskService.archivedTasks(projectId, filters, currentUserId)`
- 评论保存后解析 @ 成员并生成通知。
- 任务负责人变更时生成 `TASK_ASSIGNED` 通知。
- 任务归档和恢复时生成对应通知。
- 检查清单变更时写入 `task_activities`。

扩展看板能力：

- `TaskCardResponse` 增加 `checklistDoneCount` 和 `checklistTotalCount`。
- 项目看板和个人任务看板的卡片都返回检查清单进度。

### API 设计

检查清单：

- `GET /api/tasks/{taskId}/checklist`
- `POST /api/tasks/{taskId}/checklist`
- `PATCH /api/tasks/{taskId}/checklist/{itemId}`
- `PATCH /api/tasks/{taskId}/checklist/{itemId}/toggle`
- `PATCH /api/tasks/{taskId}/checklist/reorder`
- `DELETE /api/tasks/{taskId}/checklist/{itemId}`

归档任务：

- `GET /api/projects/{projectId}/tasks/archived`
- `PATCH /api/tasks/{taskId}/restore`

通知：

- `GET /api/notifications?status=unread|all`
- `GET /api/notifications/unread-count`
- `PATCH /api/notifications/{notificationId}/read`
- `PATCH /api/notifications/read-all`

任务详情：

- `GET /api/tasks/{taskId}` 响应中可以继续保持任务主体信息。
- 评论、动态、检查清单可以由 store 分别加载，避免详情响应过重。

### 权限规则

- 项目成员可以查看项目任务、评论、动态和检查清单。
- 项目成员可以新增、编辑、勾选、排序、删除检查项。
- 归档和恢复沿用破坏性操作权限：项目负责人、任务创建人、任务负责人可以操作。
- 通知只能由接收人本人查看和标记已读。
- 评论 @ 只能通知同项目成员。

### 错误处理

- 检查项标题为空返回 `CHECKLIST_TITLE_REQUIRED`。
- 检查项不属于任务返回 `CHECKLIST_ITEM_NOT_FOUND`。
- 归档任务恢复时项目没有任何列返回 `BOARD_COLUMN_NOT_FOUND`。
- 非授权恢复返回 `TASK_ACTION_FORBIDDEN`。
- 通知不属于当前用户返回 `NOTIFICATION_NOT_FOUND`。

## 前端设计

### API 层

新增：

- `web/src/api/checklist.ts`
- `web/src/api/notifications.ts`

扩展：

- `web/src/api/tasks.ts`
  - 增加恢复任务接口。
  - 增加归档任务列表接口。
  - `TaskResponse` 或相关活动响应增加可展示文案。
- `web/src/api/board.ts`
  - `TaskCard` 增加检查清单进度字段。

### Store 层

新增：

- `web/src/stores/notifications.ts`

扩展：

- `web/src/stores/tasks.ts`
  - 加载、创建、更新、勾选、排序、删除检查项。
  - 恢复归档任务。
  - 打开归档任务详情。
- `web/src/stores/board.ts`
  - 刷新卡片检查清单进度。
  - 归档恢复后刷新当前项目看板。

### 组件和页面

新增或扩展：

- `TaskDrawer.vue`
  - 新增检查清单区域。
  - 评论输入支持 @ 文本。
  - 动态展示改为中文文案。
- `TaskCard.vue`
  - 展示检查清单进度。
- `ProjectBoardView.vue`
  - 增加“当前看板 / 已归档”切换。
- 新增归档任务列表组件，例如 `ArchivedTaskList.vue`。
- 新增通知列表组件，例如 `NotificationPanel.vue`。
- `App.vue`
  - 顶部导航增加通知入口和未读数量。

## 数据流

### 检查清单

1. 用户打开任务详情。
2. `tasks` store 加载任务主体、评论、动态和检查清单。
3. 用户新增或修改检查项。
4. 后端保存检查项并写入任务动态。
5. 前端刷新检查清单和任务卡片进度。

### 归档恢复

1. 用户切换到项目“已归档”视图。
2. 前端按筛选条件请求归档任务列表。
3. 用户打开任务详情并点击恢复。
4. 后端校验权限，恢复任务，写入动态，生成通知。
5. 前端从归档列表移除该任务，并在返回当前看板时刷新看板。

### @ 通知

1. 用户提交评论。
2. 后端保存评论。
3. 后端解析评论中的 `@昵称`。
4. 匹配当前项目成员，去重并排除操作者本人。
5. 为匹配成员生成 `MENTION` 通知。
6. 前端通知入口下次轮询或刷新时显示未读数量。

## 测试计划

### 后端测试

- `TaskChecklistControllerTest`
  - 项目成员可以新增检查项。
  - 可以勾选和取消勾选检查项。
  - 可以编辑、删除、排序检查项。
  - 非项目成员不能访问检查项。
  - 检查项变更写入动态。
- `TaskControllerTest`
  - 可以查询已归档任务列表。
  - 有权限用户可以恢复任务。
  - 无权限用户不能恢复任务。
  - 恢复任务时原列不存在则回到第一列。
  - 评论 @ 成员生成通知。
  - 负责人变更生成通知。
- `NotificationControllerTest`
  - 用户只能查看自己的通知。
  - 未读数量正确。
  - 单条和全部标记已读正确。
- `BoardApiTest`
  - 项目看板卡片返回检查清单进度。
  - 个人任务看板卡片返回检查清单进度。

### 前端测试

- `task-drawer.spec.ts`
  - 展示检查清单进度。
  - 新增、编辑、勾选、删除检查项。
  - 动态展示中文文案。
  - 评论中输入 @ 成员并提交。
- `project-board-view.spec.ts`
  - 任务卡片显示检查清单进度。
  - 可以切换到归档任务视图。
  - 可以恢复归档任务。
- `notifications-store.spec.ts`
  - 加载通知列表。
  - 加载未读数量。
  - 标记单条和全部已读。
- Playwright E2E
  - 创建任务。
  - 添加检查清单并完成部分检查项。
  - 评论 @ 成员。
  - 归档任务。
  - 在归档视图恢复任务。
  - 查看恢复后的任务仍在看板中。

## 实施顺序

1. 数据库迁移和后端实体。
2. 检查清单后端接口和测试。
3. 归档列表、恢复接口和测试。
4. 通知后端接口、@ 解析和测试。
5. 活动文案增强和卡片检查清单进度。
6. 前端 API 和 store。
7. 任务详情检查清单 UI。
8. 归档任务视图和恢复 UI。
9. 通知入口和通知列表 UI。
10. 前端单元测试、后端测试、构建和 Playwright E2E。

## 验收标准

- 用户可以在任务详情中维护检查清单，卡片和详情均能看到完成进度。
- 用户可以查看项目已归档任务，并按关键词、负责人、类型、优先级筛选。
- 有权限用户可以恢复归档任务，恢复后任务重新出现在项目看板。
- 评论 @ 项目成员后，被 @ 成员可以在站内通知中看到提醒。
- 任务分配、归档、恢复会生成站内通知，操作者本人不会收到自己的通知。
- 任务动态以中文可读文案展示关键变更。
- 新增后端接口具备自动化测试覆盖。
- 前端新增核心交互具备单元测试覆盖。
- `npm test`、后端相关 Maven 测试、`npm run build` 和核心 Playwright E2E 通过。
