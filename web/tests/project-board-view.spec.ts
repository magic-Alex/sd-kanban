import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProjectBoardView from '../src/views/ProjectBoardView.vue'
import TaskDrawer from '../src/components/task/TaskDrawer.vue'
import { fetchProjectBoard } from '../src/api/board'
import { fetchProjectMembers } from '../src/api/projects'
import { createTask, fetchTask, updateTask, updateTaskPosition } from '../src/api/tasks'
import { useBoardStore } from '../src/stores/board'
import { useTasksStore } from '../src/stores/tasks'

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { projectId: '7' } }),
}))

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
  fetchTask: vi.fn(),
  updateTask: vi.fn(),
  updateTaskPosition: vi.fn(),
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

describe('ProjectBoardView', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    setActivePinia(createPinia())
    vi.mocked(fetchProjectBoard).mockReset()
    vi.mocked(fetchProjectMembers).mockReset()
    vi.mocked(createTask).mockReset()
    vi.mocked(fetchTask).mockReset()
    vi.mocked(updateTask).mockReset()
    vi.mocked(updateTaskPosition).mockReset()
    vi.mocked(fetchProjectBoard).mockResolvedValue(projectBoard)
    vi.mocked(fetchProjectMembers).mockResolvedValue(members)
    vi.mocked(createTask).mockResolvedValue(createdTask)
    vi.mocked(fetchTask).mockResolvedValue(createdTask)
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
