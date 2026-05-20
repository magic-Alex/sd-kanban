# SD Kanban 设计规格说明

## 目标

构建一套面向小团队的完整敏捷开发看板应用，技术栈使用 Spring Boot、Vue 3、Maven、npm 和 MySQL。第一版聚焦团队日常执行，覆盖项目、成员、轻量迭代、自定义看板列、研发任务卡片、项目仪表盘，以及两种看板视图：项目总体看板和“我的任务”看板。

## 已确认决策

- 产品方向：团队日常执行看板。
- 用户模型：基础多用户登录和项目成员管理。
- 数据库：MySQL，地址 `localhost:3306`，数据库名 `sd_kanban`，本地开发用户名 `root`，密码 `root`。
- 看板列：每个项目可以自定义列；新建项目时自动生成默认列布局。
- 迭代支持：项目下支持轻量 Sprint/迭代。
- 任务卡片深度：研发任务卡片包含类型、优先级、故事点、工时估算、验收标准、评论、标签和活动日志。
- 主要页面结构：先进入首页仪表盘，再进入项目详情和看板页面。
- 统计深度：敏捷统计，包含迭代进度、任务类型分布、成员任务量和完成趋势。
- 交付方式：开发时前后端分离；生产时将 Vue 构建产物打进 Spring Boot jar。
- 架构方案：模块化单体。

## 运行与构建环境

项目后端使用 Maven 构建 Spring Boot，前端使用 npm 构建 Vue。Maven 本地仓库地址为 `D:\root\dev\Java\maven\repository`。Java、npm 和 Maven 已加入环境变量。

开发模式：

- 后端：Spring Boot 运行在 `8101` 端口。
- 前端：Vite 运行在 `8102` 端口。
- 前端通过代理将 `/api/**` 请求转发到 `http://localhost:8101`。

生产模式：

- Maven 执行后端测试和前端构建。
- Vue 的 `dist` 构建产物复制到 Spring Boot 静态资源目录。
- 最终通过单个 `java -jar` 进程启动应用。

## 架构设计

后端采用 Spring Boot 模块化单体。模块按照业务能力划分，而不是只按技术层划分：

- `auth` 和 `user`：注册、登录、当前用户、BCrypt 密码加密、JWT 签发和校验。
- `project` 和 `member`：项目创建、项目负责人、项目成员关系、成员列表、成员移除、负责人转交。
- `sprint`：轻量迭代创建、状态更新、日期范围、迭代筛选。
- `board`：项目自定义列、默认列初始化、列排序、列删除规则。
- `task`：研发任务卡片、任务分配、拖拽流转、评论、标签和活动日志。
- `dashboard`：首页仪表盘、项目统计、迭代统计、我的任务汇总等只读聚合数据。

前端采用 Vue 3、Vite、Vue Router、Pinia 和 Axios。页面和状态模块按相同业务边界组织：认证、仪表盘、项目、看板、任务和统计。

仪表盘模块保持只读。它聚合项目、迭代、任务和活动数据，但不反向修改业务状态。

## 数据模型

核心表：

- `users`：账号、昵称、邮箱、头像 URL、密码哈希、状态、创建时间、更新时间。
- `projects`：名称、描述、`owner_id`、创建人 id、状态、创建时间、更新时间。
- `project_members`：项目 id、用户 id、角色、加入时间。角色为 `owner` 和 `member`。
- `sprints`：项目 id、名称、目标、开始日期、结束日期、状态、创建时间、更新时间。
- `board_columns`：项目 id、名称、颜色、排序值、是否完成列、创建时间、更新时间。
- `tasks`：项目 id、迭代 id、列 id、负责人 id、创建人 id、标题、描述、任务类型、优先级、故事点、预估工时、截止日期、验收标准、排序值、状态标记、创建时间、更新时间。
- `task_tags`：项目 id、名称、颜色。
- `task_tag_links`：任务 id、标签 id。
- `task_comments`：任务 id、评论人 id、内容、创建时间、更新时间。
- `task_activities`：任务 id、项目 id、操作人 id、操作类型、字段名、旧值、新值、创建时间。

项目负责人和任务负责人要明确分开：

- `projects.owner_id` 表示项目负责人，可以管理项目设置、成员、迭代和看板列。
- `tasks.assignee_id` 表示任务负责人，只负责具体任务。

