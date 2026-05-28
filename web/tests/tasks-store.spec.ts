import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { addTaskComment, archiveTask, deleteTask, fetchTask, updateTask } from '../src/api/tasks'
import { useTasksStore } from '../src/stores/tasks'

vi.mock('../src/api/tasks', () => ({
  addTaskComment: vi.fn(),
  archiveTask: vi.fn(),
  deleteTask: vi.fn(),
  fetchTask: vi.fn(),
  updateTask: vi.fn(),
}))

const taskA = {
  id: 101,
  projectId: 7,
  sprintId: null,
  columnId: 1,
  assignee: null,
  creator: { id: 1, account: 'alex', nickname: 'Alex', email: null, avatarUrl: null },
  title: 'Task A',
  description: null,
  taskType: 'TASK',
  priority: 'MEDIUM',
  storyPoints: null,
  estimatedHours: null,
  dueDate: null,
  acceptanceCriteria: null,
  sortOrder: 0,
  tags: [],
  createdAt: '2026-05-28T10:00:00',
  updatedAt: '2026-05-28T10:00:00',
}

const taskB = {
  ...taskA,
  id: 202,
  title: 'Task B',
}

describe('tasks store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(addTaskComment).mockReset()
    vi.mocked(archiveTask).mockReset()
    vi.mocked(deleteTask).mockReset()
    vi.mocked(fetchTask).mockReset()
    vi.mocked(updateTask).mockReset()
  })

  it('does not replace the active task with a stale save response', async () => {
    let resolveUpdate: (task: typeof taskA) => void = () => undefined
    vi.mocked(updateTask).mockImplementation(() => new Promise((resolve) => {
      resolveUpdate = resolve
    }))
    const tasks = useTasksStore()
    tasks.activeTask = taskA
    tasks.drawerOpen = true

    const savePromise = tasks.saveTask({ title: 'Task A saved' })
    tasks.activeTask = taskB
    resolveUpdate({ ...taskA, title: 'Task A saved' })
    await savePromise

    expect(updateTask).toHaveBeenCalledWith(101, { title: 'Task A saved' })
    expect(tasks.activeTask).toEqual(taskB)
  })
})
