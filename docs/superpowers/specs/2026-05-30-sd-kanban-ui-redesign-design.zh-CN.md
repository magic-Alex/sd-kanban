# SD Kanban UI 重设计规格

## 背景

当前 SD Kanban 已具备登录、项目、项目看板、我的任务、任务创建、任务详情、检查清单、归档、通知和用户管理等能力，但前端视觉仍偏基础，部分中文文案在文件和测试中出现乱码，移动端和小窗口下按钮密度与布局稳定性仍需要提升。

本次不再使用 Figma。设计参考 `UI UX Pro Max` 的可访问性、触控、响应式、表单反馈规则，并借鉴 `awesome-design-md` 的 DESIGN.md 方法，将视觉规则写入项目根目录 `DESIGN.md`，再直接落地到 Vue 前端。

## 目标

- 建立一套工程团队精确型工作台视觉语言。
- 恢复关键页面和测试中的中文文案。
- 优化登录、应用壳、仪表盘、项目看板、我的任务、任务创建弹窗、任务抽屉、通知面板、用户管理。
- 强化移动端可用性，避免按钮错位、文字溢出和页面级横向滚动。
- 不改变后端接口和业务语义。

## 非目标

- 不新增后端业务功能。
- 不引入重型 UI 框架。
- 不做暗色模式。
- 不做品牌营销页。

## 设计方向

采用“工程团队精确型工作台”：

- Linear 式低噪声与高精度布局。
- Airtable 式结构化数据表达。
- Notion 式柔和浅色工作表面。

主色为青绿色，状态色使用蓝、绿、琥珀、红、紫分担含义，避免单一蓝紫或深色面板统治全局。

## 范围

### 文档

- 新增根目录 `DESIGN.md`，作为后续 UI 改造的设计源文件。
- 新增实施计划文档，记录测试和代码改造步骤。

### 前端

- `web/src/styles/main.css`：重建全局设计 token、布局、组件、响应式。
- `web/src/App.vue`：修复中文文案，优化应用壳导航和账号区。
- `web/src/views/LoginView.vue`：重做登录页结构与说明文案。
- `web/src/views/DashboardView.vue`：修复中文文案，强化指标和趋势布局。
- `web/src/views/ProjectBoardView.vue`：修复中文文案，优化页头、视图切换、创建任务入口。
- `web/src/views/MyTaskBoardView.vue`：修复中文文案，优化个人任务看板。
- `web/src/views/UserAdminView.vue`：修复中文文案，优化创建用户和列表。
- `web/src/components/board/*`：优化过滤、列、任务卡文案和结构。
- `web/src/components/task/*`：优化创建弹窗、抽屉、检查清单、归档视图。
- `web/src/components/notification/NotificationPanel.vue`：修复文案与面板行为。

## 验收标准

- `npm test` 通过。
- `npm run build` 通过。
- 浏览器打开 `http://localhost:8102` 后，登录页、仪表盘、项目看板、我的任务、用户管理的中文显示正常。
- 小窗口和移动宽度下不出现主要按钮错位，不出现页面级横向滚动。
- 看板任务创建入口明显，任务卡显示类型、优先级、负责人、截止日期和清单进度。
