import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { fetchMyTaskBoard, fetchProjectBoard } from '../src/api/board'
import { archiveTask, createTask, deleteTask, updateTaskPosition } from '../src/api/tasks'
import { useBoardStore } from '../src/stores/board'

vi.mock('../src/api/board', () => ({
  fetchProjectBoard: vi.fn(),
  fetchMyTaskBoard: vi.fn(),
}))

vi.mock('../src/api/tasks', () => ({
  archiveTask: vi.fn(),
  createTask: vi.fn(),
  deleteTask: vi.fn(),
  updateTaskPosition: vi.fn(),
}))

const projectBoard = {
  projectId: 7,
  columns: [
    {
      id: 1,
      name: 'Ready',
      color: '#0ea5e9',
      sortOrder: 0,
      isDone: false,
      tasks: [
        {
          id: 12,
          projectId: 7,
          sprintId: null,
          columnId: 1,
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
      color: '#22c55e',
      sortOrder: 1,
      isDone: true,
      tasks: [],
    },
  ],
}

describe('board store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(fetchProjectBoard).mockReset()
    vi.mocked(fetchMyTaskBoard).mockReset()
    vi.mocked(archiveTask).mockReset()
    vi.mocked(createTask).mockReset()
    vi.mocked(deleteTask).mockReset()
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

  it('loads my-task board cards', async () => {
    vi.mocked(fetchMyTaskBoard).mockResolvedValue({
      groupBy: 'project',
      groups: [
        {
          id: 7,
          name: 'Delivery',
          tasks: projectBoard.columns[0].tasks,
        },
      ],
    })

    const board = useBoardStore()
    await board.loadMyTaskBoard('project')

    expect(fetchMyTaskBoard).toHaveBeenCalledWith('project')
    expect(board.myTaskBoard?.groups[0].tasks[0].title).toBe('Build board')
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
})
