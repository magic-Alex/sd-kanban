import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProjectBoardView from '../src/views/ProjectBoardView.vue'
import TaskDrawer from '../src/components/task/TaskDrawer.vue'
import { fetchProjectBoard } from '../src/api/board'
import { fetchProjectMembers } from '../src/api/projects'
import {
  createTask,
  fetchArchivedTasks,
  fetchTask,
  restoreTask,
  updateTask,
  updateTaskPosition,
} from '../src/api/tasks'
import { useBoardStore } from '../src/stores/board'
import { useTasksStore } from '../src/stores/tasks'

const mockedRoute = vi.hoisted(() => ({
  value: undefined as unknown as {
    params: { projectId: string }
    query: Record<string, string>
  },
}))

vi.mock('vue-router', async () => {
  const { reactive } = await vi.importActual<typeof import('vue')>('vue')
  mockedRoute.value = reactive({
    params: { projectId: '7' },
    query: {},
  })
  return {
    useRoute: () => mockedRoute.value,
  }
})

vi.mock('../src/api/board', () => ({
  fetchProjectBoard: vi.fn(),
  fetchMyTaskBoard: vi.fn(),
}))

vi.mock('../src/api/projects', () => ({
  fetchProjectMembers: vi.fn(),
}))

vi.mock('../src/api/tasks', () => ({
  addTaskComment: vi.fn(),
  createTask: vi.fn(),
  fetchArchivedTasks: vi.fn(),
  fetchTask: vi.fn(),
  fetchTaskActivities: vi.fn().mockResolvedValue([]),
  fetchTaskComments: vi.fn().mockResolvedValue([]),
  restoreTask: vi.fn(),
  updateTask: vi.fn(),
  updateTaskPosition: vi.fn(),
}))

vi.mock('../src/api/checklist', () => ({
  createChecklistItem: vi.fn(),
  deleteChecklistItem: vi.fn(),
  fetchChecklistItems: vi.fn().mockResolvedValue([]),
  reorderChecklistItems: vi.fn(),
  toggleChecklistItem: vi.fn(),
  updateChecklistItem: vi.fn(),
}))

const projectBoard = {
  projectId: 7,
  columns: [
    {
      id: 1,
      name: 'Backlog',
      color: '#64748b',
      sortOrder: 0,
      isDone: false,
      tasks: [],
    },
    {
      id: 2,
      name: 'Ready',
      color: '#0ea5e9',
      sortOrder: 1,
      isDone: false,
      tasks: [],
    },
  ],
}

const members = [
  {
    user: { id: 11, account: 'admin', nickname: 'Admin', email: null, avatarUrl: null },
    role: 'owner',
    joinedAt: '2026-05-28T10:00:00',
  },
]

const createdTask = {
  id: 55,
  projectId: 7,
  sprintId: null,
  columnId: 1,
  assignee: members[0].user,
  creator: members[0].user,
  title: '补齐看板创建任务',
  description: '从看板直接创建任务',
  taskType: 'BUG',
  priority: 'HIGH',
  storyPoints: 3,
  estimatedHours: 4,
  dueDate: '2026-06-01',
  acceptanceCriteria: '任务创建后出现在 Backlog',
  sortOrder: 0,
  tags: [],
  createdAt: '2026-05-28T10:00:00',
  updatedAt: '2026-05-28T10:00:00',
}

const archivedTask = {
  ...createdTask,
  id: 77,
  title: 'Archived task',
  assignee: null,
  priority: 'LOW',
  taskType: 'TASK',
}

const currentBoardTask = {
  id: 88,
  title: 'Current board task',
  priority: 'MEDIUM',
  taskType: 'STORY',
  assignee: null,
  sortOrder: 0,
  storyPoints: null,
  dueDate: null,
  checklistTotalCount: 0,
  checklistDoneCount: 0,
}

function deferred<T>() {
  let resolve: (value: T) => void = () => undefined
  let reject: (reason?: unknown) => void = () => undefined
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, resolve, reject }
}

enableAutoUnmount(afterEach)

