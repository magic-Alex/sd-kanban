# SD Kanban 实施计划

> **给 agentic workers：** 必须使用子技能：建议使用 `superpowers:subagent-driven-development`，或使用 `superpowers:executing-plans` 按任务逐项执行本计划。步骤使用 checkbox（`- [ ]`）语法跟踪。

**目标：** 构建一套完整的 Spring Boot + Vue 敏捷看板应用，包含登录、项目、项目负责人、成员、迭代、自定义列、研发任务、项目总体看板、我的任务看板、仪表盘统计、MySQL 持久化和统一 jar 打包。

**架构：** 后端采用 `com.sdkanban` 包下的模块化单体，业务模块包括 auth、project、sprint、board、task 和 dashboard。前端位于 `web/`，使用 Vue 3、Pinia、Vue Router、Axios API 客户端，Vite 开发服务器端口为 `8102`。开发时 Spring Boot 运行在 `8101`，Vite 运行在 `8102`；生产时将前端构建产物打包进 Spring Boot jar。

**技术栈：** Java 17+、Spring Boot 3、Maven、MySQL 8、Spring Security、JWT、JPA/Hibernate、Bean Validation、Vue 3、Vite、Pinia、Vue Router、Axios、Element Plus、Vitest、Playwright。

---

## 来源规格

- 中文设计规格：`docs/superpowers/specs/2026-05-19-sd-kanban-design.zh-CN.md`
- 英文设计规格：`docs/superpowers/specs/2026-05-19-sd-kanban-design.md`
- MySQL：`localhost:3306`，数据库 `sd_kanban`，用户名 `root`，密码 `root`
- Maven 本地仓库：`D:\root\dev\Java\maven\repository`
- 后端端口：`8101`
- 前端端口：`8102`

## 文件结构

创建以下结构：

```text
sd_kanban/
  pom.xml
  README.md
  src/main/java/com/sdkanban/SdKanbanApplication.java
  src/main/java/com/sdkanban/common/ApiResponse.java
  src/main/java/com/sdkanban/common/BusinessException.java
  src/main/java/com/sdkanban/common/GlobalExceptionHandler.java
  src/main/java/com/sdkanban/common/CurrentUser.java
  src/main/java/com/sdkanban/config/SecurityConfig.java
  src/main/java/com/sdkanban/config/JwtService.java
  src/main/resources/application.yml
  src/main/resources/db/migration/V1__initial_schema.sql
  src/main/resources/db/migration/V2__seed_demo_data.sql
  src/main/java/com/sdkanban/auth/*
  src/main/java/com/sdkanban/user/*
  src/main/java/com/sdkanban/project/*
  src/main/java/com/sdkanban/sprint/*
  src/main/java/com/sdkanban/board/*
  src/main/java/com/sdkanban/task/*
  src/main/java/com/sdkanban/dashboard/*
  src/test/java/com/sdkanban/**/*
  web/package.json
  web/vite.config.ts
  web/index.html
  web/src/main.ts
  web/src/App.vue
  web/src/router/index.ts
  web/src/api/http.ts
  web/src/api/*.ts
  web/src/stores/*.ts
  web/src/views/*.vue
  web/src/components/**/*.vue
  web/src/styles/main.css
  web/tests/**/*.spec.ts
  web/e2e/**/*.spec.ts
```

职责边界：

- `common`：响应包装、异常模型、当前用户访问、共享校验行为。
- `config`：安全过滤链、JWT 解析、CORS、密码编码器。
- `auth` 和 `user`：账号持久化、注册、登录、当前用户。
- `project`：项目、负责人转交、项目成员关系。
- `sprint`：项目下的轻量迭代生命周期。
- `board`：项目自定义列和项目看板读模型。
- `task`：任务卡片状态、分配、评论、标签、活动日志、拖拽排序。
- `dashboard`：只读汇总和统计。
- `web/src/api`：类型化 API 客户端模块。
- `web/src/stores`：按功能划分的 Pinia 状态。
- `web/src/views`：路由级页面。
- `web/src/components`：看板、卡片、抽屉、筛选器、仪表盘等复用 UI。

## Git 说明

