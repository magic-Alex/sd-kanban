import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import MyTaskBoardView from '../src/views/MyTaskBoardView.vue'
import TaskDrawer from '../src/components/task/TaskDrawer.vue'
import { fetchMyTaskBoard, fetchProjectBoard } from '../src/api/board'
import { fetchProjectMembers } from '../src/api/projects'
import {
  archiveTask,
  deleteTask,
  fetchTask,
  fetchTaskActivities,
  fetchTaskComments,
  updateTask,
  updatePersonalTaskPosition,
  updateTaskPosition,
} from '../src/api/tasks'
import { fetchChecklistItems } from '../src/api/checklist'
import { useBoardStore } from '../src/stores/board'
import { useTasksStore } from '../src/stores/tasks'

vi.mock('../src/api/board', () => ({
  fetchProjectBoard: vi.fn(),
  fetchMyTaskBoard: vi.fn(),
}))

vi.mock('../src/api/tasks', () => ({
  addTaskComment: vi.fn(),
  archiveTask: vi.fn(),
  deleteTask: vi.fn(),
  fetchTask: vi.fn(),
  fetchTaskActivities: vi.fn(),
  fetchTaskComments: vi.fn(),
  updateTask: vi.fn(),
  updatePersonalTaskPosition: vi.fn(),
  updateTaskPosition: vi.fn(),
}))

vi.mock('../src/api/checklist', () => ({
  fetchChecklistItems: vi.fn(),
}))

vi.mock('../src/api/projects', () => ({
  fetchProjectMembers: vi.fn(),
}))

const user = {
  id: 1,
  account: 'alex',
  nickname: 'Alex',
  email: 'alex@example.com',
  avatarUrl: null,
}

const taskCard = {
  id: 12,
  projectId: 7,
  projectCode: 'DEL',
  projectName: 'Delivery',
  projectColor: '#0ea5e9',
  sprintId: null,
  columnId: 1,
  columnTemplateKey: 'READY',
  assigneeId: null,
  assignee: null,
  title: 'Review my task',
  taskType: 'TASK',
  priority: 'HIGH',
  storyPoints: null,
  dueDate: null,
  sortOrder: 0,
  checklistDoneCount: 0,
  checklistTotalCount: 0,
}

const task = {
  ...taskCard,
  assignee: null,
  creator: user,
  description: 'Opened from my tasks',
  estimatedHours: null,
  acceptanceCriteria: null,
  tags: [],
  createdAt: '2026-05-28T10:00:00',
  updatedAt: '2026-05-28T10:00:00',
}

const members = [
  {
    user,
    role: 'owner',
    joinedAt: '2026-05-28T09:00:00',
  },
]

const projectBoard = {
  projectId: 7,
  columns: [
    { id: 1, name: 'Ready', color: '#0ea5e9', sortOrder: 0, isDone: false, tasks: [taskCard] },
    { id: 2, name: 'Done', color: '#22c55e', sortOrder: 1, isDone: true, tasks: [] },
  ],
}