创建项目时，创建者自动成为项目负责人并加入项目成员。系统自动创建默认看板列，例如 backlog、ready、in progress、testing、done。项目负责人可以在项目创建后重命名、排序、新增和删除列。删除非空列会被阻止，必须先移动列中的任务。

## 权限设计

所有项目级资源都要求当前用户是项目成员：

- 项目成员可以查看项目、迭代、看板、任务、评论、活动日志和统计。
- 项目成员可以创建和更新任务、将任务分配给项目成员、发表评论、移动任务列。
- 项目负责人可以更新项目设置、转交负责人、管理成员、管理迭代、管理看板列。
- 项目负责人必须同时是项目成员。

第一版不包含全局管理员，也不做复杂的组织级权限体系。

## 页面与交互设计

应用从登录和注册开始。登录后用户进入首页仪表盘。

首页仪表盘：

- 展示我的待办、逾期任务、我负责的项目、我参与的项目、最近活动、完成趋势、迭代进度摘要、任务类型分布和成员任务量概览。
- 提供项目列表、项目详情、项目看板和我的任务看板入口。

项目列表：

- 展示项目名称、项目负责人、成员数量、当前活跃迭代、任务进度和最后活动时间。
- 支持创建新项目。

项目详情：

- 展示项目资料、负责人、成员、迭代列表、最近活动和项目统计。
- 项目负责人可以管理成员、转交负责人和管理迭代。

项目总体看板模式：

- 展示某个项目下的全部任务，使用该项目自定义列。
- 支持按迭代、负责人、任务类型、优先级和关键词筛选。
- 支持任务在列之间拖拽流转，以及在列内拖拽排序。
- 支持项目负责人维护看板列：新增、重命名、排序、删除空列。

我的任务看板模式：

- 展示分配给当前登录用户的任务，支持跨项目查看。
- 支持按状态/列分组，也支持按项目分组。
- 默认展示未完成任务，并支持按项目、迭代、优先级、截止日期和任务类型筛选。
- 使用任务原始项目中的列状态，所以修改会同步反映到项目总体看板。

任务详情抽屉：

- 从项目总体看板和我的任务看板都可以打开，不离开当前页面。
- 支持编辑标题、描述、类型、优先级、故事点、预估工时、验收标准、截止日期、负责人、迭代和标签。
- 展示评论和活动日志。
- 保存任务后刷新当前看板和相关仪表盘数据。

## API 设计

所有后端接口统一使用 `/api` 前缀。前端通过 Axios 拦截器附加 JWT，并统一处理标准错误响应。

认证：

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

项目与成员：

- `GET /api/projects`
- `POST /api/projects`
- `GET /api/projects/{projectId}`
- `PATCH /api/projects/{projectId}`
- `PATCH /api/projects/{projectId}/owner`
- `GET /api/projects/{projectId}/members`
- `POST /api/projects/{projectId}/members`
- `DELETE /api/projects/{projectId}/members/{userId}`

迭代：

- `GET /api/projects/{projectId}/sprints`
- `POST /api/projects/{projectId}/sprints`
- `PATCH /api/projects/{projectId}/sprints/{sprintId}`
- `DELETE /api/projects/{projectId}/sprints/{sprintId}`

看板列与看板数据：

- `GET /api/projects/{projectId}/board`
- `GET /api/projects/{projectId}/columns`
- `POST /api/projects/{projectId}/columns`
- `PATCH /api/projects/{projectId}/columns/{columnId}`
- `PATCH /api/projects/{projectId}/columns/order`
- `DELETE /api/projects/{projectId}/columns/{columnId}`

任务：

- `POST /api/tasks`
- `GET /api/tasks/{taskId}`
- `PATCH /api/tasks/{taskId}`
- `PATCH /api/tasks/{taskId}/position`
- `DELETE /api/tasks/{taskId}`
- `GET /api/tasks/mine`
- `GET /api/tasks/mine/board`

任务协作：

- `GET /api/tasks/{taskId}/comments`
- `POST /api/tasks/{taskId}/comments`
- `DELETE /api/tasks/{taskId}/comments/{commentId}`
- `GET /api/tasks/{taskId}/activities`
- `GET /api/projects/{projectId}/tags`
- `POST /api/projects/{projectId}/tags`
- `PATCH /api/projects/{projectId}/tags/{tagId}`
- `DELETE /api/projects/{projectId}/tags/{tagId}`

仪表盘与统计：

