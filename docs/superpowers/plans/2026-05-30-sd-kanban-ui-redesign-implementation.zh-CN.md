# SD Kanban UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the Vue frontend into a polished engineering kanban workspace while fixing garbled Chinese UI strings.

**Architecture:** Keep the existing Vue + Pinia + API boundaries. Implement the redesign through global design tokens, focused view/component markup updates, and Vitest coverage for labels and core interactions.

**Tech Stack:** Vue 3, Pinia, Vue Router, Vitest, Vue Test Utils, Vite, CSS.

---

### Task 1: Design Source And UI Text Tests

**Files:**
- Create: `DESIGN.md`
- Create: `docs/superpowers/specs/2026-05-30-sd-kanban-ui-redesign-design.zh-CN.md`
- Modify: `web/tests/app.spec.ts`
- Modify: `web/tests/dashboard-view.spec.ts`
- Modify: `web/tests/task-card.spec.ts`
- Modify: `web/tests/user-admin-view.spec.ts`

- [ ] **Step 1: Write failing tests for readable Chinese shell and page labels**

Update tests to assert readable Chinese strings:

```ts
expect(wrapper.text()).toContain('仪表盘')
expect(wrapper.text()).toContain('项目')
expect(wrapper.text()).toContain('我的任务')
expect(wrapper.text()).toContain('用户管理')
expect(wrapper.text()).toContain('退出')
```

- [ ] **Step 2: Run the focused tests and verify failure**

Run:

```powershell
npm test -- app.spec.ts dashboard-view.spec.ts task-card.spec.ts user-admin-view.spec.ts
```

Expected: tests fail while components still render garbled strings.

- [ ] **Step 3: Fix UI text in the application shell and major views**

Replace garbled labels in `App.vue`, `DashboardView.vue`, `TaskCard.vue`, and `UserAdminView.vue` with readable Chinese.

- [ ] **Step 4: Run the same tests and verify pass**

Run:

```powershell
npm test -- app.spec.ts dashboard-view.spec.ts task-card.spec.ts user-admin-view.spec.ts
```

Expected: all focused tests pass.

### Task 2: Board And Task Workflow UI

**Files:**
- Modify: `web/tests/project-board-view.spec.ts`
- Modify: `web/tests/task-drawer.spec.ts`
- Modify: `web/src/views/ProjectBoardView.vue`
- Modify: `web/src/views/MyTaskBoardView.vue`
- Modify: `web/src/components/board/BoardFilters.vue`
- Modify: `web/src/components/board/BoardColumn.vue`
- Modify: `web/src/components/task/TaskCreateModal.vue`
- Modify: `web/src/components/task/TaskDrawer.vue`
- Modify: `web/src/components/task/TaskChecklist.vue`
- Modify: `web/src/components/task/ArchivedTaskList.vue`

- [ ] **Step 1: Update tests to assert readable Chinese board workflow labels**

Use labels such as `新增任务`, `创建看板任务`, `任务负责人`, `检查清单`, `恢复`, `已归档任务`.

- [ ] **Step 2: Run focused board tests and verify failure**

Run:

```powershell
npm test -- project-board-view.spec.ts task-drawer.spec.ts
```

Expected: tests fail while the board workflow still has garbled labels.

- [ ] **Step 3: Fix board and task workflow labels**

Replace garbled labels and aria labels in the board, task modal, drawer, checklist, and archived task components.

- [ ] **Step 4: Run focused board tests and verify pass**

Run:

```powershell
npm test -- project-board-view.spec.ts task-drawer.spec.ts
```

Expected: focused board workflow tests pass.

### Task 3: Visual System CSS

**Files:**
- Modify: `web/src/styles/main.css`

- [ ] **Step 1: Replace raw styling with design tokens**

Define `:root` variables for color, radius, shadow, spacing, z-index, and motion:

```css
:root {
  --color-canvas: #f6f8fb;
  --color-surface: #ffffff;
  --color-primary: #0f766e;
  --radius-md: 8px;
  --shadow-md: 0 18px 40px rgba(15, 23, 42, 0.12);
}
```

- [ ] **Step 2: Redesign shared primitives**

Update buttons, inputs, page headers, panels, metric cards, sidebar, board columns, task cards, modals, drawers, notifications, user rows, and responsive states.

- [ ] **Step 3: Run frontend tests**

Run:

```powershell
npm test
```

Expected: all frontend unit tests pass.

### Task 4: Browser Verification

**Files:**
- No source files unless visual verification finds issues.

- [ ] **Step 1: Build the frontend**

Run:

```powershell
npm run build
```

Expected: Vite build succeeds.

- [ ] **Step 2: Open local app in the browser**

Use the running frontend at `http://localhost:8102` or start it with:

```powershell
npm run dev -- --host 127.0.0.1 --port 8102
```

Expected: login page renders readable Chinese and the redesign is visible.

- [ ] **Step 3: Verify desktop and mobile widths**

Check login, dashboard, project board, task modal/drawer, my tasks, and user management at desktop and mobile widths. Confirm no page-level horizontal scroll and no overlapping buttons.
