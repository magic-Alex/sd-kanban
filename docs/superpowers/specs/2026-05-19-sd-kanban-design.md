# SD Kanban Design Specification

## Goal

Build a complete agile development Kanban application for small teams using Spring Boot, Vue 3, Maven, npm, and MySQL. The first version focuses on daily team execution: projects, members, lightweight sprints, custom board columns, rich development task cards, project dashboards, and two Kanban views: project-wide and "my tasks".

## Confirmed Decisions

- Product direction: team daily execution Kanban.
- User model: basic multi-user login and project membership.
- Database: MySQL on `localhost:3306`, database name `sd_kanban`, local development username `root`, password `root`.
- Board columns: each project can customize columns; new projects receive a default column layout.
- Sprint support: lightweight sprints under each project.
- Task card depth: development task cards with type, priority, story points, estimates, acceptance criteria, comments, tags, and activity logs.
- Main UX structure: home dashboard first, then project detail and board pages.
- Reporting depth: agile statistics with sprint progress, task type distribution, member workload, and completion trends.
- Delivery mode: development uses separate Spring Boot and Vue dev servers; production builds Vue into the Spring Boot jar.
- Architecture approach: modular monolith.

## Runtime And Build Environment

The project will use Maven for the Spring Boot backend and npm for the Vue frontend. The local Maven repository is `D:\root\dev\Java\maven\repository`. Java, npm, and Maven are expected to be available from environment variables.

Development mode:

- Backend: Spring Boot on port `8101`.
- Frontend: Vite on port `8102`.
- Frontend requests proxy `/api/**` to `http://localhost:8101`.

Production mode:

- Maven runs backend tests and frontend build.
- Vue `dist` output is copied into Spring Boot static resources during packaging.
- The application starts with a single `java -jar` process.

## Architecture

The backend is a Spring Boot modular monolith. Modules are organized by business capability rather than technical layer alone:

- `auth` and `user`: registration, login, current user, BCrypt password hashing, JWT issuing and validation.
- `project` and `member`: project creation, project owner, project membership, member listing, member removal, owner transfer.
- `sprint`: lightweight sprint creation, status updates, date ranges, sprint filtering.
- `board`: custom project columns, default column bootstrap, column ordering, column deletion rules.
- `task`: development task cards, assignment, drag-and-drop movement, comments, tags, and activity logging.
- `dashboard`: read-only aggregated data for home dashboard, project stats, sprint stats, and my-task summaries.

The frontend is Vue 3 with Vite, Vue Router, Pinia, and Axios. Pages and stores follow the same business boundaries: auth, dashboard, projects, board, tasks, and stats.

The dashboard module is read-only. It aggregates data from projects, sprints, tasks, and activities but does not mutate business state.

## Data Model

Core tables:

- `users`: account, nickname, email, avatar URL, password hash, status, created time, updated time.
- `projects`: name, description, `owner_id`, creator id, status, created time, updated time.
- `project_members`: project id, user id, role, joined time. Roles are `owner` and `member`.
- `sprints`: project id, name, goal, start date, end date, status, created time, updated time.
- `board_columns`: project id, name, color, sort order, done-column flag, created time, updated time.
- `tasks`: project id, sprint id, column id, assignee id, creator id, title, description, task type, priority, story points, estimated hours, due date, acceptance criteria, sort order, status flags, created time, updated time.
- `task_tags`: project id, name, color.
- `task_tag_links`: task id, tag id.
- `task_comments`: task id, author id, content, created time, updated time.
- `task_activities`: task id, project id, actor id, action type, field name, old value, new value, created time.

Project owner and task assignee are intentionally separate:

- `projects.owner_id` is the project owner and can manage project settings, members, sprints, and board columns.
- `tasks.assignee_id` is the task owner responsible for a specific task.

When a project is created, the creator becomes the project owner and a project member. The system creates default board columns, such as backlog, ready, in progress, testing, and done. The project owner can rename, reorder, add, and remove columns after project creation. Removing a non-empty column is blocked unless tasks are moved first.

