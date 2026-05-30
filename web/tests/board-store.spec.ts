import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { fetchMyTaskBoard, fetchProjectBoard } from '../src/api/board'
import { archiveTask, createTask, deleteTask, updatePersonalTaskPosition, updateTaskPosition } from '../src/api/tasks'
import { useBoardStore } from '../src/stores/board'

vi.mock('../src/api/board', () => ({
  fetchProjectBoard: vi.fn(),
  fetchMyTaskBoard: vi.fn(),
}))

vi.mock('../src/api/tasks', () => ({
  archiveTask: vi.fn(),
  createTask: vi.fn(),
  deleteTask: vi.fn(),
  updatePersonalTaskPosition: vi.fn(),
  updateTaskPosition: vi.fn(),
}))

const projectBoard = {
  projectId: 7,
  columns: [
    {
      id: 1,
      name: 'Ready',
      templateKey: 'READY',
      color: '#0ea5e9',
      sortOrder: 0,
      isDone: false,
      tasks: [
        {
          id: 12,
          projectId: 7,
          projectCode: 'DEL',
          projectName: 'Delivery',
          projectColor: '#0ea5e9',
          sprintId: null,
          columnId: 1,
          columnTemplateKey: 'READY',
          assigneeId: 3,
          assignee: { id: 3, account: 'member', nickname: 'Member', email: 'member@example.com', avatarUrl: null },
          title: 'Build board',
          taskType: 'STORY',
          priority: 'HIGH',
          storyPoints: 5,
          dueDate: null,
          sortOrder: 0,
        },
      ],
    },
    {
      id: 2,
      name: 'Done',
      templateKey: 'DONE',
      color: '#22c55e',
      sortOrder: 1,
      isDone: true,
      tasks: [],
    },
  ],
}

