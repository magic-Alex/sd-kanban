import { expect, test } from '@playwright/test'
import { createConnection, type RowDataPacket } from 'mysql2/promise'

const backendUrl = process.env.E2E_BACKEND_URL ?? 'http://localhost:8101'
const frontendUrl = process.env.E2E_FRONTEND_URL ?? 'http://localhost:8102'
const authHeaders = (token: string) => ({ Authorization: `Bearer ${token}` })

type ProjectRow = RowDataPacket & { id: number }
type TaskApiResponse = {
  id: number
  title: string
  description: string | null
  storyPoints: number | string | null
  estimatedHours: number | string | null
  columnId: number
}
type ProjectBoardApiResponse = {
  columns: Array<{
    id: number
    name: string
    tasks: Array<{ id: number; title: string; columnId: number }>
  }>
}

function placeholders(values: unknown[]) {
  return values.map(() => '?').join(', ')
}

async function cleanupE2eData(account: string, projectId: number | null) {
  const connection = await createConnection({
    host: process.env.E2E_DB_HOST ?? 'localhost',
    port: Number(process.env.E2E_DB_PORT ?? '3306'),
    user: process.env.E2E_DB_USER ?? 'root',
    password: process.env.E2E_DB_PASSWORD ?? 'root',
    database: process.env.E2E_DB_NAME ?? 'sd_kanban',
  })

  try {
    await connection.beginTransaction()
    const [projectRows] = projectId
      ? await connection.query<ProjectRow[]>(
          `SELECT DISTINCT projects.id
             FROM projects
             JOIN users ON users.id IN (projects.owner_id, projects.creator_id)
            WHERE projects.id = ?
              AND users.account = ?`,
          [projectId, account],
        )
      : await connection.query<ProjectRow[]>(
          `SELECT DISTINCT projects.id
             FROM projects
             JOIN users ON users.id IN (projects.owner_id, projects.creator_id)
            WHERE users.account = ?`,
          [account],
        )

    const projectIds = projectRows.map((project) => Number(project.id))
    if (projectIds.length > 0) {
      const ids = placeholders(projectIds)
      await connection.query(`DELETE FROM task_tag_links WHERE project_id IN (${ids})`, projectIds)
      await connection.query(
        `DELETE task_comments
           FROM task_comments
           JOIN tasks ON tasks.id = task_comments.task_id
          WHERE tasks.project_id IN (${ids})`,
        projectIds,
      )
      await connection.query(`DELETE FROM task_activities WHERE project_id IN (${ids})`, projectIds)
      await connection.query(`DELETE FROM tasks WHERE project_id IN (${ids})`, projectIds)
      await connection.query(`DELETE FROM task_tags WHERE project_id IN (${ids})`, projectIds)
      await connection.query(`DELETE FROM sprints WHERE project_id IN (${ids})`, projectIds)
      await connection.query(`DELETE FROM board_columns WHERE project_id IN (${ids})`, projectIds)
      await connection.query(`DELETE FROM project_members WHERE project_id IN (${ids})`, projectIds)
      await connection.query(`DELETE FROM projects WHERE id IN (${ids})`, projectIds)
    }
    await connection.query('DELETE FROM users WHERE account = ?', [account])
    await connection.commit()
  } catch (error) {
    await connection.rollback()
    throw error
  } finally {
    await connection.end()
  }
}