- `GET /api/dashboard/summary`
- `GET /api/dashboard/trends`
- `GET /api/projects/{projectId}/stats`
- `GET /api/projects/{projectId}/sprints/{sprintId}/stats`

## 数据流

登录流程：

1. 用户提交账号密码到 `POST /api/auth/login`。
2. 后端校验 BCrypt 密码，并返回 JWT 和当前用户数据。
3. 前端将 token 和用户资料保存到 Pinia。
4. 后续请求附加 `Authorization: Bearer <token>`。

项目看板流程：

1. 前端调用 `GET /api/projects/{projectId}/board`，可携带迭代、负责人、类型、优先级和关键词筛选条件。
2. 后端校验项目成员身份，加载项目列，加载匹配任务，并返回分组后的看板数据。
3. 前端渲染列和任务卡片。
4. 拖拽任务卡片时调用 `PATCH /api/tasks/{taskId}/position`。
5. 后端校验成员身份，更新列和排序值，写入任务活动日志，并返回更新后的任务数据。

我的任务看板流程：

1. 前端调用 `GET /api/tasks/mine/board`。
2. 后端加载 `assignee_id` 等于当前用户、且当前用户仍属于任务所在项目的任务。
3. 前端按状态或项目渲染个人看板。
4. 更新任务时复用项目看板相同的任务接口。

仪表盘流程：

1. 登录后前端调用仪表盘汇总和趋势接口。
2. 后端按当前用户聚合项目、任务、迭代、活动和成员任务量数据。
3. 仪表盘卡片跳转到项目看板、项目详情或我的任务看板。

## 错误处理

后端使用统一错误结构，包含错误码、消息和可选字段错误。

- `400`：输入非法，例如标题缺失、日期范围非法、故事点非法、筛选条件格式错误。
- `401`：JWT 缺失或无效。
- `403`：用户不是项目成员，或缺少项目负责人权限。
- `404`：项目、迭代、列、任务、标签或评论不存在。
- `409`：业务冲突，例如删除非空列、从成员中移除当前项目负责人、将任务分配给非项目成员。

前端行为：

- 表单错误展示在对应字段附近。
- 全局错误通过通知提示。
- 拖拽更新失败时，将任务卡片回滚到原列和原顺序。
- 登录过期后跳转到登录页。

## 安全设计

安全基于 Spring Security 和 JWT：

- 密码只存储 BCrypt 哈希。
- JWT 密钥和过期时间写在应用配置中。
- 除登录和注册外，所有 `/api/**` 接口都需要认证。
- 每个项目级接口在读取或写入数据前都校验项目成员身份。
- 项目负责人专属操作在服务层显式校验。
- 请求 DTO 使用 Bean Validation 提前拦截非法数据。

第一版不包含 refresh token、SSO、组织级 RBAC 或审计导出。

## 测试策略

后端测试：

- 针对认证、项目创建、负责人转交、成员校验、迭代生命周期、列管理、任务更新、任务排序和活动日志编写单元测试与服务测试。
- 针对登录、受保护接口、项目成员授权、项目看板数据、我的任务看板数据和仪表盘汇总编写 Controller/API 测试。
- 对查询正确性要求较高的部分，编写 MySQL 兼容行为的仓库或集成测试。

前端测试：

- 测试认证状态、看板加载、任务更新、仪表盘加载和错误处理相关 store。
- 测试任务卡片、看板列、任务详情抽屉、筛选器和仪表盘组件。
- 编写端到端测试覆盖登录、创建项目、创建任务、拖拽任务、切换项目看板/我的任务看板、项目负责人专属设置。

构建验证：

- `mvn test`
- `npm test`
- `npm run build`
- Maven 打包构建，并确认 Vue 构建产物被包含进 Spring Boot jar。

## 第一版不包含的范围

- 完整 Scrum 产品待办和 Sprint 容量计划。
- 燃尽图、周期时间、吞吐量和瓶颈分析。
- 全局管理员控制台和组织层级。
- 二进制文件上传存储。附件可以先用 URL/文本链接表达。
- 基于 WebSocket 的实时协作。
- 移动端原生应用。

## 范围检查

该设计适合进入单个实施计划，因为它产出的是一个边界清晰的完整应用：一个后端、一个前端、一个数据库 schema，以及明确的业务模块边界。实施计划仍应拆成小的纵向切片：项目脚手架、认证、项目与成员、迭代与列、任务与看板、仪表盘、前端页面、打包交付和端到端验证。