const otherProjectBoard = {
  ...projectBoard,
  projectId: 9,
  columns: [
    {
      ...projectBoard.columns[0],
      id: 91,
      name: 'Project 9 Ready',
      tasks: [],
    },
  ],
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

describe('board store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(fetchProjectBoard).mockReset()
    vi.mocked(fetchMyTaskBoard).mockReset()
    vi.mocked(archiveTask).mockReset()
    vi.mocked(createTask).mockReset()
    vi.mocked(deleteTask).mockReset()
    vi.mocked(updatePersonalTaskPosition).mockReset()
    vi.mocked(updateTaskPosition).mockReset()
  })

  it('loads project board columns and cards', async () => {
    vi.mocked(fetchProjectBoard).mockResolvedValue(projectBoard)

    const board = useBoardStore()
    await board.loadProjectBoard(7)

    expect(fetchProjectBoard).toHaveBeenCalledWith(7, {})
    expect(board.projectBoard?.columns[0].name).toBe('Ready')
    expect(board.projectBoard?.columns[0].tasks[0].title).toBe('Build board')
  })

  it('keeps the newest project board when an older load resolves later', async () => {
    const firstLoad = deferred<typeof projectBoard>()
    const secondLoad = deferred<typeof otherProjectBoard>()
    vi.mocked(fetchProjectBoard)
      .mockReturnValueOnce(firstLoad.promise)
      .mockReturnValueOnce(secondLoad.promise)
    const board = useBoardStore()

    const loadFirstProject = board.loadProjectBoard(7)
    const loadSecondProject = board.loadProjectBoard(9)

    secondLoad.resolve(otherProjectBoard)
    await loadSecondProject
    expect(board.projectBoard?.projectId).toBe(9)
    expect(board.lastProjectId).toBe(9)

    firstLoad.resolve(projectBoard)
    await loadFirstProject

    expect(board.projectBoard?.projectId).toBe(9)
    expect(board.projectBoard?.columns[0].name).toBe('Project 9 Ready')
    expect(board.lastProjectId).toBe(9)
    expect(board.loading).toBe(false)
  })

  it('clears the project board and ignores pending board responses', async () => {
    const pendingLoad = deferred<typeof projectBoard>()
    vi.mocked(fetchProjectBoard).mockReturnValue(pendingLoad.promise)
    const board = useBoardStore()

    const loadProject = board.loadProjectBoard(7)
    board.clearProjectBoard()
    pendingLoad.resolve(projectBoard)
    await loadProject

    expect(board.projectBoard).toBeNull()
    expect(board.lastProjectId).toBeNull()
    expect(board.lastFilters).toEqual({})
    expect(board.loading).toBe(false)
  })

  it('loads my-task board cards', async () => {
    vi.mocked(fetchMyTaskBoard).mockResolvedValue({
      groupBy: 'project',
      groups: [
        {
          templateKey: 'READY',
          name: 'Delivery',
          color: '#0ea5e9',
          sortOrder: 0,
          isDone: false,
          tasks: projectBoard.columns[0].tasks,
        },
      ],
    })

    const board = useBoardStore()
    await board.loadMyTaskBoard('project')

    expect(fetchMyTaskBoard).toHaveBeenCalledWith('project')
    expect(board.myTaskBoard?.groups[0].tasks[0].title).toBe('Build board')
  })

  it('moves a personal task by template key with optimistic state and reconciles the response position', async () => {
    vi.mocked(updatePersonalTaskPosition).mockResolvedValue({
      ...projectBoard.columns[0].tasks[0],
      columnId: 2,
      columnTemplateKey: 'DONE',
      sortOrder: 4,
      creator: { id: 1, account: 'alex', nickname: 'Alex', email: null, avatarUrl: null },
      description: null,
      estimatedHours: null,
      acceptanceCriteria: null,
      tags: [],
      createdAt: '2026-05-28T10:00:00',
      updatedAt: '2026-05-28T10:00:00',
    })
    const board = useBoardStore()
    board.myTaskBoard = {
      groupBy: 'template',
      groups: [
        {
          templateKey: 'READY',
          name: 'Ready',
          color: '#0ea5e9',
          sortOrder: 0,
          isDone: false,
          tasks: [structuredClone(projectBoard.columns[0].tasks[0])],
        },
        {
          templateKey: 'DONE',
          name: 'Done',
          color: '#22c55e',
          sortOrder: 1,
          isDone: true,
          tasks: [],
        },
      ],
    }

    await board.movePersonalTask(12, 'DONE', 0)

    expect(updatePersonalTaskPosition).toHaveBeenCalledWith(12, { targetTemplateKey: 'DONE', sortOrder: 0 })
    expect(board.myTaskBoard.groups[0].tasks).toHaveLength(0)
    expect(board.myTaskBoard.groups[1].tasks[0].id).toBe(12)
    expect(board.myTaskBoard.groups[1].tasks[0].columnTemplateKey).toBe('DONE')
    expect(board.myTaskBoard.groups[1].tasks[0].columnId).toBe(2)
    expect(board.myTaskBoard.groups[1].tasks[0].sortOrder).toBe(4)
  })

  it('restores the previous personal board when a personal move fails', async () => {
    vi.mocked(updatePersonalTaskPosition).mockRejectedValue(new Error('network'))
    const board = useBoardStore()
    board.myTaskBoard = {
      groupBy: 'template',
      groups: [
        {
          templateKey: 'READY',
          name: 'Ready',
          color: '#0ea5e9',
          sortOrder: 0,
          isDone: false,
          tasks: [structuredClone(projectBoard.columns[0].tasks[0])],
        },
        {
          templateKey: 'DONE',
          name: 'Done',
          color: '#22c55e',
          sortOrder: 1,
          isDone: true,
          tasks: [],
        },
      ],
    }

    await expect(board.movePersonalTask(12, 'DONE', 0)).rejects.toThrow('network')

    expect(board.myTaskBoard.groups[0].tasks[0].id).toBe(12)
    expect(board.myTaskBoard.groups[0].tasks[0].columnTemplateKey).toBe('READY')
    expect(board.myTaskBoard.groups[1].tasks).toHaveLength(0)
  })

  it('does not restore an old personal board when a stale move fails after refresh', async () => {
    const moveRequest = deferred<Awaited<ReturnType<typeof updatePersonalTaskPosition>>>()
    vi.mocked(updatePersonalTaskPosition).mockReturnValue(moveRequest.promise)
    const refreshedBoard = {
      groupBy: 'template',
      groups: [
        {
          templateKey: 'READY',
          name: 'Ready',
          color: '#0ea5e9',
          sortOrder: 0,
          isDone: false,
          tasks: [],
        },
        {
          templateKey: 'DONE',
          name: 'Done',
          color: '#22c55e',
          sortOrder: 1,
          isDone: true,
          tasks: [{ ...projectBoard.columns[0].tasks[0], id: 44, title: 'Fresh task' }],
        },
      ],
    }
    vi.mocked(fetchMyTaskBoard).mockResolvedValue(refreshedBoard)
    const board = useBoardStore()
    board.myTaskBoard = {
      groupBy: 'template',
      groups: [
        {
          templateKey: 'READY',
          name: 'Ready',
          color: '#0ea5e9',
          sortOrder: 0,
          isDone: false,
          tasks: [structuredClone(projectBoard.columns[0].tasks[0])],
        },
        {
          templateKey: 'DONE',
          name: 'Done',
          color: '#22c55e',
          sortOrder: 1,
          isDone: true,
          tasks: [],
        },
      ],
    }

    const moveTask = board.movePersonalTask(12, 'DONE', 0)
    await board.loadMyTaskBoard()
    moveRequest.reject(new Error('move failed'))
    await expect(moveTask).rejects.toThrow('move failed')

    expect(board.myTaskBoard?.groups[1].tasks[0].id).toBe(44)
    expect(board.myTaskBoard?.groups[1].tasks[0].title).toBe('Fresh task')
  })

  it('clears moving task state when a personal move is superseded by a refresh', async () => {
    const moveRequest = deferred<Awaited<ReturnType<typeof updatePersonalTaskPosition>>>()
    vi.mocked(updatePersonalTaskPosition).mockReturnValue(moveRequest.promise)
    vi.mocked(fetchMyTaskBoard).mockResolvedValue({
      groupBy: 'template',
      groups: [
        {
          templateKey: 'READY',
          name: 'Ready',
          color: '#0ea5e9',
          sortOrder: 0,
          isDone: false,
          tasks: [],
        },
        {
          templateKey: 'DONE',
          name: 'Done',
          color: '#22c55e',
          sortOrder: 1,
          isDone: true,
          tasks: [{ ...projectBoard.columns[0].tasks[0], columnTemplateKey: 'DONE' }],
        },
      ],
    })
    const board = useBoardStore()
    board.myTaskBoard = {
      groupBy: 'template',
      groups: [
        {
          templateKey: 'READY',
          name: 'Ready',
          color: '#0ea5e9',
          sortOrder: 0,
          isDone: false,
          tasks: [structuredClone(projectBoard.columns[0].tasks[0])],
        },
        {
          templateKey: 'DONE',
          name: 'Done',
          color: '#22c55e',
          sortOrder: 1,
          isDone: true,
          tasks: [],
        },
      ],
    }

    const moveTask = board.movePersonalTask(12, 'DONE', 0)
    expect(board.movingTaskId).toBe(12)
    await board.loadMyTaskBoard()
    moveRequest.resolve({
      ...projectBoard.columns[0].tasks[0],
      columnId: 2,
      sortOrder: 0,
      creator: { id: 1, account: 'alex', nickname: 'Alex', email: null, avatarUrl: null },
      description: null,
      estimatedHours: null,
      acceptanceCriteria: null,
      tags: [],
      createdAt: '2026-05-28T10:00:00',
      updatedAt: '2026-05-28T10:00:00',
    })
    await moveTask

    expect(board.movingTaskId).toBeNull()
  })

  it('ignores stale my-task board load failures and loading cleanup', async () => {
    const firstLoad = deferred<Awaited<ReturnType<typeof fetchMyTaskBoard>>>()
    const secondLoad = deferred<Awaited<ReturnType<typeof fetchMyTaskBoard>>>()
    vi.mocked(fetchMyTaskBoard)
      .mockReturnValueOnce(firstLoad.promise)
      .mockReturnValueOnce(secondLoad.promise)
    const board = useBoardStore()

    const staleLoad = board.loadMyTaskBoard('template')
    const currentLoad = board.loadMyTaskBoard('template')
    secondLoad.resolve({
      groupBy: 'template',
      groups: [
        {
          templateKey: 'DONE',
          name: 'Done',
          color: '#22c55e',
          sortOrder: 1,
          isDone: true,
          tasks: [{ ...projectBoard.columns[0].tasks[0], id: 55, title: 'Current task' }],
        },
      ],
    })
    await currentLoad

    board.error = null
    board.loading = true
    firstLoad.reject(new Error('stale load failed'))
    await expect(staleLoad).rejects.toThrow('stale load failed')

    expect(board.myTaskBoard?.groups[0].tasks[0].id).toBe(55)
    expect(board.error).toBeNull()
    expect(board.loading).toBe(true)
  })

  it('moves a task by calling the position API', async () => {
    vi.mocked(updateTaskPosition).mockResolvedValue({ ...projectBoard.columns[0].tasks[0], columnId: 2, sortOrder: 0 })
    const board = useBoardStore()
    board.projectBoard = structuredClone(projectBoard)

    await board.moveTask(12, 2, 0)

    expect(updateTaskPosition).toHaveBeenCalledWith(12, { columnId: 2, sortOrder: 0 })
    expect(board.projectBoard?.columns[0].tasks).toHaveLength(0)
    expect(board.projectBoard?.columns[1].tasks[0].id).toBe(12)
    expect(board.projectBoard?.columns[1].tasks[0].columnId).toBe(2)
  })

  it('marks a task complete by moving it to the first done column', async () => {
    vi.mocked(updateTaskPosition).mockResolvedValue({ ...projectBoard.columns[0].tasks[0], columnId: 2, sortOrder: 0 })
    const board = useBoardStore()
    board.projectBoard = structuredClone(projectBoard)

    await board.markTaskComplete(12)

    expect(updateTaskPosition).toHaveBeenCalledWith(12, { columnId: 2, sortOrder: 0 })
    expect(board.projectBoard?.columns[0].tasks).toHaveLength(0)
    expect(board.projectBoard?.columns[1].tasks[0].id).toBe(12)
  })

  it('removes archived and deleted tasks from the current board', () => {
    const board = useBoardStore()
    board.projectBoard = structuredClone(projectBoard)

    board.removeTaskFromBoard(12)

    expect(board.projectBoard?.columns[0].tasks).toHaveLength(0)
  })

  it('restores previous column state when move fails', async () => {
    vi.mocked(updateTaskPosition).mockRejectedValue(new Error('network'))
    const board = useBoardStore()
    board.projectBoard = structuredClone(projectBoard)

    await expect(board.moveTask(12, 2, 0)).rejects.toThrow('network')

    expect(board.projectBoard?.columns[0].tasks[0].id).toBe(12)
    expect(board.projectBoard?.columns[1].tasks).toHaveLength(0)
  })

  it('does not restore an old project board when a stale move fails after project reset', async () => {
    const moveRequest = deferred<unknown>()
    vi.mocked(updateTaskPosition).mockReturnValue(moveRequest.promise)
    const board = useBoardStore()
    board.projectBoard = structuredClone(projectBoard)

    const moveTask = board.moveTask(12, 2, 0)
    board.clearProjectBoard()
    board.projectBoard = structuredClone(otherProjectBoard)
    moveRequest.reject(new Error('move failed'))
    await expect(moveTask).rejects.toThrow('move failed')

    expect(board.projectBoard?.projectId).toBe(9)
    expect(board.projectBoard?.columns[0].name).toBe('Project 9 Ready')
  })

  it('creates a task and reloads the current project board', async () => {
    vi.mocked(createTask).mockResolvedValue({
      id: 31,
      projectId: 7,
      sprintId: null,
      columnId: 1,
      assignee: null,
      creator: { id: 1, account: 'alex', nickname: 'Alex', email: null, avatarUrl: null },
      title: 'Write onboarding checklist',
      description: null,
      taskType: 'TASK',
      priority: 'MEDIUM',
      storyPoints: null,
      estimatedHours: null,
      dueDate: null,
      acceptanceCriteria: null,
      sortOrder: 1,
      tags: [],
      createdAt: '2026-05-28T10:00:00',
      updatedAt: '2026-05-28T10:00:00',
    })
    vi.mocked(fetchProjectBoard).mockResolvedValue(projectBoard)
    const board = useBoardStore()

    const task = await board.createTask(7, {
      title: 'Write onboarding checklist',
      taskType: 'TASK',
      priority: 'MEDIUM',
      columnId: 1,
    }, { keyword: 'onboarding' })

    expect(createTask).toHaveBeenCalledWith(7, {
      title: 'Write onboarding checklist',
      taskType: 'TASK',
      priority: 'MEDIUM',
      columnId: 1,
    })
    expect(fetchProjectBoard).toHaveBeenCalledWith(7, { keyword: 'onboarding' })
    expect(task.title).toBe('Write onboarding checklist')
  })

  it('skips the create refresh when the caller marks the project context stale', async () => {
    vi.mocked(createTask).mockResolvedValue({
      id: 32,
      projectId: 7,
      sprintId: null,
      columnId: 1,
      assignee: null,
      creator: { id: 1, account: 'alex', nickname: 'Alex', email: null, avatarUrl: null },
      title: 'Delayed task',
      description: null,
      taskType: 'TASK',
      priority: 'MEDIUM',
      storyPoints: null,
      estimatedHours: null,
      dueDate: null,
      acceptanceCriteria: null,
      sortOrder: 1,
      tags: [],
      createdAt: '2026-05-28T10:00:00',
      updatedAt: '2026-05-28T10:00:00',
    })
    const board = useBoardStore()

    const task = await board.createTask(7, {
      title: 'Delayed task',
      taskType: 'TASK',
      priority: 'MEDIUM',
      columnId: 1,
    }, {}, { shouldRefresh: () => false })

    expect(task.title).toBe('Delayed task')
    expect(fetchProjectBoard).not.toHaveBeenCalled()
  })
})
