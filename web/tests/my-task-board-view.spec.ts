import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import MyTaskBoardView from '../src/views/MyTaskBoardView.vue'
import TaskDrawer from '../src/components/task/TaskDrawer.vue'
import { fetchMyTaskBoard } from '../src/api/board'
import { archiveTask, deleteTask, fetchTask, updateTask } from '../src/api/tasks'
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
  updateTask: vi.fn(),
  updateTaskPosition: vi.fn(),
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
  sprintId: null,
  columnId: 1,
  assigneeId: null,
  assignee: null,
  title: 'Review my task',
  taskType: 'TASK',
  priority: 'HIGH',
  storyPoints: null,
  dueDate: null,
  sortOrder: 0,
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

describe('MyTaskBoardView', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    setActivePinia(createPinia())
    vi.mocked(fetchMyTaskBoard).mockReset()
    vi.mocked(fetchTask).mockReset()
    vi.mocked(updateTask).mockReset()
    vi.mocked(archiveTask).mockReset()
    vi.mocked(deleteTask).mockReset()
    vi.mocked(fetchMyTaskBoard).mockResolvedValue({
      groupBy: 'project',
      groups: [{ id: 7, name: 'Delivery', tasks: [taskCard] }],
    })
    vi.mocked(fetchTask).mockResolvedValue(task)
    vi.mocked(updateTask).mockResolvedValue(task)
    vi.mocked(archiveTask).mockResolvedValue(task)
    vi.mocked(deleteTask).mockResolvedValue(undefined)
  })

  it('opens the task drawer safely without project members or columns', async () => {
    const wrapper = mount(MyTaskBoardView, {
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.get('.task-card').trigger('click')
    await flushPromises()

    const drawer = wrapper.getComponent(TaskDrawer)
    expect(drawer.props('members')).toEqual([])
    expect(drawer.props('columns')).toEqual([])
    expect(drawer.props('actionLoading')).toBe(false)
    expect(document.body.textContent).toContain('Review my task')
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