describe('ProjectBoardView', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    mockedRoute.value.params.projectId = '7'
    mockedRoute.value.query = {}
    setActivePinia(createPinia())
    vi.mocked(fetchProjectBoard).mockReset()
    vi.mocked(fetchProjectMembers).mockReset()
    vi.mocked(createTask).mockReset()
    vi.mocked(fetchArchivedTasks).mockReset()
    vi.mocked(fetchTask).mockReset()
    vi.mocked(restoreTask).mockReset()
    vi.mocked(updateTask).mockReset()
    vi.mocked(updateTaskPosition).mockReset()
    vi.mocked(fetchProjectBoard).mockResolvedValue(projectBoard)
    vi.mocked(fetchProjectMembers).mockResolvedValue(members)
    vi.mocked(createTask).mockResolvedValue(createdTask)
    vi.mocked(fetchArchivedTasks).mockResolvedValue([archivedTask])
    vi.mocked(fetchTask).mockResolvedValue(createdTask)
    vi.mocked(restoreTask).mockResolvedValue(archivedTask)
    vi.mocked(updateTask).mockResolvedValue(createdTask)
    vi.mocked(updateTaskPosition).mockResolvedValue(undefined)
  })

  it('opens a create task modal and refreshes the board after submit', async () => {
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    const createButton = wrapper.findAll('button').find((button) => button.text().includes('新增任务'))
    expect(createButton?.exists()).toBe(true)
    await createButton?.trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain('创建看板任务')

    const form = wrapper.get('form[aria-label="创建任务表单"]')
    await form.get('[aria-label="任务标题"]').setValue('补齐看板创建任务')
    await form.get('[aria-label="任务描述"]').setValue('从看板直接创建任务')
    await form.get('[aria-label="任务类型"]').setValue('BUG')
    await form.get('[aria-label="任务优先级"]').setValue('HIGH')
    await form.get('[aria-label="任务负责人"]').setValue('11')
    await form.get('[aria-label="故事点"]').setValue('3')
    await form.get('[aria-label="预计工时"]').setValue('4')
    await form.get('[aria-label="截止日期"]').setValue('2026-06-01')
    await form.get('[aria-label="验收标准"]').setValue('任务创建后出现在 Backlog')
    await form.trigger('submit')
    await flushPromises()

    expect(createTask).toHaveBeenCalledWith('7', expect.objectContaining({
      title: '补齐看板创建任务',
      description: '从看板直接创建任务',
      taskType: 'BUG',
      priority: 'HIGH',
      columnId: 1,
      assigneeId: 11,
      storyPoints: 3,
      estimatedHours: 4,
      dueDate: '2026-06-01',
      acceptanceCriteria: '任务创建后出现在 Backlog',
    }))
    expect(fetchProjectBoard).toHaveBeenCalledTimes(2)
    expect(document.body.textContent).not.toContain('创建看板任务')
  })

  it('passes members to filters and applies the assignee filter', async () => {
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.get('select[aria-label="任务负责人筛选"]').setValue('11')
    await wrapper.get('form.board-filters').trigger('submit')
    await flushPromises()

    expect(fetchProjectBoard).toHaveBeenLastCalledWith('7', { assigneeId: '11' })
  })

  it('keeps the unassigned assignee filter selected after applying filters', async () => {
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    const assigneeFilter = wrapper.get('select[aria-label="任务负责人筛选"]')
    await assigneeFilter.setValue('0')
    await wrapper.get('form.board-filters').trigger('submit')
    await flushPromises()

    expect(fetchProjectBoard).toHaveBeenLastCalledWith('7', { assigneeId: '0' })
    expect((assigneeFilter.element as HTMLSelectElement).value).toBe('0')
  })

  it('opens the task drawer from a taskId route query', async () => {
    mockedRoute.value.query = { taskId: '77' }
    vi.mocked(fetchTask).mockResolvedValue({ ...archivedTask, archived: true })

    mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    expect(fetchTask).toHaveBeenCalledWith(77)
    expect(document.body.querySelector('.task-drawer')?.textContent).toContain('Archived task')
    expect(document.body.querySelector('.task-drawer')?.textContent).toContain('恢复')
  })

  it('reloads board context when notification navigation reuses the project board route', async () => {
    vi.mocked(fetchTask).mockResolvedValue({ ...archivedTask, projectId: 9, archived: true })
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()
    vi.mocked(fetchProjectBoard).mockClear()
    vi.mocked(fetchProjectMembers).mockClear()
    vi.mocked(fetchArchivedTasks).mockClear()

    mockedRoute.value.params.projectId = '9'
    mockedRoute.value.query = { taskId: '77' }
    await flushPromises()

    expect(fetchProjectBoard).toHaveBeenCalledWith('9', {})
    expect(fetchProjectMembers).toHaveBeenCalledWith('9')
    expect(fetchTask).toHaveBeenCalledWith(77)

    await wrapper.findAll('.segmented-control button')[1].trigger('click')
    await flushPromises()

    expect(fetchArchivedTasks).toHaveBeenLastCalledWith('9', {})
  })

  it('does not show a stale notification task from another project', async () => {
    mockedRoute.value.params.projectId = '9'
    mockedRoute.value.query = { taskId: '77' }
    vi.mocked(fetchTask).mockResolvedValue({ ...archivedTask, projectId: 7, archived: true })

    mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    expect(fetchTask).toHaveBeenCalledWith(77)
    expect(document.body.querySelector('.task-drawer')).toBeNull()
  })

  it('keeps the newer route task drawer when an older route task request resolves later', async () => {
    const olderTaskRequest = deferred<typeof createdTask>()
    const newerTaskRequest = deferred<typeof createdTask>()
    vi.mocked(fetchTask)
      .mockReturnValueOnce(olderTaskRequest.promise)
      .mockReturnValueOnce(newerTaskRequest.promise)
    mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    mockedRoute.value.query = { taskId: '77' }
    await flushPromises()
    mockedRoute.value.query = { taskId: '88' }
    await flushPromises()

    newerTaskRequest.resolve({ ...createdTask, id: 88, title: 'New route task', archived: false })
    await flushPromises()
    expect(document.body.querySelector('.task-drawer')?.textContent).toContain('New route task')

    olderTaskRequest.resolve({ ...createdTask, id: 77, title: 'Old route task', archived: false })
    await flushPromises()

    expect(fetchTask).toHaveBeenNthCalledWith(1, 77)
    expect(fetchTask).toHaveBeenNthCalledWith(2, 88)
    expect(document.body.querySelector('.task-drawer')?.textContent).toContain('New route task')
    expect(document.body.querySelector('.task-drawer')?.textContent).not.toContain('Old route task')
  })

  it('clears stale project members while a reused route loads the next project members', async () => {
    const firstMembers = deferred<typeof members>()
    const secondMembers = deferred<typeof members>()
    vi.mocked(fetchProjectMembers)
      .mockReturnValueOnce(firstMembers.promise)
      .mockReturnValueOnce(secondMembers.promise)
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    firstMembers.resolve(members)
    await flushPromises()

    mockedRoute.value.params.projectId = '9'
    await flushPromises()
    await wrapper.get('.header-actions .primary-button').trigger('click')
    await flushPromises()

    const modal = document.body.querySelector('.task-modal')
    const assigneeSelect = modal?.querySelectorAll('select')[3] as HTMLSelectElement | undefined
    expect(Array.from(assigneeSelect?.options ?? []).map((option) => option.value)).not.toContain('11')

    secondMembers.resolve([
      {
        user: { id: 22, account: 'dev', nickname: 'Developer', email: null, avatarUrl: null },
        role: 'member',
        joinedAt: '2026-05-28T11:00:00',
      },
    ])
    await flushPromises()

    expect(Array.from(assigneeSelect?.options ?? []).map((option) => option.value)).toContain('22')
  })

  it('loads archived tasks and restores an archived task from the archived view', async () => {
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.get('[aria-label="查看已归档任务"]').trigger('click')
    await flushPromises()

    expect(fetchArchivedTasks).toHaveBeenCalledWith('7', {})
    expect(document.body.textContent).toContain('Archived task')

    await wrapper.get('[aria-label="恢复任务 Archived task"]').trigger('click')
    await flushPromises()

    expect(restoreTask).toHaveBeenCalledWith(77)
    expect(fetchProjectBoard).toHaveBeenCalledTimes(2)
    expect(document.body.textContent).not.toContain('Archived task')
  })

  it('keeps the latest archived task response when an earlier request resolves later', async () => {
    const firstRequest = deferred<typeof archivedTask[]>()
    const secondTask = { ...archivedTask, id: 78, title: 'Latest archived task' }
    const secondRequest = deferred<typeof archivedTask[]>()
    vi.mocked(fetchArchivedTasks)
      .mockReturnValueOnce(firstRequest.promise)
      .mockReturnValueOnce(secondRequest.promise)
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.get('[aria-label="查看已归档任务"]').trigger('click')
    await flushPromises()
    await wrapper.get('form[aria-label="已归档任务筛选"] input').setValue('latest')
    await wrapper.get('form[aria-label="已归档任务筛选"]').trigger('submit')
    secondRequest.resolve([secondTask])
    await flushPromises()

    expect(document.body.textContent).toContain('Latest archived task')

    firstRequest.resolve([archivedTask])
    await flushPromises()

    expect(document.body.textContent).toContain('Latest archived task')
    expect(document.body.textContent).not.toContain('Archived task')
  })

  it('keeps drawer archive actions tied to the task open source instead of the current board mode', async () => {
    vi.mocked(fetchProjectBoard).mockResolvedValue({
      ...projectBoard,
      columns: [
        {
          ...projectBoard.columns[0],
          tasks: [currentBoardTask],
        },
        projectBoard.columns[1],
      ],
    })
    vi.mocked(fetchTask).mockImplementation(async (taskId) => (
      Number(taskId) === 77
        ? archivedTask
        : { ...createdTask, id: 88, title: 'Current board task' }
    ))
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.get('article.task-card').trigger('click')
    await flushPromises()
    expect(document.body.textContent).toContain('Current board task')

    await wrapper.get('[aria-label="查看已归档任务"]').trigger('click')
    await flushPromises()
    expect(document.body.querySelector('[aria-label="恢复任务"]')).toBeNull()

    await wrapper.get('button.archived-task-title').trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain('Archived task')
    expect(document.body.querySelector('[aria-label="恢复任务"]')).not.toBeNull()
  })

  it('keeps a current board task drawer in board mode when opening an archived task fails', async () => {
    vi.mocked(fetchProjectBoard).mockResolvedValue({
      ...projectBoard,
      columns: [
        {
          ...projectBoard.columns[0],
          tasks: [currentBoardTask],
        },
        projectBoard.columns[1],
      ],
    })
    vi.mocked(fetchTask).mockImplementation(async (taskId) => {
      if (Number(taskId) === 77) {
        throw new Error('archived task load failed')
      }
      return { ...createdTask, id: 88, title: 'Current board task' }
    })
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.get('article.task-card').trigger('click')
    await flushPromises()
    expect(document.body.textContent).toContain('Current board task')
    expect(document.body.querySelector('[aria-label="归档任务"]')).not.toBeNull()

    await wrapper.get('[aria-label="查看已归档任务"]').trigger('click')
    await flushPromises()
    await wrapper.get('button.archived-task-title').trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain('Current board task')
    expect(document.body.querySelector('[aria-label="恢复任务"]')).toBeNull()
    expect(document.body.querySelector('[aria-label="归档任务"]')).not.toBeNull()
  })

  it('keeps an archived task drawer in archived mode when opening a current board task fails', async () => {
    vi.mocked(fetchProjectBoard).mockResolvedValue({
      ...projectBoard,
      columns: [
        {
          ...projectBoard.columns[0],
          tasks: [currentBoardTask],
        },
        projectBoard.columns[1],
      ],
    })
    vi.mocked(fetchTask).mockImplementation(async (taskId) => {
      if (Number(taskId) === 88) {
        throw new Error('current task load failed')
      }
      return archivedTask
    })
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.get('[aria-label="查看已归档任务"]').trigger('click')
    await flushPromises()
    await wrapper.get('button.archived-task-title').trigger('click')
    await flushPromises()
    expect(document.body.textContent).toContain('Archived task')
    expect(document.body.querySelector('[aria-label="恢复任务"]')).not.toBeNull()

    const currentBoardButton = wrapper.findAll('button').find((button) => button.text().includes('当前看板'))
    expect(currentBoardButton?.exists()).toBe(true)
    await currentBoardButton?.trigger('click')
    await flushPromises()
    await wrapper.get('article.task-card').trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain('Archived task')
    expect(document.body.querySelector('[aria-label="归档任务"]')).toBeNull()
    expect(document.body.querySelector('[aria-label="恢复任务"]')).not.toBeNull()
  })

  it('removes a restored archived task when board refresh fails and reports refresh failure', async () => {
    vi.mocked(fetchProjectBoard)
      .mockResolvedValueOnce(projectBoard)
      .mockRejectedValueOnce(new Error('refresh failed'))
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.get('[aria-label="查看已归档任务"]').trigger('click')
    await flushPromises()
    await wrapper.get('[aria-label="恢复任务 Archived task"]').trigger('click')
    await flushPromises()

    expect(restoreTask).toHaveBeenCalledWith(77)
    expect(document.body.textContent).not.toContain('Archived task')
    expect(document.body.textContent).toContain('任务已恢复，但看板刷新失败')
    expect(document.body.textContent).not.toContain('任务恢复失败')
  })

  it('does not send duplicate restore requests while an archived task restore is pending', async () => {
    const restoreRequest = deferred<typeof archivedTask>()
    vi.mocked(restoreTask).mockReturnValue(restoreRequest.promise)
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.get('[aria-label="查看已归档任务"]').trigger('click')
    await flushPromises()
    const restoreButton = wrapper.get('[aria-label="恢复任务 Archived task"]')
    await restoreButton.trigger('click')
    await restoreButton.trigger('click')

    expect(restoreTask).toHaveBeenCalledTimes(1)
    expect((restoreButton.element as HTMLButtonElement).disabled).toBe(true)

    restoreRequest.resolve(archivedTask)
  })

  it('does not reopen the originally active task after completing if the drawer active task changes', async () => {
    let resolveMove: () => void = () => undefined
    vi.mocked(updateTaskPosition).mockImplementation(() => new Promise<void>((resolve) => {
      resolveMove = resolve
    }))
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    const board = useBoardStore()
    const tasks = useTasksStore()
    board.projectBoard = {
      projectId: 7,
      columns: [
        {
          id: 1,
          name: 'Backlog',
          color: '#64748b',
          sortOrder: 0,
          isDone: false,
          tasks: [{ id: 101, title: 'Original task', priority: 'HIGH', taskType: 'STORY', assignee: null, sortOrder: 0 }],
        },
        { id: 2, name: 'Done', color: '#16a34a', sortOrder: 1, isDone: true, tasks: [] },
      ],
    }
    tasks.drawerOpen = true
    tasks.activeTask = { ...createdTask, id: 101, title: 'Original task' }
    vi.mocked(fetchTask).mockClear()

    const completePromise = wrapper.getComponent(TaskDrawer).props('completeTask')()
    await flushPromises()
    tasks.activeTask = { ...createdTask, id: 202, title: 'Different task' }
    resolveMove()
    await completePromise

    expect(updateTaskPosition).toHaveBeenCalledWith(101, { columnId: 2, sortOrder: 0 })
    expect(fetchTask).not.toHaveBeenCalled()
    expect(tasks.activeTask?.id).toBe(202)
  })

  it('resolves task saves even when board refresh fails afterward', async () => {
    vi.mocked(updateTask).mockResolvedValue({ ...createdTask, title: 'Saved task' })
    const wrapper = mount(ProjectBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    const tasks = useTasksStore()
    tasks.drawerOpen = true
    tasks.activeTask = createdTask
    vi.mocked(fetchProjectBoard).mockRejectedValueOnce(new Error('refresh failed'))

    await expect(wrapper.getComponent(TaskDrawer).props('saveTask')({ title: 'Saved task' })).resolves.toBeUndefined()
    expect(updateTask).toHaveBeenCalledWith(55, { title: 'Saved task' })
  })
})
