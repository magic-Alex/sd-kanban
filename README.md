# SD Kanban

SD Kanban is a Spring Boot 3 + Vue 3 agile kanban application for project owners and small delivery teams. It includes JWT login, projects, project owners, members, sprints, custom board columns, tasks, project and personal boards, dashboard statistics, and MySQL persistence.

## Backend

- Java 17
- Spring Boot backend port: `8101`
- MySQL database: `sd_kanban`
- Local credentials: `root` / `root`

Run backend tests with the project Maven repository and Java 17:

```powershell
$env:JAVA_HOME='D:\root\dev\Java\jdk\jdk17'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository test
```

Start the backend:

```powershell
.\scripts\dev-backend.ps1
```

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

## Package

The Maven package phase runs the Vue build and copies `web/dist` into the Spring Boot jar under `static/`.

```powershell
.\scripts\package.ps1
```

The packaged jar is created under `target/`.
