import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import {
  addTaskComment,
  archiveTask,
  deleteTask,
  fetchTask,
  fetchTaskActivities,
  fetchTaskComments,
  restoreTask,
  updateTask,
} from '../src/api/tasks'
import {
  createChecklistItem,
  deleteChecklistItem,
  fetchChecklistItems,
  reorderChecklistItems,
  toggleChecklistItem,
  updateChecklistItem,
} from '../src/api/checklist'
import { useTasksStore } from '../src/stores/tasks'

vi.mock('../src/api/tasks', () => ({
  addTaskComment: vi.fn(),
  archiveTask: vi.fn(),
  deleteTask: vi.fn(),
  fetchTask: vi.fn(),
  fetchTaskActivities: vi.fn(),
  fetchTaskComments: vi.fn(),
  restoreTask: vi.fn(),
  updateTask: vi.fn(),
}))

vi.mock('../src/api/checklist', () => ({
  createChecklistItem: vi.fn(),
  deleteChecklistItem: vi.fn(),
  fetchChecklistItems: vi.fn(),
  reorderChecklistItems: vi.fn(),
  toggleChecklistItem: vi.fn(),
  updateChecklistItem: vi.fn(),
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

const checklistItemA = {
  id: 301,
  taskId: taskA.id,
  projectId: taskA.projectId,
  title: 'Check API',
  done: false,
  sortOrder: 0,
  createdBy: taskA.creator,
  completedBy: null,
  completedAt: null,
  createdAt: '2026-05-29T10:00:02',
  updatedAt: '2026-05-29T10:00:02',
}

const checklistItemB = {
  ...checklistItemA,
  id: 302,
  title: 'Wire store',
  sortOrder: 1,
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
    vi.mocked(createChecklistItem).mockReset()
    vi.mocked(deleteChecklistItem).mockReset()
    vi.mocked(fetchTaskActivities).mockReset()
    vi.mocked(fetchTaskComments).mockReset()
    vi.mocked(fetchChecklistItems).mockReset()
    vi.mocked(reorderChecklistItems).mockReset()
    vi.mocked(restoreTask).mockReset()
    vi.mocked(toggleChecklistItem).mockReset()
    vi.mocked(updateChecklistItem).mockReset()
    vi.mocked(updateTask).mockReset()
    vi.mocked(fetchTaskActivities).mockResolvedValue([])
    vi.mocked(fetchTaskComments).mockResolvedValue([])
    vi.mocked(fetchChecklistItems).mockResolvedValue([])
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

  it('does not show a stale open task failure after a newer task opens', async () => {
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
    const error = new Error('task A unavailable')
    fetchA.reject(error)
    await expect(openA).rejects.toThrow(error)

    expect(fetchTask).toHaveBeenNthCalledWith(1, taskA.id)
    expect(fetchTask).toHaveBeenNthCalledWith(2, taskB.id)
    expect(tasks.activeTask).toEqual(taskB)
    expect(tasks.drawerOpen).toBe(true)
    expect(tasks.error).toBeNull()
    expect(tasks.loading).toBe(false)
  })

  it('loads task detail side data when opening a task', async () => {
    vi.mocked(fetchTask).mockResolvedValue(taskA)
    vi.mocked(fetchTaskComments).mockResolvedValue([{ id: 1, taskId: taskA.id, author: taskA.creator, content: 'Hello', createdAt: '2026-05-29T10:00:00', updatedAt: '2026-05-29T10:00:00' }])
    vi.mocked(fetchTaskActivities).mockResolvedValue([{ id: 2, taskId: taskA.id, actor: taskA.creator, actionType: 'TASK_CREATED', fieldName: null, oldValue: null, newValue: null, displayText: 'Alex 创建了任务', createdAt: '2026-05-29T10:00:01' }])
    vi.mocked(fetchChecklistItems).mockResolvedValue([{ id: 3, taskId: taskA.id, projectId: taskA.projectId, title: 'Check API', done: false, sortOrder: 0, createdBy: taskA.creator, completedBy: null, completedAt: null, createdAt: '2026-05-29T10:00:02', updatedAt: '2026-05-29T10:00:02' }])

    const tasks = useTasksStore()
    await tasks.openTask(taskA.id)

    expect(tasks.activeTask).toEqual(taskA)
    expect(tasks.comments).toHaveLength(1)
    expect(tasks.activities[0].displayText).toBe('Alex 创建了任务')
    expect(tasks.checklistItems[0].title).toBe('Check API')
  })

  it('opens task detail when side data fails and clears failed side data', async () => {
    vi.mocked(fetchTask).mockResolvedValue(taskA)
    vi.mocked(fetchTaskComments).mockRejectedValue(new Error('comments failed'))
    vi.mocked(fetchTaskActivities).mockResolvedValue([{ id: 2, taskId: taskA.id, actor: taskA.creator, actionType: 'TASK_CREATED', fieldName: null, oldValue: null, newValue: null, displayText: 'Task created', createdAt: '2026-05-29T10:00:01' }])
    vi.mocked(fetchChecklistItems).mockRejectedValue(new Error('checklist failed'))
    const tasks = useTasksStore()
    tasks.comments = [{ id: 9, taskId: taskA.id, author: taskA.creator, content: 'stale', createdAt: '2026-05-28T10:00:00', updatedAt: '2026-05-28T10:00:00' }]
    tasks.checklistItems = [checklistItemB]

    await tasks.openTask(taskA.id)

    expect(tasks.activeTask).toEqual(taskA)
    expect(tasks.drawerOpen).toBe(true)
    expect(tasks.comments).toEqual([])
    expect(tasks.activities).toHaveLength(1)
    expect(tasks.checklistItems).toEqual([])
    expect(tasks.error).toBeNull()
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

  it('restores the active archived task and closes the drawer', async () => {
    vi.mocked(restoreTask).mockResolvedValue(taskA)
    const tasks = useTasksStore()
    tasks.activeTask = taskA
    tasks.drawerOpen = true

    await tasks.restoreActiveTask()

    expect(restoreTask).toHaveBeenCalledWith(taskA.id)
    expect(tasks.drawerOpen).toBe(false)
  })

  it('updates checklist state for add, rename, toggle, remove, and reorder actions', async () => {
    vi.mocked(createChecklistItem).mockResolvedValue(checklistItemB)
    vi.mocked(updateChecklistItem).mockResolvedValue({ ...checklistItemA, title: 'Updated API' })
    vi.mocked(toggleChecklistItem).mockResolvedValue({ ...checklistItemA, done: true })
    vi.mocked(deleteChecklistItem).mockResolvedValue(undefined)
    vi.mocked(reorderChecklistItems).mockResolvedValue([
      { ...checklistItemB, sortOrder: 0 },
      { ...checklistItemA, sortOrder: 1 },
    ])
    const tasks = useTasksStore()
    tasks.activeTask = taskA
    tasks.drawerOpen = true
    tasks.checklistItems = [checklistItemA]

    await tasks.addChecklistItem('Wire store')
    expect(createChecklistItem).toHaveBeenCalledWith(taskA.id, 'Wire store')
    expect(tasks.checklistItems.map((item) => item.id)).toEqual([checklistItemA.id, checklistItemB.id])

    await tasks.renameChecklistItem(checklistItemA.id, 'Updated API')
    expect(updateChecklistItem).toHaveBeenCalledWith(taskA.id, checklistItemA.id, 'Updated API')
    expect(tasks.checklistItems[0].title).toBe('Updated API')

    await tasks.toggleChecklistItem(checklistItemA.id)
    expect(toggleChecklistItem).toHaveBeenCalledWith(taskA.id, checklistItemA.id)
    expect(tasks.checklistItems[0].done).toBe(true)

    await tasks.removeChecklistItem(checklistItemA.id)
    expect(deleteChecklistItem).toHaveBeenCalledWith(taskA.id, checklistItemA.id)
    expect(tasks.checklistItems.map((item) => item.id)).toEqual([checklistItemB.id])

    tasks.checklistItems = [checklistItemA, checklistItemB]
    await tasks.moveChecklistItem(checklistItemB.id, 'up')
    expect(reorderChecklistItems).toHaveBeenCalledWith(taskA.id, [checklistItemB.id, checklistItemA.id])
    expect(tasks.checklistItems.map((item) => item.id)).toEqual([checklistItemB.id, checklistItemA.id])
  })

  it('does not apply a stale checklist add response after switching tasks', async () => {
    const create = deferred<typeof checklistItemA>()
    vi.mocked(createChecklistItem).mockReturnValue(create.promise)
    const tasks = useTasksStore()
    tasks.activeTask = taskA
    tasks.drawerOpen = true
    tasks.checklistItems = [checklistItemA]

    const addPromise = tasks.addChecklistItem('Old task item')
    tasks.activeTask = taskB
    tasks.checklistItems = [checklistItemB]
    create.resolve({ ...checklistItemA, id: 303, title: 'Old task item' })
    await addPromise

    expect(createChecklistItem).toHaveBeenCalledWith(taskA.id, 'Old task item')
    expect(tasks.activeTask).toEqual(taskB)
    expect(tasks.checklistItems).toEqual([checklistItemB])
  })
})