当前 shell 找不到 `git`。每个任务仍保留提交检查点，供 Git 可用的环境使用。如果执行时 Git 仍不可用，则在实施总结中记录变更文件和验证命令输出，用它替代提交记录。

---

### 任务 1：项目脚手架和构建接线

**文件：**
- 创建：`pom.xml`
- 创建：`README.md`
- 创建：`src/main/java/com/sdkanban/SdKanbanApplication.java`
- 创建：`src/main/resources/application.yml`
- 创建：`src/test/java/com/sdkanban/SdKanbanApplicationTests.java`
- 创建：`web/package.json`
- 创建：`web/vite.config.ts`
- 创建：`web/index.html`
- 创建：`web/src/main.ts`
- 创建：`web/src/App.vue`
- 创建：`web/src/router/index.ts`
- 创建：`web/src/styles/main.css`
- 创建：`web/tests/app.spec.ts`

- [ ] **步骤 1：先写后端脚手架失败测试**

创建 `src/test/java/com/sdkanban/SdKanbanApplicationTests.java`，写一个 Spring 上下文测试：

```java
package com.sdkanban;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SdKanbanApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

- [ ] **步骤 2：运行后端测试，确认脚手架不存在时失败**

运行：

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository test
```

期望：失败，因为此时还没有 `pom.xml` 或 Spring Boot 启动类。

- [ ] **步骤 3：创建后端脚手架**

创建 `pom.xml`，包含 Spring Boot 3 的 web、security、data-jpa、validation、MySQL、Flyway、jjwt 和测试依赖，并配置前端构建接线。在 `application.yml` 中配置后端端口：

```yaml
server:
  port: 8101

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/sd_kanban?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&createDatabaseIfNotExist=true
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true

app:
  jwt:
    secret: sd-kanban-local-development-secret-change-before-production
    expires-minutes: 720
```

创建 `SdKanbanApplication.java`：

```java
package com.sdkanban;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SdKanbanApplication {
    public static void main(String[] args) {
        SpringApplication.run(SdKanbanApplication.class, args);
    }
}
```

- [ ] **步骤 4：创建前端脚手架和开发端口**

创建 `web/vite.config.ts`，使用端口 `8102`，并将 `/api` 代理到后端 `8101`：

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 8102,
    proxy: {
      '/api': 'http://localhost:8101',
    },
  },
})
```

创建 `web/tests/app.spec.ts`：

```ts
import { describe, expect, it } from 'vitest'

describe('app scaffold', () => {
  it('uses the SD Kanban app name', () => {
    expect('SD Kanban').toBe('SD Kanban')
  })
})
```

- [ ] **步骤 5：验证脚手架通过**

运行：

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository test
cd web
npm install
npm test -- --run
npm run build
```

期望：后端上下文测试通过，Vitest 通过，Vite 构建生成 `web/dist`。

- [ ] **步骤 6：提交检查点**

```powershell
git add pom.xml README.md src web
git commit -m "chore: scaffold spring boot and vue apps"
```

期望：如果 Git 可用，提交成功。

---

### 任务 2：数据库 Schema 和后端公共基础

**文件：**
- 创建：`src/main/resources/db/migration/V1__initial_schema.sql`
- 创建：`src/main/java/com/sdkanban/common/ApiResponse.java`
- 创建：`src/main/java/com/sdkanban/common/BusinessException.java`
- 创建：`src/main/java/com/sdkanban/common/GlobalExceptionHandler.java`
- 创建：`src/main/java/com/sdkanban/common/CurrentUser.java`
- 测试：`src/test/java/com/sdkanban/common/GlobalExceptionHandlerTest.java`
- 测试：`src/test/java/com/sdkanban/schema/SchemaMigrationTest.java`

- [ ] **步骤 1：先写失败的 schema 和异常测试**

创建 `SchemaMigrationTest`，启动 Spring 上下文并验证 Flyway 可以创建表。创建 `GlobalExceptionHandlerTest`，调用一个示例 controller 抛出 `BusinessException.conflict("COLUMN_NOT_EMPTY", "看板列中仍有任务")`，期望 HTTP `409` 且 JSON code 为 `COLUMN_NOT_EMPTY`。

