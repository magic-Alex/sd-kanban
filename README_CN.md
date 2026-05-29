# SD Kanban

SD Kanban 是一个基于 Spring Boot 3 + Vue 3 的敏捷看板应用，适用于项目负责人和小型交付团队。它包含 JWT 登录、项目、项目负责人、成员、迭代、自定义看板列、任务、检查清单、任务评论与 @ 通知、归档与恢复、项目看板与个人看板、仪表盘统计，以及 MySQL 数据持久化等功能。

## 后端

- Java 17
- Spring Boot 后端默认端口：`8101`
- MySQL 数据库：`sd_kanban`
- 本地 MySQL 账号密码：`root` / `root`

使用项目指定的 Maven 仓库和 Java 17 运行后端测试：

```powershell
$env:JAVA_HOME='D:\root\dev\Java\jdk\jdk17'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; mvn -Dmaven.repo.local=D:\root\dev\Java\maven\repository test
```

启动后端：

```powershell
.\scripts\dev-backend.ps1
```

运行时配置可以通过环境变量提供。本地默认值会保持现有开发环境可正常运行。

| 变量 | 默认值 | 用途 |
| --- | --- | --- |
| `SERVER_PORT` | `8101` | Spring Boot HTTP 端口 |
| `DB_URL` | 由 `DB_HOST`、`DB_PORT` 和 `DB_NAME` 构建 | 完整 JDBC URL 覆盖配置 |
| `DB_HOST` | `localhost` | 未设置 `DB_URL` 时使用的 MySQL 主机 |
| `DB_PORT` | `3306` | 未设置 `DB_URL` 时使用的 MySQL 端口 |
| `DB_NAME` | `sd_kanban` | 未设置 `DB_URL` 时使用的 MySQL 数据库名 |
| `DB_USERNAME` | `root` | MySQL 用户名 |
| `DB_PASSWORD` | `root` | MySQL 密码 |
| `DB_TIMEZONE` | `Asia/Shanghai` | JDBC 服务器时区 |
| `DB_CREATE_DATABASE_IF_NOT_EXIST` | `true` | 本地数据库自动创建开关 |
| `JPA_DDL_AUTO` | `validate` | Hibernate schema 行为 |
| `FLYWAY_ENABLED` | `true` | Flyway 迁移开关 |
| `JWT_SECRET` | 本地开发密钥 | JWT 签名密钥；生产环境请使用较长的随机值 |
| `JWT_EXPIRES_MINUTES` | `720` | JWT 有效期，单位为分钟 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:8102` | 允许调用 `/api/**` 的浏览器来源，多个来源用英文逗号分隔 |
| `MAVEN_REPO_LOCAL` | `D:\root\dev\Java\maven\repository` | 脚本使用的 Maven 本地仓库 |

## 前端

- Vue 3 + Vite
- 开发服务器默认端口：`8102`
- `/api` 代理到 `http://localhost:8101`

```powershell
cd web
npm install
npm test -- --run
npm run build
```

启动前端开发服务器：

```powershell
.\scripts\dev-frontend.ps1
```

前端开发环境变量：

| 变量 | 默认值 | 用途 |
| --- | --- | --- |
| `FRONTEND_HOST` | `0.0.0.0` | 开发脚本传入的主机地址 |
| `FRONTEND_PORT` | `8102` | Vite 开发服务器端口 |
| `FRONTEND_URL` | `http://localhost:8102` | 用于推断开发服务器端口的 URL |
| `API_PROXY_TARGET` | `http://localhost:8101` | Vite `/api` 代理目标地址 |

## 打包

Maven 的 package 阶段会运行 Vue 构建，并将 `web/dist` 复制到 Spring Boot jar 的 `static/` 目录下。

```powershell
.\scripts\package.ps1
```

打包后的 jar 会生成在 `target/` 目录下。

## 端到端测试

Playwright 端到端测试使用 `http://localhost:8102` 上的前端，并要求后端运行在 `http://localhost:8101`。

测试会在 MySQL 中创建临时账号、项目、任务、检查项、评论、归档/恢复记录和通知相关记录，并在运行结束后删除这些记录。

终端 1：

```powershell
.\scripts\dev-backend.ps1
```

终端 2：

```powershell
cd web
npx playwright install chromium
npm run test:e2e
```

可选覆盖配置：

```powershell
$env:E2E_BACKEND_URL='http://localhost:8101'
$env:E2E_FRONTEND_URL='http://localhost:8102'
$env:E2E_DB_HOST='localhost'
$env:E2E_DB_PORT='3306'
$env:E2E_DB_USER='root'
$env:E2E_DB_PASSWORD='root'
$env:E2E_DB_NAME='sd_kanban'
```

`E2E_BACKEND_URL` 也会在 Playwright 运行期间被 Vite `/api` 代理使用。
