# SD Kanban Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete Spring Boot + Vue agile Kanban application with login, projects, project owners, members, sprints, custom columns, rich tasks, project board, my-task board, dashboard statistics, MySQL persistence, and unified jar packaging.

**Architecture:** Use a modular monolith backend under `com.sdkanban` with business modules for auth, project, sprint, board, task, and dashboard. Use a Vue 3 frontend in `web/` with Pinia stores, Vue Router pages, Axios API clients, and Vite dev server on port `8102`. Development runs Spring Boot on `8101` and Vite on `8102`; production packages the frontend into the Spring Boot jar.

**Tech Stack:** Java 17+, Spring Boot 3, Maven, MySQL 8, Spring Security, JWT, JPA/Hibernate, Bean Validation, Vue 3, Vite, Pinia, Vue Router, Axios, Element Plus, Vitest, Playwright.

---

## Source Specification

- Design spec: `docs/superpowers/specs/2026-05-19-sd-kanban-design.zh-CN.md`
- English spec: `docs/superpowers/specs/2026-05-19-sd-kanban-design.md`
- MySQL: `localhost:3306`, database `sd_kanban`, username `root`, password `root`
- Maven local repository: `D:\root\dev\Java\maven\repository`
- Backend port: `8101`
- Frontend port: `8102`

## File Structure

Create this structure:

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

Responsibility boundaries:

- `common`: response wrapper, exception model, current user access, shared validation behavior.
- `config`: security filter chain, JWT parsing, CORS, password encoder.
- `auth` and `user`: account persistence, registration, login, current user.
- `project`: projects, owner transfer, project membership.
- `sprint`: lightweight sprint lifecycle under a project.
- `board`: project custom columns and project board read model.
- `task`: task card state, assignment, comments, tags, activity logs, drag-and-drop positioning.
- `dashboard`: read-only summaries and statistics.
- `web/src/api`: typed API client modules.
- `web/src/stores`: Pinia state per feature.
- `web/src/views`: route-level screens.
- `web/src/components`: reusable UI for boards, cards, drawers, filters, dashboards.

## Git Note

The current shell cannot find `git`. Each task still includes a commit checkpoint for environments where Git is available. If Git is still unavailable during execution, record the changed files and verification output instead of committing.

---

### Task 1: Project Scaffold And Build Wiring

**Files:**
- Create: `pom.xml`
- Create: `README.md`
- Create: `src/main/java/com/sdkanban/SdKanbanApplication.java`
- Create: `src/main/resources/application.yml`
- Create: `src/test/java/com/sdkanban/SdKanbanApplicationTests.java`
- Create: `web/package.json`
- Create: `web/vite.config.ts`
- Create: `web/index.html`
- Create: `web/src/main.ts`
- Create: `web/src/App.vue`
- Create: `web/src/router/index.ts`
- Create: `web/src/styles/main.css`
- Create: `web/tests/app.spec.ts`

- [ ] **Step 1: Create the backend scaffold test**

Create `src/test/java/com/sdkanban/SdKanbanApplicationTests.java` with a Spring context test:

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

- [ ] **Step 2: Run the backend test and verify it fails before scaffold exists**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository test
```

Expected: FAIL because `pom.xml` or the Spring Boot application class is not present yet.

- [ ] **Step 3: Create backend scaffold**

Create `pom.xml` with Spring Boot 3 dependencies for web, security, data-jpa, validation, MySQL, Flyway, jjwt, tests, and frontend build wiring. Configure the backend port in `application.yml`:

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

Create `SdKanbanApplication.java`:

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

- [ ] **Step 4: Create frontend scaffold and dev port**

Create `web/vite.config.ts` with port `8102` and proxy to backend `8101`:

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

Create `web/tests/app.spec.ts`:

```ts
import { describe, expect, it } from 'vitest'

describe('app scaffold', () => {
  it('uses the SD Kanban app name', () => {
    expect('SD Kanban').toBe('SD Kanban')
  })
})
```

- [ ] **Step 5: Verify scaffold passes**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository test
cd web
npm install
npm test -- --run
npm run build
```

Expected: backend context test passes, Vitest passes, and Vite build creates `web/dist`.

