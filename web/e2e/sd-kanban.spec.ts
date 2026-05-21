import { expect, test } from '@playwright/test'

const backendUrl = 'http://localhost:8101'
const frontendUrl = 'http://localhost:8102'

test('runs the core kanban workflow', async ({ page, request }) => {
  const suffix = Date.now()
  const account = `e2e-${suffix}`
  const nickname = `E2E ${suffix}`
  const password = 'secret123'
  const projectName = `E2E Project ${suffix}`
  const taskTitle = `Drag task ${suffix}`
  const commentText = `Comment ${suffix}`

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

  const projectId = Number(page.url().match(/projects\/(\d+)/)?.[1])
  expect(projectId).toBeGreaterThan(0)

  const sprint = await request.post(`${backendUrl}/api/projects/${projectId}/sprints`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      name: `Sprint ${suffix}`,
      startDate: '2026-05-21',
      endDate: '2026-06-04',
    },
  })
  expect(sprint.ok()).toBeTruthy()
  const sprintId = (await sprint.json()).data.id as number

  const columns = await request.get(`${backendUrl}/api/projects/${projectId}/columns`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  expect(columns.ok()).toBeTruthy()
  const columnList = (await columns.json()).data as Array<{ id: number; name: string }>
  const readyColumn = columnList.find((column) => column.name === 'Ready')
  const inProgressColumn = columnList.find((column) => column.name === 'In Progress')
  expect(readyColumn).toBeTruthy()
  expect(inProgressColumn).toBeTruthy()

  const task = await request.post(`${backendUrl}/api/projects/${projectId}/tasks`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      title: taskTitle,
      columnId: readyColumn!.id,
      assigneeId: userId,
      sprintId,
      taskType: 'STORY',
      priority: 'HIGH',
      acceptanceCriteria: 'Task can move and accept comments',
    },
  })
  expect(task.ok()).toBeTruthy()

  await page.goto(`${frontendUrl}/projects/${projectId}/board`)
  await expect(page.getByText(taskTitle)).toBeVisible()

  await page
    .locator('.task-card', { hasText: taskTitle })
    .dragTo(page.locator('.board-column', { hasText: 'In Progress' }))
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
})