describe('MyTaskBoardView', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    setActivePinia(createPinia())
    vi.mocked(fetchMyTaskBoard).mockReset()
    vi.mocked(fetchProjectBoard).mockReset()
    vi.mocked(fetchProjectMembers).mockReset()
    vi.mocked(fetchTask).mockReset()
    vi.mocked(fetchTaskActivities).mockReset()
    vi.mocked(fetchTaskComments).mockReset()
    vi.mocked(fetchChecklistItems).mockReset()
    vi.mocked(updateTask).mockReset()
    vi.mocked(updatePersonalTaskPosition).mockReset()
    vi.mocked(updateTaskPosition).mockReset()
    vi.mocked(archiveTask).mockReset()
    vi.mocked(deleteTask).mockReset()
    vi.mocked(fetchMyTaskBoard).mockResolvedValue({
      groupBy: 'template',
      groups: [
        { templateKey: 'READY', name: 'Ready', color: '#0ea5e9', sortOrder: 0, isDone: false, tasks: [taskCard] },
        { templateKey: 'DONE', name: 'Done', color: '#22c55e', sortOrder: 1, isDone: true, tasks: [] },
      ],
    })
    vi.mocked(fetchProjectBoard).mockResolvedValue(projectBoard)
    vi.mocked(fetchProjectMembers).mockResolvedValue(members)
    vi.mocked(fetchTask).mockResolvedValue(task)
    vi.mocked(fetchTaskActivities).mockResolvedValue([])
    vi.mocked(fetchTaskComments).mockResolvedValue([])
    vi.mocked(fetchChecklistItems).mockResolvedValue([])
    vi.mocked(updateTask).mockResolvedValue(task)
    vi.mocked(updatePersonalTaskPosition).mockResolvedValue(task)
    vi.mocked(updateTaskPosition).mockResolvedValue(undefined)
    vi.mocked(archiveTask).mockResolvedValue(task)
    vi.mocked(deleteTask).mockResolvedValue(undefined)
  })

  it('loads project context when opening the task drawer', async () => {
    const wrapper = mount(MyTaskBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.get('.task-card').trigger('click')
    await flushPromises()

    const drawer = wrapper.getComponent(TaskDrawer)
    expect(fetchProjectBoard).toHaveBeenCalledWith(7)
    expect(fetchProjectMembers).toHaveBeenCalledWith(7)
    expect(drawer.props('members')).toEqual(members)
    expect(drawer.props('columns')).toEqual(projectBoard.columns)
    expect(drawer.props('actionLoading')).toBe(false)
    expect(document.body.textContent).toContain('Review my task')
  })

  it('completes a task from the personal board', async () => {
    const wrapper = mount(MyTaskBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.get('.task-card').trigger('click')
    await flushPromises()

    await wrapper.getComponent(TaskDrawer).props('completeTask')()
    await flushPromises()

    expect(updateTaskPosition).toHaveBeenCalledWith(12, { columnId: 2, sortOrder: 0 })
    expect(fetchTask).toHaveBeenCalledWith(12)
    expect(fetchMyTaskBoard).toHaveBeenCalledTimes(2)
  })

  it('moves a personal task to a template column on drop', async () => {
    vi.mocked(fetchMyTaskBoard).mockResolvedValue({
      groupBy: 'template',
      groups: [
        { templateKey: 'BACKLOG', name: 'Backlog', color: '#64748b', sortOrder: 0, isDone: false, tasks: [{ ...taskCard, id: 1, columnTemplateKey: 'BACKLOG' }] },
        { templateKey: 'READY', name: 'Ready', color: '#0ea5e9', sortOrder: 1, isDone: false, tasks: [] },
      ],
    })
    const wrapper = mount(MyTaskBoardView, {
      attachTo: document.body,
    })
    await flushPromises()
    const board = useBoardStore()
    const movePersonalTask = vi.spyOn(board, 'movePersonalTask').mockResolvedValue(undefined)
    const dataTransfer = {
      getData: vi.fn((type: string) => (type === 'application/sd-kanban-task' ? '1' : '')),
    }

    await wrapper.get('[data-template-key="READY"]').trigger('drop', { dataTransfer })

    expect(movePersonalTask).toHaveBeenCalledWith(1, 'READY', 0)
  })

  it('shows an error and rolls back when a personal task move fails', async () => {
    vi.mocked(fetchMyTaskBoard).mockResolvedValue({
      groupBy: 'template',
      groups: [
        { templateKey: 'BACKLOG', name: 'Backlog', color: '#64748b', sortOrder: 0, isDone: false, tasks: [{ ...taskCard, id: 1, columnTemplateKey: 'BACKLOG' }] },
        { templateKey: 'READY', name: 'Ready', color: '#0ea5e9', sortOrder: 1, isDone: false, tasks: [] },
      ],
    })
    vi.mocked(updatePersonalTaskPosition).mockRejectedValue(new Error('move failed'))
    const wrapper = mount(MyTaskBoardView, {
      attachTo: document.body,
    })
    await flushPromises()
    const dataTransfer = {
      getData: vi.fn((type: string) => (type === 'application/sd-kanban-task' ? '1' : '')),
    }

    await wrapper.get('[data-template-key="READY"]').trigger('drop', { dataTransfer })
    await flushPromises()

    const board = useBoardStore()
    expect(board.error).toBe('个人任务移动失败，请重试')
    expect(wrapper.text()).toContain('个人任务移动失败，请重试')
    expect(board.myTaskBoard?.groups.find((group) => group.templateKey === 'BACKLOG')?.tasks.map((candidate) => candidate.id)).toEqual([1])
    expect(board.myTaskBoard?.groups.find((group) => group.templateKey === 'READY')?.tasks).toEqual([])
  })

  it('clears a stale move error after a later successful personal drop', async () => {
    vi.mocked(fetchMyTaskBoard).mockResolvedValue({
      groupBy: 'template',
      groups: [
        { templateKey: 'BACKLOG', name: 'Backlog', color: '#64748b', sortOrder: 0, isDone: false, tasks: [{ ...taskCard, id: 1, columnTemplateKey: 'BACKLOG' }] },
        { templateKey: 'READY', name: 'Ready', color: '#0ea5e9', sortOrder: 1, isDone: false, tasks: [] },
      ],
    })
    vi.mocked(updatePersonalTaskPosition).mockResolvedValue({ ...task, id: 1, columnTemplateKey: 'READY', sortOrder: 0 })
    const wrapper = mount(MyTaskBoardView, {
      attachTo: document.body,
    })
    await flushPromises()
    const board = useBoardStore()
    board.error = 'stale move error'
    const dataTransfer = {
      getData: vi.fn((type: string) => (type === 'application/sd-kanban-task' ? '1' : '')),
    }

    await wrapper.get('[data-template-key="READY"]').trigger('drop', { dataTransfer })
    await flushPromises()

    expect(board.error).toBeNull()
    expect(wrapper.text()).not.toContain('stale move error')
    expect(board.myTaskBoard?.groups.find((group) => group.templateKey === 'READY')?.tasks.map((candidate) => candidate.id)).toEqual([1])
  })

  it('resolves task saves even when my task board refresh fails afterward', async () => {
    vi.mocked(updateTask).mockResolvedValue({ ...task, title: 'Saved task' })
    const wrapper = mount(MyTaskBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    const tasks = useTasksStore()
    tasks.drawerOpen = true
    tasks.activeTask = task
    vi.mocked(fetchMyTaskBoard).mockRejectedValueOnce(new Error('refresh failed'))

    await expect(wrapper.getComponent(TaskDrawer).props('saveTask')({ title: 'Saved task' })).resolves.toBeUndefined()
    expect(updateTask).toHaveBeenCalledWith(task.id, { title: 'Saved task' })
  })
})