- [ ] **Step 6: Commit checkpoint**

```powershell
git add pom.xml README.md src web
git commit -m "chore: scaffold spring boot and vue apps"
```

Expected if Git is available: commit succeeds.

---

### Task 2: Database Schema And Shared Backend Foundation

**Files:**
- Create: `src/main/resources/db/migration/V1__initial_schema.sql`
- Create: `src/main/java/com/sdkanban/common/ApiResponse.java`
- Create: `src/main/java/com/sdkanban/common/BusinessException.java`
- Create: `src/main/java/com/sdkanban/common/GlobalExceptionHandler.java`
- Create: `src/main/java/com/sdkanban/common/CurrentUser.java`
- Test: `src/test/java/com/sdkanban/common/GlobalExceptionHandlerTest.java`
- Test: `src/test/java/com/sdkanban/schema/SchemaMigrationTest.java`

- [ ] **Step 1: Write failing schema and exception tests**

Create `SchemaMigrationTest` that starts the Spring context and verifies Flyway can create tables. Create `GlobalExceptionHandlerTest` that calls a sample controller throwing `BusinessException.conflict("COLUMN_NOT_EMPTY", "看板列中仍有任务")` and expects HTTP `409` with JSON code `COLUMN_NOT_EMPTY`.

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=SchemaMigrationTest,GlobalExceptionHandlerTest test
```

Expected: FAIL because schema and common error classes do not exist.

- [ ] **Step 3: Create initial schema**

Create `V1__initial_schema.sql` with tables: `users`, `projects`, `project_members`, `sprints`, `board_columns`, `tasks`, `task_tags`, `task_tag_links`, `task_comments`, and `task_activities`. Include foreign keys for `projects.owner_id`, `tasks.assignee_id`, `tasks.project_id`, `tasks.sprint_id`, `tasks.column_id`, and `project_members`.

- [ ] **Step 4: Create shared response and exceptions**

Create `ApiResponse<T>` with `success`, `data`, `code`, `message`, and `fieldErrors`. Create `BusinessException` factory methods for bad request, forbidden, not found, and conflict. Create `GlobalExceptionHandler` mapping validation failures to `400`, authentication failures to `401`, forbidden failures to `403`, missing resource failures to `404`, and conflicts to `409`.

- [ ] **Step 5: Verify tests pass**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=SchemaMigrationTest,GlobalExceptionHandlerTest test
```

Expected: PASS. Database migration creates all core tables and error response shape is stable.

- [ ] **Step 6: Commit checkpoint**

```powershell
git add src/main/resources/db/migration src/main/java/com/sdkanban/common src/test/java/com/sdkanban
git commit -m "feat: add schema and shared api foundation"
```

---

### Task 3: Authentication, Current User, And Security

**Files:**
- Create: `src/main/java/com/sdkanban/config/SecurityConfig.java`
- Create: `src/main/java/com/sdkanban/config/JwtService.java`
- Create: `src/main/java/com/sdkanban/auth/AuthController.java`
- Create: `src/main/java/com/sdkanban/auth/AuthService.java`
- Create: `src/main/java/com/sdkanban/auth/LoginRequest.java`
- Create: `src/main/java/com/sdkanban/auth/RegisterRequest.java`
- Create: `src/main/java/com/sdkanban/auth/AuthResponse.java`
- Create: `src/main/java/com/sdkanban/user/User.java`
- Create: `src/main/java/com/sdkanban/user/UserRepository.java`
- Create: `src/main/java/com/sdkanban/user/UserSummary.java`
- Test: `src/test/java/com/sdkanban/auth/AuthControllerTest.java`

- [ ] **Step 1: Write failing authentication API tests**

Create tests that cover:

```java
// register creates a user and returns token
// login returns token for valid password
// login rejects wrong password with 401
// /api/auth/me returns current user when Authorization header is present
// /api/projects returns 401 without Authorization header
```

