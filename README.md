# SD Kanban

SD Kanban is a Spring Boot 3 + Vue 3 agile kanban application for project owners and small delivery teams. It includes JWT login, projects, project owners, members, sprints, a global board template, tasks, project and personal boards, dashboard statistics, and MySQL persistence.

## Core Workflow

- Default ports: backend `8101`, frontend `8102`.
- Public self-registration is disabled; create the first administrator through your deployment or database bootstrap process, then manage users from the user management page.
- Projects require a unique project code and a project color. The code appears on task cards, and the color helps distinguish tasks from different projects in the personal board.
- All projects use the same global board-column template managed from System Settings. The default statuses are `待办（Backlog）`, `就绪（Ready）`, `进行中（In Progress）`, `测试（Testing）`, and `完成（Done）`.
- The personal task board shows only tasks assigned to the current user. Cards can be dragged between global template statuses while keeping their original project binding.
- Project owners add project members from the project detail page. Tasks can only be assigned to users who are already members of that project.

## Backend

- Java 17
- Spring Boot backend port: `8101`
- MySQL database: `sd_kanban`
- Local credentials: `root` / `root`
- Public self-registration is disabled by default; create the first administrator through your deployment or database bootstrap process.

Run backend tests with the project Maven repository and Java 17:

```powershell
$env:JAVA_HOME='D:\root\dev\Java\jdk\jdk17'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository test
```

Start the backend:

```powershell
.\scripts\dev-backend.ps1
```

Runtime configuration can be supplied through environment variables. Local defaults keep the existing development setup working.

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8101` | Spring Boot HTTP port |
| `DB_URL` | Built from `DB_HOST`, `DB_PORT`, and `DB_NAME` | Full JDBC URL override |
| `DB_HOST` | `localhost` | MySQL host when `DB_URL` is not set |
| `DB_PORT` | `3306` | MySQL port when `DB_URL` is not set |
| `DB_NAME` | `sd_kanban` | MySQL database name when `DB_URL` is not set |
| `DB_USERNAME` | `root` | MySQL user |
| `DB_PASSWORD` | `root` | MySQL password |
| `DB_TIMEZONE` | `Asia/Shanghai` | JDBC server timezone |
| `DB_CREATE_DATABASE_IF_NOT_EXIST` | `true` | Local database auto-create switch |
| `JPA_DDL_AUTO` | `validate` | Hibernate schema behavior |
| `FLYWAY_ENABLED` | `true` | Flyway migration switch |
| `REGISTRATION_ENABLED` | `false` | Public `/api/auth/register` switch; keep disabled for normal use |
| `JWT_SECRET` | Local development secret | JWT signing secret; use a long random value in production |
| `JWT_EXPIRES_MINUTES` | `720` | JWT lifetime in minutes |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:8102,http://127.0.0.1:8102` | Comma-separated browser origins allowed to call `/api/**` |
| `MAVEN_REPO_LOCAL` | `D:\root\dev\Java\maven\repository` | Maven local repository used by scripts |

## Frontend

- Vue 3 with Vite
- Dev server port: `8102`
- `/api` proxies to `http://localhost:8101`

```powershell
cd web
npm install
npm test -- --run
npm run build
```

Start the frontend dev server:

```powershell
.\scripts\dev-frontend.ps1
```

Frontend development variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `FRONTEND_HOST` | `0.0.0.0` | Host passed by the dev script |
| `FRONTEND_PORT` | `8102` | Vite dev server port |
| `FRONTEND_URL` | `http://localhost:8102` | URL used to infer the dev server port |
| `API_PROXY_TARGET` | `http://localhost:8101` | Vite `/api` proxy target |

## Package

The Maven package phase runs the Vue build and copies `web/dist` into the Spring Boot jar under `static/`.

```powershell
.\scripts\package.ps1
```

The packaged jar is created under `target/`.

## End-To-End Tests

Playwright E2E tests use the frontend at `http://localhost:8102` and require the backend to be running at `http://localhost:8101`.
The test creates temporary account, project, task, and comment records in MySQL and removes them after the run.

Terminal 1:

```powershell
.\scripts\dev-backend.ps1
```

Terminal 2:

```powershell
cd web
npx playwright install chromium
npm run test:e2e
```

Optional overrides:

```powershell
$env:E2E_BACKEND_URL='http://localhost:8101'
$env:E2E_FRONTEND_URL='http://localhost:8102'
$env:E2E_DB_HOST='localhost'
$env:E2E_DB_PORT='3306'
$env:E2E_DB_USER='root'
$env:E2E_DB_PASSWORD='root'
$env:E2E_DB_NAME='sd_kanban'
```

`E2E_BACKEND_URL` is also used by the Vite `/api` proxy during the Playwright run.