- [ ] **步骤 2：运行测试，确认失败**

运行：

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=SchemaMigrationTest,GlobalExceptionHandlerTest test
```

期望：失败，因为 schema 和公共错误类还不存在。

- [ ] **步骤 3：创建初始 schema**

创建 `V1__initial_schema.sql`，包含表：`users`、`projects`、`project_members`、`sprints`、`board_columns`、`tasks`、`task_tags`、`task_tag_links`、`task_comments`、`task_activities`。加入 `projects.owner_id`、`tasks.assignee_id`、`tasks.project_id`、`tasks.sprint_id`、`tasks.column_id` 和 `project_members` 相关外键。

- [ ] **步骤 4：创建公共响应和异常模型**

创建 `ApiResponse<T>`，字段包含 `success`、`data`、`code`、`message`、`fieldErrors`。创建 `BusinessException`，提供 bad request、forbidden、not found、conflict 的工厂方法。创建 `GlobalExceptionHandler`，将参数校验失败映射为 `400`，认证失败映射为 `401`，权限失败映射为 `403`，资源不存在映射为 `404`，业务冲突映射为 `409`。

- [ ] **步骤 5：验证测试通过**

运行：

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=SchemaMigrationTest,GlobalExceptionHandlerTest test
```

期望：通过。数据库迁移创建所有核心表，错误响应结构稳定。

- [ ] **步骤 6：提交检查点**

```powershell
git add src/main/resources/db/migration src/main/java/com/sdkanban/common src/test/java/com/sdkanban
git commit -m "feat: add schema and shared api foundation"
```

---

### 任务 3：认证、当前用户和安全配置

**文件：**
- 创建：`src/main/java/com/sdkanban/config/SecurityConfig.java`
- 创建：`src/main/java/com/sdkanban/config/JwtService.java`
- 创建：`src/main/java/com/sdkanban/auth/AuthController.java`
- 创建：`src/main/java/com/sdkanban/auth/AuthService.java`
- 创建：`src/main/java/com/sdkanban/auth/LoginRequest.java`
- 创建：`src/main/java/com/sdkanban/auth/RegisterRequest.java`
- 创建：`src/main/java/com/sdkanban/auth/AuthResponse.java`
- 创建：`src/main/java/com/sdkanban/user/User.java`
- 创建：`src/main/java/com/sdkanban/user/UserRepository.java`
- 创建：`src/main/java/com/sdkanban/user/UserSummary.java`
- 测试：`src/test/java/com/sdkanban/auth/AuthControllerTest.java`

- [ ] **步骤 1：先写失败的认证 API 测试**

覆盖以下行为：

```java
// register creates a user and returns token
// login returns token for valid password
// login rejects wrong password with 401
// /api/auth/me returns current user when Authorization header is present
// /api/projects returns 401 without Authorization header
```