test('runs the core kanban workflow', async ({ page, request }) => {
  const suffix = Date.now()
  const account = `e2e-${suffix}`
  const nickname = `E2E ${suffix}`
  const password = 'secret123'
  const projectName = `E2E Project ${suffix}`
  const taskTitle = `Drag task ${suffix}`
  const updatedTaskTitle = `${taskTitle} updated`
  const commentText = `Comment ${suffix}`
  let workflowError: unknown
  let projectId: number | null = null

  try {
    const registration = await request.post(`${backendUrl}/api/auth/register`, {
      data: {
        account,
        nickname,
        email: `${account}@example.com`,
        password,
      },
    })
    expect(registration.ok()).toBeTruthy()
    const registrationBody = await registration.json()
    const token = registrationBody.data.token as string
    const userId = registrationBody.data.user.id as number

    await page.goto(`${frontendUrl}/login`)
    await page.getByLabel('账号').fill(account)
    await page.getByLabel('密码').fill(password)
    await page.getByRole('button', { name: '登录' }).click()
    await expect(page.getByRole('heading', { name: '仪表盘' })).toBeVisible()

    await page.locator('.primary-nav').getByRole('link', { name: '项目' }).click()
    await page.getByLabel('名称').fill(projectName)
    await page.getByLabel('描述').fill('Created by Playwright')
    await page.getByRole('button', { name: '创建项目' }).click()
    await expect(page.getByRole('heading', { name: projectName })).toBeVisible()
    await expect(page.locator('.panel-block', { hasText: '项目负责人' }).getByText(nickname)).toBeVisible()

    projectId = Number(page.url().match(/projects\/(\d+)/)?.[1])
    expect(projectId).toBeGreaterThan(0)

    const sprint = await request.post(`${backendUrl}/api/projects/${projectId}/sprints`, {
      headers: authHeaders(token),
      data: {
        name: `Sprint ${suffix}`,
        startDate: '2026-05-21',
        endDate: '2026-06-04',
      },
    })
    expect(sprint.ok()).toBeTruthy()
    const sprintId = (await sprint.json()).data.id as number

    const columns = await request.get(`${backendUrl}/api/projects/${projectId}/columns`, {
      headers: authHeaders(token),
    })
    expect(columns.ok()).toBeTruthy()
    const columnList = (await columns.json()).data as Array<{ id: number; name: string }>
    const readyColumn = columnList.find((column) => column.name === 'Ready')
    const inProgressColumn = columnList.find((column) => column.name === 'In Progress')
    const doneColumn = columnList.find((column) => column.name === 'Done')
    expect(readyColumn).toBeTruthy()
    expect(inProgressColumn).toBeTruthy()
    expect(doneColumn).toBeTruthy()

    const task = await request.post(`${backendUrl}/api/projects/${projectId}/tasks`, {
      headers: authHeaders(token),
      data: {
        title: taskTitle,
        columnId: readyColumn!.id,
        assigneeId: userId,
        sprintId,
        description: 'Initial task description',
        taskType: 'STORY',
        priority: 'HIGH',
        acceptanceCriteria: 'Task can move and accept comments',
      },
    })
    expect(task.ok()).toBeTruthy()
    const taskId = (await task.json()).data.id as number

    await page.goto(`${frontendUrl}/projects/${projectId}/board`)
    await expect(page.getByText(taskTitle)).toBeVisible()

    const moveResponsePromise = page.waitForResponse(
      (response) => response.url().includes(`/api/tasks/${taskId}/position`) && response.request().method() === 'PATCH',
    )
    await page
      .locator('.task-card', { hasText: taskTitle })
      .dragTo(page.locator('.board-column', { hasText: 'In Progress' }))
    const moveResponse = await moveResponsePromise
    expect(moveResponse.ok()).toBeTruthy()
    await expect(page.locator('.board-column', { hasText: 'In Progress' }).getByText(taskTitle)).toBeVisible()

    const movedTask = await request.get(`${backendUrl}/api/tasks/${taskId}`, {
      headers: authHeaders(token),
    })
    expect(movedTask.ok()).toBeTruthy()
    expect((await movedTask.json()).data.columnId).toBe(inProgressColumn!.id)
    await page.reload()
    await expect(page.locator('.board-column', { hasText: 'In Progress' }).getByText(taskTitle)).toBeVisible()

    await page.locator('.primary-nav').getByRole('link', { name: '我的任务' }).click()
    await expect(page.getByText(taskTitle)).toBeVisible()
    await page.getByText(taskTitle).click()
    await expect(page.getByText('Task can move and accept comments')).toBeVisible()

    await page.getByLabel('新增评论').fill(commentText)
    await page.getByRole('button', { name: '添加评论' }).click()
    await expect(page.getByText(commentText)).toBeVisible()
    await page.getByRole('button', { name: '关闭' }).click()

    await page.locator('.primary-nav').getByRole('link', { name: '仪表盘' }).click()
    await expect(page.locator('.metric-card', { hasText: '待处理任务' }).getByText('1')).toBeVisible()

    await page.goto(`${frontendUrl}/projects/${projectId}/board`)
    await expect(page.locator('.board-column', { hasText: 'In Progress' }).getByText(taskTitle)).toBeVisible()
    await page.locator('.task-card', { hasText: taskTitle }).click()
    await page.getByRole('button', { name: '编辑任务' }).click()
    await page.getByLabel('编辑任务标题').fill(updatedTaskTitle)
    await page.getByLabel('编辑任务描述').fill('')
    await page.getByLabel('编辑故事点').fill('2')
    await page.getByLabel('编辑预计工时').fill('6')
    const editResponsePromise = page.waitForResponse(
      (response) =>
        response.url().includes(`/api/tasks/${taskId}`) &&
        !response.url().includes('/position') &&
        !response.url().includes('/archive') &&
        response.request().method() === 'PATCH',
    )
    await page.getByRole('button', { name: '保存任务' }).click()
    const editResponse = await editResponsePromise
    expect(editResponse.ok(), await editResponse.text()).toBeTruthy()
    await expect(page.locator('.task-drawer')).toContainText(updatedTaskTitle)

    const editedTaskResponse = await request.get(`${backendUrl}/api/tasks/${taskId}`, {
      headers: authHeaders(token),
    })
    expect(editedTaskResponse.ok()).toBeTruthy()
    const editedTask = (await editedTaskResponse.json()).data as TaskApiResponse
    expect(editedTask.title).toBe(updatedTaskTitle)
    expect(editedTask.description).toBeNull()
    expect(Number(editedTask.storyPoints)).toBe(2)
    expect(Number(editedTask.estimatedHours)).toBe(6)

    const completeResponsePromise = page.waitForResponse(
      (response) => response.url().includes(`/api/tasks/${taskId}/position`) && response.request().method() === 'PATCH',
    )
    await page.getByRole('button', { name: '标记完成' }).click()
    const completeResponse = await completeResponsePromise
    expect(completeResponse.ok()).toBeTruthy()
    await page.getByRole('button', { name: '关闭' }).click()
    await expect(page.locator('.board-column', { hasText: 'Done' }).getByText(updatedTaskTitle)).toBeVisible()

    const completedTaskResponse = await request.get(`${backendUrl}/api/tasks/${taskId}`, {
      headers: authHeaders(token),
    })
    expect(completedTaskResponse.ok()).toBeTruthy()
    expect(((await completedTaskResponse.json()).data as TaskApiResponse).columnId).toBe(doneColumn!.id)

    await page.locator('.task-card', { hasText: updatedTaskTitle }).click()
    const archiveResponsePromise = page.waitForResponse(
      (response) => response.url().includes(`/api/tasks/${taskId}/archive`) && response.request().method() === 'PATCH',
    )
    await page.getByRole('button', { name: '归档任务' }).click()
    const archiveResponse = await archiveResponsePromise
    expect(archiveResponse.ok()).toBeTruthy()
    await expect(page.locator('.task-card', { hasText: updatedTaskTitle })).toHaveCount(0)

    const archivedBoardResponse = await request.get(`${backendUrl}/api/projects/${projectId}/board`, {
      headers: authHeaders(token),
    })
    expect(archivedBoardResponse.ok()).toBeTruthy()
    const archivedBoard = (await archivedBoardResponse.json()).data as ProjectBoardApiResponse
    const archivedBoardTaskIds = archivedBoard.columns.flatMap((column) => column.tasks.map((taskItem) => taskItem.id))
    expect(archivedBoardTaskIds).not.toContain(taskId)
  } catch (error) {
    workflowError = error
    throw error
  } finally {
    try {
      await cleanupE2eData(account, projectId)
    } catch (cleanupError) {
      if (!workflowError) {
        throw cleanupError
      }
      console.warn('E2E cleanup failed after workflow failure:', cleanupError)
    }
  }
})
