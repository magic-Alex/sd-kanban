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

function deferred<T>() {
  let resolve: (value: T) => void = () => undefined
  let reject: (error: Error) => void = () => undefined
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })
  return { promise, resolve, reject }
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

  it('does not show a stale save failure on a newer active task', async () => {
    let rejectUpdate: (error: Error) => void = () => undefined
    vi.mocked(updateTask).mockImplementation(() => new Promise((_, reject) => {
      rejectUpdate = reject
    }))
    const tasks = useTasksStore()
    tasks.activeTask = taskA
    tasks.drawerOpen = true

    const savePromise = tasks.saveTask({ title: 'Task A saved' })
    tasks.activeTask = taskB
    tasks.actionLoading = false

    const error = new Error('network unavailable')
    rejectUpdate(error)
    await expect(savePromise).rejects.toThrow(error)

    expect(updateTask).toHaveBeenCalledWith(101, { title: 'Task A saved' })
    expect(tasks.activeTask).toEqual(taskB)
    expect(tasks.actionError).toBeNull()
    expect(tasks.actionLoading).toBe(false)
  })

  it('clears stale action loading when opening a new task before save succeeds', async () => {
    const update = deferred<typeof taskA>()
    const fetch = deferred<typeof taskB>()
    vi.mocked(updateTask).mockReturnValue(update.promise)
    vi.mocked(fetchTask).mockReturnValue(fetch.promise)
    const tasks = useTasksStore()
    tasks.activeTask = taskA
    tasks.drawerOpen = true

    const savePromise = tasks.saveTask({ title: 'Task A saved' })
    const openPromise = tasks.openTask(taskB.id)

    expect(tasks.actionLoading).toBe(false)
    expect(tasks.actionError).toBeNull()

    fetch.resolve(taskB)
    await openPromise
    update.resolve({ ...taskA, title: 'Task A saved' })
    await savePromise

    expect(tasks.activeTask).toEqual(taskB)
    expect(tasks.drawerOpen).toBe(true)
    expect(tasks.actionLoading).toBe(false)
  })

  it('does not replace the active task with a stale open task response', async () => {
    const fetchA = deferred<typeof taskA>()
    const fetchB = deferred<typeof taskB>()
    vi.mocked(fetchTask)
      .mockReturnValueOnce(fetchA.promise)
      .mockReturnValueOnce(fetchB.promise)
    const tasks = useTasksStore()

    const openA = tasks.openTask(taskA.id)
    const openB = tasks.openTask(taskB.id)

    fetchB.resolve(taskB)
    await openB
    fetchA.resolve(taskA)
    await openA

    expect(fetchTask).toHaveBeenNthCalledWith(1, taskA.id)
    expect(fetchTask).toHaveBeenNthCalledWith(2, taskB.id)
    expect(tasks.activeTask).toEqual(taskB)
    expect(tasks.drawerOpen).toBe(true)
    expect(tasks.error).toBeNull()
    expect(tasks.loading).toBe(false)
  })

  it('does not close a newer task drawer after a stale archive succeeds', async () => {
    const archive = deferred<void>()
    vi.mocked(archiveTask).mockReturnValue(archive.promise)
    const tasks = useTasksStore()
    tasks.activeTask = taskA
    tasks.drawerOpen = true

    const archivePromise = tasks.archiveActiveTask()
    tasks.activeTask = taskB
    tasks.drawerOpen = true
    tasks.actionLoading = false

    archive.resolve()
    await archivePromise

    expect(archiveTask).toHaveBeenCalledWith(taskA.id)
    expect(tasks.activeTask).toEqual(taskB)
    expect(tasks.drawerOpen).toBe(true)
    expect(tasks.actionError).toBeNull()
    expect(tasks.actionLoading).toBe(false)
  })

  it('does not show a stale archive failure on a newer active task', async () => {
    const archive = deferred<void>()
    vi.mocked(archiveTask).mockReturnValue(archive.promise)
    const tasks = useTasksStore()
    tasks.activeTask = taskA
    tasks.drawerOpen = true

    const archivePromise = tasks.archiveActiveTask()
    tasks.activeTask = taskB
    tasks.drawerOpen = true
    tasks.actionLoading = false

    const error = new Error('archive failed')
    archive.reject(error)
    await expect(archivePromise).rejects.toThrow(error)

    expect(archiveTask).toHaveBeenCalledWith(taskA.id)
    expect(tasks.activeTask).toEqual(taskB)
    expect(tasks.drawerOpen).toBe(true)
    expect(tasks.actionError).toBeNull()
    expect(tasks.actionLoading).toBe(false)
  })

  it('does not close a newer task drawer after a stale delete succeeds', async () => {
    const remove = deferred<void>()
    vi.mocked(deleteTask).mockReturnValue(remove.promise)
    const tasks = useTasksStore()
    tasks.activeTask = taskA
    tasks.drawerOpen = true

    const deletePromise = tasks.deleteActiveTask()
    tasks.activeTask = taskB
    tasks.drawerOpen = true
    tasks.actionLoading = false

    remove.resolve()
    await deletePromise

    expect(deleteTask).toHaveBeenCalledWith(taskA.id)
    expect(tasks.activeTask).toEqual(taskB)
    expect(tasks.drawerOpen).toBe(true)
    expect(tasks.actionError).toBeNull()
    expect(tasks.actionLoading).toBe(false)
  })

  it('does not show a stale delete failure on a newer active task', async () => {
    const remove = deferred<void>()
    vi.mocked(deleteTask).mockReturnValue(remove.promise)
    const tasks = useTasksStore()
    tasks.activeTask = taskA
    tasks.drawerOpen = true

    const deletePromise = tasks.deleteActiveTask()
    tasks.activeTask = taskB
    tasks.drawerOpen = true
    tasks.actionLoading = false

    const error = new Error('delete failed')
    remove.reject(error)
    await expect(deletePromise).rejects.toThrow(error)

    expect(deleteTask).toHaveBeenCalledWith(taskA.id)
    expect(tasks.activeTask).toEqual(taskB)
    expect(tasks.drawerOpen).toBe(true)
    expect(tasks.actionError).toBeNull()
    expect(tasks.actionLoading).toBe(false)
  })
})