- [ ] **步骤 2：运行认证测试，确认失败**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=AuthControllerTest test
```

期望：失败，因为认证接口和安全配置还不存在。

- [ ] **步骤 3：实现认证和安全**

实现 BCrypt 密码加密、JWT 创建和解析、Bearer Token 认证过滤器、注册/登录公开接口、受保护的 `/api/**` 接口，以及 `/api/auth/me`。

- [ ] **步骤 4：验证认证测试通过**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=AuthControllerTest test
```

期望：通过。匿名请求无法访问受保护接口，携带有效 JWT 的请求能得到当前用户数据。

- [ ] **步骤 5：提交检查点**

```powershell
git add src/main/java/com/sdkanban/config src/main/java/com/sdkanban/auth src/main/java/com/sdkanban/user src/test/java/com/sdkanban/auth
git commit -m "feat: add jwt authentication"
```

---

### 任务 4：项目、项目负责人和成员

**文件：**
- 创建：`src/main/java/com/sdkanban/project/Project.java`
- 创建：`src/main/java/com/sdkanban/project/ProjectMember.java`
- 创建：`src/main/java/com/sdkanban/project/ProjectRepository.java`
- 创建：`src/main/java/com/sdkanban/project/ProjectMemberRepository.java`
- 创建：`src/main/java/com/sdkanban/project/ProjectService.java`
- 创建：`src/main/java/com/sdkanban/project/ProjectController.java`
- 创建：`src/main/java/com/sdkanban/project/dto/*.java`
- 测试：`src/test/java/com/sdkanban/project/ProjectControllerTest.java`

- [ ] **步骤 1：先写失败的项目测试**

覆盖以下行为：

```java
// creating a project sets projects.owner_id to current user
// creating a project creates project_members row with role owner
// project list returns owned and joined projects only
// non-member cannot read project detail
// project owner can add a member
// ordinary member cannot transfer project owner
// owner transfer changes projects.owner_id and membership roles
```

- [ ] **步骤 2：运行测试，确认失败**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=ProjectControllerTest test
```

期望：失败，因为项目 API 还未实现。

- [ ] **步骤 3：实现项目和成员 API**

实现项目创建、项目列表、项目详情、成员列表、添加成员、移除成员和负责人转交。使用显式服务方法 `requireMember(projectId, userId)` 和 `requireOwner(projectId, userId)` 做权限校验。

- [ ] **步骤 4：验证项目测试通过**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=ProjectControllerTest test
```

期望：通过。项目负责人和成员规则符合规格。

- [ ] **步骤 5：提交检查点**

```powershell
git add src/main/java/com/sdkanban/project src/test/java/com/sdkanban/project
git commit -m "feat: add projects and membership"
```

---

### 任务 5：迭代和自定义看板列

**文件：**
- 创建：`src/main/java/com/sdkanban/sprint/*.java`
- 创建：`src/main/java/com/sdkanban/board/BoardColumn.java`
- 创建：`src/main/java/com/sdkanban/board/BoardColumnRepository.java`
- 创建：`src/main/java/com/sdkanban/board/BoardColumnService.java`
- 创建：`src/main/java/com/sdkanban/board/BoardColumnController.java`
- 测试：`src/test/java/com/sdkanban/sprint/SprintControllerTest.java`
- 测试：`src/test/java/com/sdkanban/board/BoardColumnControllerTest.java`

- [ ] **步骤 1：先写失败的迭代和列测试**

覆盖以下行为：

```java
// creating a project creates default columns: backlog, ready, in progress, testing, done
// project owner can create, rename, and reorder columns
// ordinary member cannot manage columns
// non-empty column deletion returns 409 with COLUMN_NOT_EMPTY
// project member can create and update sprints
// sprint end date cannot be before start date
```

- [ ] **步骤 2：运行测试，确认失败**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=SprintControllerTest,BoardColumnControllerTest test
```

期望：失败，因为迭代和看板列模块还不存在。

- [ ] **步骤 3：实现迭代和列**

实现项目下的迭代 CRUD。项目创建时自动初始化默认列。实现列新增、重命名、排序和删除空列。

- [ ] **步骤 4：验证测试通过**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=SprintControllerTest,BoardColumnControllerTest test
```

期望：通过。默认列存在，且列管理只允许项目负责人执行。

- [ ] **步骤 5：提交检查点**

```powershell
git add src/main/java/com/sdkanban/sprint src/main/java/com/sdkanban/board src/test/java/com/sdkanban/sprint src/test/java/com/sdkanban/board
git commit -m "feat: add sprints and board columns"
```

---

### 任务 6：任务、评论、标签和活动日志

**文件：**
- 创建：`src/main/java/com/sdkanban/task/*.java`
- 创建：`src/main/java/com/sdkanban/task/dto/*.java`
- 测试：`src/test/java/com/sdkanban/task/TaskControllerTest.java`
- 测试：`src/test/java/com/sdkanban/task/TaskActivityTest.java`

- [ ] **步骤 1：先写失败的任务测试**

覆盖以下行为：

```java
// project member can create a task with type, priority, story points, estimate, acceptance criteria, assignee, sprint, column
// task assignee must be a project member
// updating a task writes task_activities rows for changed fields
// adding a comment creates task_comments row and task activity
// tags are project-scoped and can be linked to tasks
// non-member cannot read or update a task
```

- [ ] **步骤 2：运行测试，确认失败**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=TaskControllerTest,TaskActivityTest test
```

期望：失败，因为任务模块还不存在。

- [ ] **步骤 3：实现任务协作模型**

实现任务实体、任务类型和优先级枚举、任务 DTO、任务服务、任务控制器、评论、标签、标签关联和活动日志。字段变更记录操作类型、字段名、旧值和新值。

- [ ] **步骤 4：验证任务测试通过**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=TaskControllerTest,TaskActivityTest test
```

期望：通过。任务创建、分配、评论、标签和活动日志符合规格。

- [ ] **步骤 5：提交检查点**

```powershell
git add src/main/java/com/sdkanban/task src/test/java/com/sdkanban/task
git commit -m "feat: add task collaboration"
```

---

### 任务 7：项目总体看板和我的任务看板 API

**文件：**
- 修改：`src/main/java/com/sdkanban/board/BoardController.java`
- 修改：`src/main/java/com/sdkanban/board/BoardService.java`
- 修改：`src/main/java/com/sdkanban/task/TaskController.java`
- 修改：`src/main/java/com/sdkanban/task/TaskService.java`
- 创建：`src/main/java/com/sdkanban/board/dto/BoardResponse.java`
- 创建：`src/main/java/com/sdkanban/board/dto/BoardColumnTasks.java`
- 创建：`src/main/java/com/sdkanban/board/dto/TaskCardResponse.java`
- 测试：`src/test/java/com/sdkanban/board/BoardApiTest.java`
- 测试：`src/test/java/com/sdkanban/task/MyTaskBoardApiTest.java`

- [ ] **步骤 1：先写失败的看板 API 测试**

覆盖以下行为：

```java
// GET /api/projects/{projectId}/board returns columns and filtered project tasks
// project board supports sprintId, assigneeId, type, priority, keyword filters
// PATCH /api/tasks/{taskId}/position changes column and sort order
// failed position update does not change task state
// GET /api/tasks/mine/board returns only tasks assigned to current user
// my-task board can group by project or by column state
```

- [ ] **步骤 2：运行测试，确认失败**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=BoardApiTest,MyTaskBoardApiTest test
```

期望：失败，因为看板读模型和任务位置更新还缺失。

- [ ] **步骤 3：实现看板读模型和任务位置更新**

实现看板 DTO、项目总体看板查询、我的任务看板查询、任务位置更新事务，以及拖拽活动日志。

- [ ] **步骤 4：验证看板测试通过**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=BoardApiTest,MyTaskBoardApiTest test
```

期望：通过。项目总体看板和个人看板返回正确任务范围。

- [ ] **步骤 5：提交检查点**

```powershell
git add src/main/java/com/sdkanban/board src/main/java/com/sdkanban/task src/test/java/com/sdkanban/board src/test/java/com/sdkanban/task
git commit -m "feat: add project and personal boards"
```

---

### 任务 8：仪表盘和敏捷统计

**文件：**
- 创建：`src/main/java/com/sdkanban/dashboard/DashboardController.java`
- 创建：`src/main/java/com/sdkanban/dashboard/DashboardService.java`
- 创建：`src/main/java/com/sdkanban/dashboard/dto/*.java`
- 测试：`src/test/java/com/sdkanban/dashboard/DashboardControllerTest.java`

- [ ] **步骤 1：先写失败的仪表盘测试**

覆盖以下行为：

```java
// GET /api/dashboard/summary returns my pending count, overdue count, owned projects, joined projects, recent activity
// GET /api/dashboard/trends returns completion trend buckets
// GET /api/projects/{projectId}/stats returns sprint progress, task type distribution, member workload
// non-member cannot read project stats
```

- [ ] **步骤 2：运行测试，确认失败**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=DashboardControllerTest test
```

期望：失败，因为仪表盘模块还不存在。

- [ ] **步骤 3：实现仪表盘只读接口**

实现当前用户汇总、趋势、项目统计、迭代统计、任务类型分布、成员任务量和最近活动的只读聚合查询。

- [ ] **步骤 4：验证仪表盘测试通过**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=DashboardControllerTest test
```

期望：通过。仪表盘接口返回当前用户范围内的数据。

- [ ] **步骤 5：提交检查点**

```powershell
git add src/main/java/com/sdkanban/dashboard src/test/java/com/sdkanban/dashboard
git commit -m "feat: add dashboard statistics"
```

---

### 任务 9：前端认证、应用外壳、仪表盘和项目页面

**文件：**
- 创建：`web/src/api/http.ts`
- 创建：`web/src/api/auth.ts`
- 创建：`web/src/api/projects.ts`
- 创建：`web/src/api/dashboard.ts`
- 创建：`web/src/stores/auth.ts`
- 创建：`web/src/stores/projects.ts`
- 创建：`web/src/stores/dashboard.ts`
- 创建：`web/src/views/LoginView.vue`
- 创建：`web/src/views/DashboardView.vue`
- 创建：`web/src/views/ProjectListView.vue`
- 创建：`web/src/views/ProjectDetailView.vue`
- 修改：`web/src/router/index.ts`
- 修改：`web/src/App.vue`
- 测试：`web/tests/auth-store.spec.ts`
- 测试：`web/tests/dashboard-view.spec.ts`

- [ ] **步骤 1：先写失败的前端认证和仪表盘测试**

创建 Vitest 测试，覆盖：

```ts
// auth store saves token after login
// auth store clears token after logout
// router guard redirects anonymous users to login
// dashboard view renders my pending tasks and owned projects from mocked API
```

- [ ] **步骤 2：运行前端测试，确认失败**

```powershell
cd web
npm test -- --run auth-store.spec.ts dashboard-view.spec.ts
```

期望：失败，因为 store 和页面还不存在。

- [ ] **步骤 3：实现前端应用外壳**

实现带 JWT 拦截器的 Axios 客户端、auth store、路由守卫、登录页、仪表盘页、项目列表、项目详情，以及 Dashboard、Projects、My Tasks、Logout 的共享布局导航。

- [ ] **步骤 4：验证前端测试通过**

```powershell
cd web
npm test -- --run auth-store.spec.ts dashboard-view.spec.ts
npm run build
```

期望：通过。前端构建成功，并使用 `8102` 端口配置。

- [ ] **步骤 5：提交检查点**

```powershell
git add web/src web/tests
git commit -m "feat: add frontend auth and dashboard"
```

---

### 任务 10：前端看板、任务抽屉和拖拽

**文件：**
- 创建：`web/src/api/board.ts`
- 创建：`web/src/api/tasks.ts`
- 创建：`web/src/stores/board.ts`
- 创建：`web/src/stores/tasks.ts`
- 创建：`web/src/views/ProjectBoardView.vue`
- 创建：`web/src/views/MyTaskBoardView.vue`
- 创建：`web/src/components/board/BoardColumn.vue`
- 创建：`web/src/components/board/TaskCard.vue`
- 创建：`web/src/components/board/BoardFilters.vue`
- 创建：`web/src/components/task/TaskDrawer.vue`
- 测试：`web/tests/board-store.spec.ts`
- 测试：`web/tests/task-drawer.spec.ts`

- [ ] **步骤 1：先写失败的看板组件测试**

创建 Vitest 测试，覆盖：

```ts
// board store loads project board columns and cards
// board store loads my-task board cards
// moving a task calls PATCH /api/tasks/{id}/position
// failed move restores previous column state
// task drawer renders acceptance criteria, comments, tags, and activities
```

- [ ] **步骤 2：运行前端看板测试，确认失败**

```powershell
cd web
npm test -- --run board-store.spec.ts task-drawer.spec.ts
```

期望：失败，因为看板 store 和组件还不存在。

- [ ] **步骤 3：实现看板 UI 和任务抽屉**

实现项目总体看板页、我的任务看板页、筛选器、列组件、任务卡片组件、任务抽屉、拖拽状态更新、失败回滚和任务保存后刷新。

- [ ] **步骤 4：验证看板 UI 测试通过**

```powershell
cd web
npm test -- --run board-store.spec.ts task-drawer.spec.ts
npm run build
```

期望：通过。项目总体看板和我的任务看板行为被组件测试和 store 测试覆盖。

- [ ] **步骤 5：提交检查点**

```powershell
git add web/src web/tests
git commit -m "feat: add frontend board workflows"
```

---

### 任务 11：统一打包和本地运行脚本

**文件：**
- 修改：`pom.xml`
- 修改：`README.md`
- 创建：`scripts/dev-backend.ps1`
- 创建：`scripts/dev-frontend.ps1`
- 创建：`scripts/package.ps1`
- 测试：`src/test/java/com/sdkanban/PackagingSmokeTest.java`

- [ ] **步骤 1：先写打包冒烟测试**

创建冒烟测试，验证当前端资源存在于 `src/main/resources/static` 时，Spring Boot 静态资源处理器可以返回 `index.html`。

- [ ] **步骤 2：运行打包测试，确认失败**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=PackagingSmokeTest test
```

期望：失败，因为前端构建产物尚未接入后端资源。

- [ ] **步骤 3：接入 Maven 前端构建和脚本**

配置 Maven 在 package 阶段进入 `web/` 执行 `npm install` 和 `npm run build`，然后在 jar 打包前将 `web/dist` 复制到 Spring Boot 静态资源。添加 PowerShell 脚本：

```powershell
# scripts/dev-backend.ps1
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository spring-boot:run

# scripts/dev-frontend.ps1
Set-Location web
npm run dev -- --host 0.0.0.0 --port 8102

# scripts/package.ps1
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository clean package
```

- [ ] **步骤 4：验证打包**

```powershell
.\scripts\package.ps1
```

期望：通过。Maven 创建 Spring Boot jar，并包含前端资源。

- [ ] **步骤 5：提交检查点**

```powershell
git add pom.xml README.md scripts src/test/java/com/sdkanban/PackagingSmokeTest.java
git commit -m "build: package frontend into spring boot jar"
```

---

### 任务 12：端到端验证

**文件：**
- 创建：`web/e2e/sd-kanban.spec.ts`
- 创建：`web/playwright.config.ts`
- 修改：`web/package.json`
- 修改：`README.md`

- [ ] **步骤 1：先写失败的 Playwright 场景**

创建端到端测试，覆盖：

```ts
// register and login
// create a project and see current user as project owner
// create a sprint
// create a task assigned to current user
// drag task across project board columns
// open my-task board and see the same task
// open task drawer and add a comment
// dashboard shows updated pending-task count
```

- [ ] **步骤 2：运行端到端测试，确认最终接线前失败**

```powershell
cd web
npx playwright test
```

期望：如果后端和前端服务未运行，或 E2E 接线缺失，则失败。

- [ ] **步骤 3：完成 E2E 接线**

配置 Playwright base URL 为 `http://localhost:8102`，文档说明后端必须运行在 `8101`，并添加 npm 脚本 `test:e2e`。

- [ ] **步骤 4：运行完整验证**

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository test
cd web
npm test -- --run
npm run build
npm run test:e2e
```

期望：所有后端测试通过，所有前端单元测试通过，前端构建成功，Playwright 场景在本地服务上通过。

- [ ] **步骤 5：提交检查点**

```powershell
git add web/e2e web/playwright.config.ts web/package.json README.md
git commit -m "test: add end-to-end kanban workflow"
```

---

## 自检记录

规格覆盖：

- MySQL、Maven 本地仓库、后端端口 `8101`、前端端口 `8102`：由任务 1 和任务 11 覆盖。
- Auth 和 JWT：由任务 3 覆盖。
- 项目负责人和任务负责人分离：由任务 4 和任务 6 覆盖。
- 项目自定义列和默认列布局：由任务 5 覆盖。
- 轻量迭代：由任务 5 覆盖。
- 研发任务卡片、评论、标签、活动日志：由任务 6 覆盖。
- 项目总体看板和我的任务看板：由任务 7 和任务 10 覆盖。
- 仪表盘统计：由任务 8 和任务 9 覆盖。
- 统一 jar 打包：由任务 11 覆盖。
- 端到端工作流：由任务 12 覆盖。

执行规则：

- 每个行为变更任务都从失败测试开始。
- 每个任务都包含验证失败的命令和验证通过的命令。
- 如果 Git 仍不可用，在实施总结中记录变更文件和命令输出，作为任务检查点。