- [ ] **Step 2: Run auth tests and verify failure**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=AuthControllerTest test
```

Expected: FAIL because auth endpoints and security configuration do not exist.

- [ ] **Step 3: Implement auth and security**

Implement BCrypt password hashing, JWT creation and parsing, a bearer-token authentication filter, public endpoints for register/login, protected `/api/**` endpoints, and `/api/auth/me`.

- [ ] **Step 4: Verify auth tests pass**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=AuthControllerTest test
```

Expected: PASS. Protected endpoints reject anonymous requests, and valid JWT requests receive current user data.

- [ ] **Step 5: Commit checkpoint**

```powershell
git add src/main/java/com/sdkanban/config src/main/java/com/sdkanban/auth src/main/java/com/sdkanban/user src/test/java/com/sdkanban/auth
git commit -m "feat: add jwt authentication"
```

---

### Task 4: Projects, Project Owner, And Members

**Files:**
- Create: `src/main/java/com/sdkanban/project/Project.java`
- Create: `src/main/java/com/sdkanban/project/ProjectMember.java`
- Create: `src/main/java/com/sdkanban/project/ProjectRepository.java`
- Create: `src/main/java/com/sdkanban/project/ProjectMemberRepository.java`
- Create: `src/main/java/com/sdkanban/project/ProjectService.java`
- Create: `src/main/java/com/sdkanban/project/ProjectController.java`
- Create: `src/main/java/com/sdkanban/project/dto/*.java`
- Test: `src/test/java/com/sdkanban/project/ProjectControllerTest.java`

- [ ] **Step 1: Write failing project tests**

Test these behaviors:

```java
// creating a project sets projects.owner_id to current user
// creating a project creates project_members row with role owner
// project list returns owned and joined projects only
// non-member cannot read project detail
// project owner can add a member
// ordinary member cannot transfer project owner
// owner transfer changes projects.owner_id and membership roles
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=ProjectControllerTest test
```

Expected: FAIL because project APIs are not implemented.

- [ ] **Step 3: Implement project and member APIs**

Implement project creation, project listing, project detail, member listing, member add/remove, and owner transfer. Use explicit service methods `requireMember(projectId, userId)` and `requireOwner(projectId, userId)`.

- [ ] **Step 4: Verify project tests pass**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=ProjectControllerTest test
```

Expected: PASS. Project owner and membership rules match the spec.

- [ ] **Step 5: Commit checkpoint**

```powershell
git add src/main/java/com/sdkanban/project src/test/java/com/sdkanban/project
git commit -m "feat: add projects and membership"
```

---

### Task 5: Sprints And Custom Board Columns

**Files:**
- Create: `src/main/java/com/sdkanban/sprint/*.java`
- Create: `src/main/java/com/sdkanban/board/BoardColumn.java`
- Create: `src/main/java/com/sdkanban/board/BoardColumnRepository.java`
- Create: `src/main/java/com/sdkanban/board/BoardColumnService.java`
- Create: `src/main/java/com/sdkanban/board/BoardColumnController.java`
- Test: `src/test/java/com/sdkanban/sprint/SprintControllerTest.java`
- Test: `src/test/java/com/sdkanban/board/BoardColumnControllerTest.java`

- [ ] **Step 1: Write failing sprint and column tests**

Test these behaviors:

```java
// creating a project creates default columns: backlog, ready, in progress, testing, done
// project owner can create, rename, and reorder columns
// ordinary member cannot manage columns
// non-empty column deletion returns 409 with COLUMN_NOT_EMPTY
// project member can create and update sprints
// sprint end date cannot be before start date
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=SprintControllerTest,BoardColumnControllerTest test
```

Expected: FAIL because sprint and board column modules do not exist.

- [ ] **Step 3: Implement sprints and columns**

Implement sprint CRUD under a project. Implement default column bootstrap from project creation. Implement column create, rename, reorder, and delete-empty-column behavior.

- [ ] **Step 4: Verify tests pass**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=SprintControllerTest,BoardColumnControllerTest test
```

Expected: PASS. Default columns exist and owner-only column management is enforced.

- [ ] **Step 5: Commit checkpoint**

```powershell
git add src/main/java/com/sdkanban/sprint src/main/java/com/sdkanban/board src/test/java/com/sdkanban/sprint src/test/java/com/sdkanban/board
git commit -m "feat: add sprints and board columns"
```

---

### Task 6: Tasks, Comments, Tags, And Activity Logs

**Files:**
- Create: `src/main/java/com/sdkanban/task/*.java`
- Create: `src/main/java/com/sdkanban/task/dto/*.java`
- Test: `src/test/java/com/sdkanban/task/TaskControllerTest.java`
- Test: `src/test/java/com/sdkanban/task/TaskActivityTest.java`

- [ ] **Step 1: Write failing task tests**

Test these behaviors:

```java
// project member can create a task with type, priority, story points, estimate, acceptance criteria, assignee, sprint, column
// task assignee must be a project member
// updating a task writes task_activities rows for changed fields
// adding a comment creates task_comments row and task activity
// tags are project-scoped and can be linked to tasks
// non-member cannot read or update a task
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=TaskControllerTest,TaskActivityTest test
```

Expected: FAIL because task module does not exist.

- [ ] **Step 3: Implement task collaboration model**

Implement task entity, task type and priority enums, task DTOs, task service, task controller, comments, tags, tag links, and activity logging. Store field changes with action type, field name, old value, and new value.

- [ ] **Step 4: Verify task tests pass**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=TaskControllerTest,TaskActivityTest test
```

Expected: PASS. Task creation, assignment, comments, tags, and activity logs match the spec.

- [ ] **Step 5: Commit checkpoint**

```powershell
git add src/main/java/com/sdkanban/task src/test/java/com/sdkanban/task
git commit -m "feat: add task collaboration"
```

---

### Task 7: Project Board And My-Task Board APIs

**Files:**
- Modify: `src/main/java/com/sdkanban/board/BoardController.java`
- Modify: `src/main/java/com/sdkanban/board/BoardService.java`
- Modify: `src/main/java/com/sdkanban/task/TaskController.java`
- Modify: `src/main/java/com/sdkanban/task/TaskService.java`
- Create: `src/main/java/com/sdkanban/board/dto/BoardResponse.java`
- Create: `src/main/java/com/sdkanban/board/dto/BoardColumnTasks.java`
- Create: `src/main/java/com/sdkanban/board/dto/TaskCardResponse.java`
- Test: `src/test/java/com/sdkanban/board/BoardApiTest.java`
- Test: `src/test/java/com/sdkanban/task/MyTaskBoardApiTest.java`

- [ ] **Step 1: Write failing board API tests**

Test these behaviors:

```java
// GET /api/projects/{projectId}/board returns columns and filtered project tasks
// project board supports sprintId, assigneeId, type, priority, keyword filters
// PATCH /api/tasks/{taskId}/position changes column and sort order
// failed position update does not change task state
// GET /api/tasks/mine/board returns only tasks assigned to current user
// my-task board can group by project or by column state
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=BoardApiTest,MyTaskBoardApiTest test
```

Expected: FAIL because board read models and position updates are missing.

- [ ] **Step 3: Implement board read models and positioning**

Implement board DTOs, project board query, my-task board query, position update transaction, and drag activity logging.

- [ ] **Step 4: Verify board tests pass**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=BoardApiTest,MyTaskBoardApiTest test
```

Expected: PASS. Project-wide board and personal board return correct task scopes.

- [ ] **Step 5: Commit checkpoint**

```powershell
git add src/main/java/com/sdkanban/board src/main/java/com/sdkanban/task src/test/java/com/sdkanban/board src/test/java/com/sdkanban/task
git commit -m "feat: add project and personal boards"
```

---

### Task 8: Dashboard And Agile Statistics

**Files:**
- Create: `src/main/java/com/sdkanban/dashboard/DashboardController.java`
- Create: `src/main/java/com/sdkanban/dashboard/DashboardService.java`
- Create: `src/main/java/com/sdkanban/dashboard/dto/*.java`
- Test: `src/test/java/com/sdkanban/dashboard/DashboardControllerTest.java`

- [ ] **Step 1: Write failing dashboard tests**

Test these behaviors:

```java
// GET /api/dashboard/summary returns my pending count, overdue count, owned projects, joined projects, recent activity
// GET /api/dashboard/trends returns completion trend buckets
// GET /api/projects/{projectId}/stats returns sprint progress, task type distribution, member workload
// non-member cannot read project stats
```

- [ ] **Step 2: Run tests to verify failure**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=DashboardControllerTest test
```

Expected: FAIL because dashboard module does not exist.

- [ ] **Step 3: Implement dashboard read endpoints**

Implement read-only aggregation queries for current user summary, trends, project stats, sprint stats, task type distribution, member workload, and recent activities.

- [ ] **Step 4: Verify dashboard tests pass**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=DashboardControllerTest test
```

Expected: PASS. Dashboard endpoints return scoped data for the current user.

- [ ] **Step 5: Commit checkpoint**

```powershell
git add src/main/java/com/sdkanban/dashboard src/test/java/com/sdkanban/dashboard
git commit -m "feat: add dashboard statistics"
```

---

### Task 9: Frontend Auth, App Shell, Dashboard, And Projects

**Files:**
- Create: `web/src/api/http.ts`
- Create: `web/src/api/auth.ts`
- Create: `web/src/api/projects.ts`
- Create: `web/src/api/dashboard.ts`
- Create: `web/src/stores/auth.ts`
- Create: `web/src/stores/projects.ts`
- Create: `web/src/stores/dashboard.ts`
- Create: `web/src/views/LoginView.vue`
- Create: `web/src/views/DashboardView.vue`
- Create: `web/src/views/ProjectListView.vue`
- Create: `web/src/views/ProjectDetailView.vue`
- Modify: `web/src/router/index.ts`
- Modify: `web/src/App.vue`
- Test: `web/tests/auth-store.spec.ts`
- Test: `web/tests/dashboard-view.spec.ts`

- [ ] **Step 1: Write failing frontend auth and dashboard tests**

Create Vitest tests for:

```ts
// auth store saves token after login
// auth store clears token after logout
// router guard redirects anonymous users to login
// dashboard view renders my pending tasks and owned projects from mocked API
```

- [ ] **Step 2: Run frontend tests to verify failure**

Run:

```powershell
cd web
npm test -- --run auth-store.spec.ts dashboard-view.spec.ts
```

Expected: FAIL because stores and views do not exist.

- [ ] **Step 3: Implement frontend shell**

Implement Axios client with JWT interceptor, auth store, router guards, login view, dashboard view, project list, project detail, and shared layout navigation for Dashboard, Projects, My Tasks, and Logout.

- [ ] **Step 4: Verify frontend tests pass**

Run:

```powershell
cd web
npm test -- --run auth-store.spec.ts dashboard-view.spec.ts
npm run build
```

Expected: PASS. Frontend build succeeds and uses port `8102` config.

- [ ] **Step 5: Commit checkpoint**

```powershell
git add web/src web/tests
git commit -m "feat: add frontend auth and dashboard"
```

---

### Task 10: Frontend Boards, Task Drawer, And Drag-And-Drop

**Files:**
- Create: `web/src/api/board.ts`
- Create: `web/src/api/tasks.ts`
- Create: `web/src/stores/board.ts`
- Create: `web/src/stores/tasks.ts`
- Create: `web/src/views/ProjectBoardView.vue`
- Create: `web/src/views/MyTaskBoardView.vue`
- Create: `web/src/components/board/BoardColumn.vue`
- Create: `web/src/components/board/TaskCard.vue`
- Create: `web/src/components/board/BoardFilters.vue`
- Create: `web/src/components/task/TaskDrawer.vue`
- Test: `web/tests/board-store.spec.ts`
- Test: `web/tests/task-drawer.spec.ts`

- [ ] **Step 1: Write failing board component tests**

Create Vitest tests for:

```ts
// board store loads project board columns and cards
// board store loads my-task board cards
// moving a task calls PATCH /api/tasks/{id}/position
// failed move restores previous column state
// task drawer renders acceptance criteria, comments, tags, and activities
```

- [ ] **Step 2: Run frontend board tests to verify failure**

Run:

```powershell
cd web
npm test -- --run board-store.spec.ts task-drawer.spec.ts
```

Expected: FAIL because board stores and components do not exist.

- [ ] **Step 3: Implement board UI and task drawer**

Implement project board view, my-task board view, filters, column component, task card component, task drawer, drag-and-drop state updates, failed-update rollback, and task save refresh.

- [ ] **Step 4: Verify board UI tests pass**

Run:

```powershell
cd web
npm test -- --run board-store.spec.ts task-drawer.spec.ts
npm run build
```

Expected: PASS. Project board and my-task board behavior is covered by component and store tests.

- [ ] **Step 5: Commit checkpoint**

```powershell
git add web/src web/tests
git commit -m "feat: add frontend board workflows"
```

---

### Task 11: Unified Packaging And Local Run Scripts

**Files:**
- Modify: `pom.xml`
- Modify: `README.md`
- Create: `scripts/dev-backend.ps1`
- Create: `scripts/dev-frontend.ps1`
- Create: `scripts/package.ps1`
- Test: `src/test/java/com/sdkanban/PackagingSmokeTest.java`

- [ ] **Step 1: Write packaging smoke test**

Create a smoke test that verifies the Spring Boot static resource handler can serve `index.html` when frontend assets are present in `src/main/resources/static`.

- [ ] **Step 2: Run packaging test to verify failure**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository -Dtest=PackagingSmokeTest test
```

Expected: FAIL because frontend build output is not wired into backend resources.

- [ ] **Step 3: Wire Maven frontend build and scripts**

Configure Maven to run `npm install` and `npm run build` in `web/` during package, then copy `web/dist` into Spring Boot static resources before jar packaging. Add PowerShell scripts:

```powershell
# scripts/dev-backend.ps1
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository spring-boot:run

# scripts/dev-frontend.ps1
Set-Location web
npm run dev -- --host 0.0.0.0 --port 8102

# scripts/package.ps1
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository clean package
```

- [ ] **Step 4: Verify packaging**

Run:

```powershell
.\scripts\package.ps1
```

Expected: PASS. Maven creates a Spring Boot jar and includes frontend assets.

- [ ] **Step 5: Commit checkpoint**

```powershell
git add pom.xml README.md scripts src/test/java/com/sdkanban/PackagingSmokeTest.java
git commit -m "build: package frontend into spring boot jar"
```

---

### Task 12: End-To-End Verification

**Files:**
- Create: `web/e2e/sd-kanban.spec.ts`
- Create: `web/playwright.config.ts`
- Modify: `web/package.json`
- Modify: `README.md`

- [ ] **Step 1: Write failing Playwright scenarios**

Create E2E tests for:

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

- [ ] **Step 2: Run E2E tests to verify failure before final wiring**

Run:

```powershell
cd web
npx playwright test
```

Expected: FAIL if backend and frontend servers are not running or E2E test wiring is missing.

- [ ] **Step 3: Complete E2E wiring**

Configure Playwright base URL `http://localhost:8102`, document that the backend must run on `8101`, and add npm script `test:e2e`.

- [ ] **Step 4: Run full verification**

Run:

```powershell
mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository test
cd web
npm test -- --run
npm run build
npm run test:e2e
```

Expected: all backend tests pass, all frontend unit tests pass, frontend builds, and Playwright scenarios pass against local services.

- [ ] **Step 5: Commit checkpoint**

```powershell
git add web/e2e web/playwright.config.ts web/package.json README.md
git commit -m "test: add end-to-end kanban workflow"
```

---

## Self-Review Notes

Spec coverage:

- MySQL, Maven repository, backend port `8101`, frontend port `8102`: covered by Tasks 1 and 11.
- Auth and JWT: covered by Task 3.
- Project owner separate from task assignee: covered by Tasks 4 and 6.
- Custom project columns and default layout: covered by Task 5.
- Lightweight sprints: covered by Task 5.
- Rich task cards, comments, tags, activity logs: covered by Task 6.
- Project board and my-task board: covered by Tasks 7 and 10.
- Dashboard statistics: covered by Tasks 8 and 9.
- Unified jar packaging: covered by Task 11.
- End-to-end workflow: covered by Task 12.

Execution rule:

- Each behavior-changing task starts with failing tests.
- Each task includes the command to verify failure and the command to verify passing behavior.
- If Git remains unavailable, keep task checkpoints by recording changed files and command output in the implementation summary.