## Permissions

All project-scoped resources require project membership:

- Project members can view projects, sprints, boards, tasks, comments, activity logs, and statistics.
- Project members can create and update tasks, assign tasks to project members, comment, and move tasks between columns.
- Project owners can update project settings, transfer ownership, manage members, manage sprints, and manage board columns.
- A project owner must also be a project member.

This first version does not include global administrators or complex organization-level permissions.

## Pages And Interaction Design

The application starts with login and registration. After login, users land on the home dashboard.

Home dashboard:

- Shows my pending tasks, overdue tasks, projects I own, projects I participate in, recent activities, completion trend, sprint progress summaries, task type distribution, and member workload snapshots.
- Provides entry points to project list, project detail, project board, and my-task board.

Project list:

- Shows project name, project owner, member count, active sprint, task progress, and last activity.
- Allows creating a new project.

Project detail:

- Shows project profile, owner, members, sprint list, recent activities, and project statistics.
- Project owners can manage members, transfer owner, and manage sprints.

Project board mode:

- Displays all tasks in one project using that project's custom columns.
- Supports filters for sprint, assignee, task type, priority, and keyword.
- Supports drag-and-drop task movement between columns and ordering inside a column.
- Supports project owner column operations: add, rename, reorder, delete empty column.

My-task board mode:

- Displays tasks assigned to the current user across projects.
- Supports grouping by status/column or by project.
- Defaults to unfinished tasks, with filters for project, sprint, priority, due date, and task type.
- Uses the task's original project and column state, so changes are reflected in the project board as well.

Task detail drawer:

- Opens from both project board and my-task board without leaving the current page.
- Allows editing title, description, type, priority, story points, estimated hours, acceptance criteria, due date, assignee, sprint, and tags.
- Shows comments and activity logs.
- Saving a task refreshes the current board and relevant dashboard data.

## API Design

All backend endpoints use the `/api` prefix. The frontend uses Axios interceptors to attach JWT and handle standard error responses.

Authentication:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

Projects and members:

- `GET /api/projects`
- `POST /api/projects`
- `GET /api/projects/{projectId}`
- `PATCH /api/projects/{projectId}`
- `PATCH /api/projects/{projectId}/owner`
- `GET /api/projects/{projectId}/members`
- `POST /api/projects/{projectId}/members`
- `DELETE /api/projects/{projectId}/members/{userId}`

Sprints:

- `GET /api/projects/{projectId}/sprints`
- `POST /api/projects/{projectId}/sprints`
- `PATCH /api/projects/{projectId}/sprints/{sprintId}`
- `DELETE /api/projects/{projectId}/sprints/{sprintId}`

Board columns and board data:

- `GET /api/projects/{projectId}/board`
- `GET /api/projects/{projectId}/columns`
- `POST /api/projects/{projectId}/columns`
- `PATCH /api/projects/{projectId}/columns/{columnId}`
- `PATCH /api/projects/{projectId}/columns/order`
- `DELETE /api/projects/{projectId}/columns/{columnId}`

Tasks:

- `POST /api/tasks`
- `GET /api/tasks/{taskId}`
- `PATCH /api/tasks/{taskId}`
- `PATCH /api/tasks/{taskId}/position`
- `DELETE /api/tasks/{taskId}`
- `GET /api/tasks/mine`
- `GET /api/tasks/mine/board`

Task collaboration:

- `GET /api/tasks/{taskId}/comments`
- `POST /api/tasks/{taskId}/comments`
- `DELETE /api/tasks/{taskId}/comments/{commentId}`
- `GET /api/tasks/{taskId}/activities`
- `GET /api/projects/{projectId}/tags`
- `POST /api/projects/{projectId}/tags`
- `PATCH /api/projects/{projectId}/tags/{tagId}`
- `DELETE /api/projects/{projectId}/tags/{tagId}`

Dashboard and statistics:

- `GET /api/dashboard/summary`
- `GET /api/dashboard/trends`
- `GET /api/projects/{projectId}/stats`
- `GET /api/projects/{projectId}/sprints/{sprintId}/stats`

## Data Flow

Login flow:

1. User submits credentials to `POST /api/auth/login`.
2. Backend verifies BCrypt password and returns JWT plus current user data.
3. Frontend stores token and user profile in Pinia.
4. Subsequent requests attach `Authorization: Bearer <token>`.

Project board flow:

1. Frontend calls `GET /api/projects/{projectId}/board` with optional sprint, assignee, type, priority, and keyword filters.
2. Backend checks project membership, loads project columns, loads matching tasks, and returns grouped board data.
3. Frontend renders columns and cards.
4. Dragging a card calls `PATCH /api/tasks/{taskId}/position`.
5. Backend validates membership, updates column and sort order, writes a task activity, and returns updated task data.

My-task board flow:

1. Frontend calls `GET /api/tasks/mine/board`.
2. Backend loads tasks where `assignee_id` equals the current user and the user still belongs to the task's project.
3. Frontend renders the personal board grouped by status or project.
4. Updating a task uses the same task endpoints as the project board.

Dashboard flow:

1. Frontend calls dashboard summary and trends endpoints after login.
2. Backend aggregates projects, tasks, sprints, activities, and member workload for the current user.
3. Dashboard cards link into project board, project detail, or my-task board.

## Error Handling

Backend responses use a consistent error shape with code, message, and optional field errors.

- `400`: invalid input, including missing title, invalid date ranges, invalid story points, and malformed filters.
- `401`: missing or invalid JWT.
- `403`: user is not a project member or lacks project-owner permission.
- `404`: project, sprint, column, task, tag, or comment does not exist.
- `409`: business conflict, such as deleting a non-empty column, removing the current project owner from members, or assigning a task to a non-member.

Frontend behavior:

- Form errors appear near the relevant field.
- Global errors use a notification.
- Failed drag-and-drop updates roll the card back to its previous column and order.
- Expired login redirects to the login page.

## Security

Security is based on Spring Security and JWT:

- Passwords are stored with BCrypt only.
- JWT secret and expiration are configured in application properties.
- All `/api/**` endpoints except login and registration require authentication.
- Every project-scoped endpoint checks membership before reading or writing data.
- Project-owner-only operations are checked explicitly in service methods.
- Request DTOs use Bean Validation to block invalid data early.

The first version does not include refresh tokens, SSO, organization-level RBAC, or audit export.

## Testing Strategy

Backend tests:

- Unit and service tests for authentication, project creation, owner transfer, member checks, sprint lifecycle, column management, task updates, task positioning, and activity logging.
- Controller/API tests for login, protected endpoints, project membership authorization, project board data, my-task board data, and dashboard summaries.
- Repository or integration tests against MySQL-compatible behavior where query correctness matters.

Frontend tests:

- Store tests for auth state, board loading, task updates, dashboard loading, and error handling.
- Component tests for task cards, board columns, task detail drawer, filters, and dashboard widgets.
- End-to-end tests for login, project creation, task creation, task drag-and-drop, switching between project board and my-task board, and owner-only project settings.

Build verification:

- `mvn test`
- `npm test`
- `npm run build`
- Maven package build that includes the Vue output in the Spring Boot jar.

## Out Of Scope For First Version

- Full Scrum product backlog and sprint capacity planning.
- Burn-down charts, cycle time, throughput, and bottleneck analysis.
- Global admin console and organization hierarchy.
- Binary file upload storage. Attachment links can be represented as URL/text fields if needed.
- Real-time collaboration through WebSocket.
- Mobile-native app.

## Scope Check

This design is suitable for a single implementation plan because it produces one coherent application with one backend, one frontend, one database schema, and clear module boundaries. The implementation plan should still split work into small vertical slices: project scaffold, auth, projects and members, sprints and columns, tasks and boards, dashboards, frontend pages, packaging, and end-to-end verification.
