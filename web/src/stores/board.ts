import { defineStore } from 'pinia'
import {
  fetchMyTaskBoard,
  fetchProjectBoard,
  type BoardQuery,
  type MyTaskBoard,
  type ProjectBoard,
  type TaskCard,
} from '../api/board'
import {
  createTask,
  updatePersonalTaskPosition,
  updateTaskPosition,
  type CreateTaskRequest,
  type TaskResponse,
} from '../api/tasks'

function cloneBoard(board: ProjectBoard | null): ProjectBoard | null {
  return board ? JSON.parse(JSON.stringify(board)) : null
}

function cloneMyTaskBoard(board: MyTaskBoard | null): MyTaskBoard | null {
  return board ? JSON.parse(JSON.stringify(board)) : null
}

type CreateTaskOptions = {
  shouldRefresh?: () => boolean
}

export const useBoardStore = defineStore('board', {
  state: () => ({
    projectBoard: null as ProjectBoard | null,
    myTaskBoard: null as MyTaskBoard | null,
    loading: false,
    movingTaskId: null as number | null,
    error: null as string | null,
    lastFilters: {} as BoardQuery,
    lastProjectId: null as number | string | null,
    projectBoardRequestId: 0,
    myTaskBoardRequestId: 0,
  }),
  actions: {
    async loadProjectBoard(projectId: number | string, filters: BoardQuery = {}) {
      const requestId = this.projectBoardRequestId + 1
      this.projectBoardRequestId = requestId
      this.loading = true
      this.error = null
      try {
        const projectBoard = await fetchProjectBoard(projectId, filters)
        if (this.projectBoardRequestId === requestId) {
          this.projectBoard = projectBoard
          this.lastProjectId = projectId
          this.lastFilters = { ...filters }
        }
      } catch (error) {
        if (this.projectBoardRequestId === requestId) {
          this.error = '看板加载失败'
        }
        throw error
      } finally {
        if (this.projectBoardRequestId === requestId) {
          this.loading = false
        }
      }
    },
    clearProjectBoard() {
      this.projectBoardRequestId += 1
      this.projectBoard = null
      this.loading = false
      this.movingTaskId = null
      this.error = null
      this.lastFilters = {}
      this.lastProjectId = null
    },
    async loadMyTaskBoard(groupBy = 'template') {
      const requestId = this.myTaskBoardRequestId + 1
      this.myTaskBoardRequestId = requestId
      this.movingTaskId = null
      this.loading = true
      this.error = null
      try {
        const myTaskBoard = await fetchMyTaskBoard(groupBy)
        if (this.myTaskBoardRequestId === requestId) {
          this.myTaskBoard = myTaskBoard
        }
      } catch (error) {
        if (this.myTaskBoardRequestId === requestId) {
          this.error = '我的任务加载失败'
        }
        throw error
      } finally {
        if (this.myTaskBoardRequestId === requestId) {
          this.loading = false
        }
      }
    },
    async moveTask(taskId: number, columnId: number, sortOrder: number) {
      const previous = cloneBoard(this.projectBoard)
      const boardRequestId = this.projectBoardRequestId
      const boardProjectId = this.projectBoard?.projectId
      const task = this.removeTask(taskId)
      if (!task) {
        return
      }
      this.insertTask({ ...task, columnId, sortOrder }, columnId, sortOrder)
      this.movingTaskId = taskId
      try {
        await updateTaskPosition(taskId, { columnId, sortOrder })
      } catch (error) {
        if (this.projectBoardRequestId === boardRequestId && this.projectBoard?.projectId === boardProjectId) {
          this.projectBoard = previous
        }
        throw error
      } finally {
        if (this.movingTaskId === taskId) {
          this.movingTaskId = null
        }
      }
    },
    async movePersonalTask(taskId: number, targetTemplateKey: string, sortOrder: number) {
      const requestId = this.myTaskBoardRequestId + 1
      this.myTaskBoardRequestId = requestId
      const previous = cloneMyTaskBoard(this.myTaskBoard)
      const task = this.removePersonalTask(taskId)
      if (!task) {
        return
      }

      const inserted = this.insertPersonalTask(
        { ...task, columnTemplateKey: targetTemplateKey, sortOrder },
        targetTemplateKey,
        sortOrder,
      )
      if (!inserted) {
        if (this.myTaskBoardRequestId === requestId) {
          this.myTaskBoard = previous
        }
        return
      }

      this.movingTaskId = taskId
      try {
        const updatedTask = await updatePersonalTaskPosition(taskId, { targetTemplateKey, sortOrder })
        if (this.myTaskBoardRequestId === requestId) {
          this.reconcilePersonalTask(taskId, targetTemplateKey, updatedTask)
        }
      } catch (error) {
        if (this.myTaskBoardRequestId === requestId) {
          this.myTaskBoard = previous
        }
        throw error
      } finally {
        if (this.myTaskBoardRequestId === requestId && this.movingTaskId === taskId) {
          this.movingTaskId = null
        }
      }
    },
    async refreshProjectBoard() {
      if (this.lastProjectId === null) {
        return
      }
      await this.loadProjectBoard(this.lastProjectId, this.lastFilters)
    },
    async markTaskComplete(taskId: number) {
      const doneColumn = this.projectBoard?.columns.find((column) => column.isDone)
      if (!doneColumn) {
        throw new Error('项目暂无完成列')
      }
      await this.moveTask(taskId, doneColumn.id, doneColumn.tasks.length)
    },
    removeTaskFromBoard(taskId: number) {
      this.removeTask(taskId)
    },
    async createTask(
      projectId: number | string,
      request: CreateTaskRequest,
      filters: BoardQuery = {},
      options: CreateTaskOptions = {},
    ) {
      const task = await createTask(projectId, request)
      if (options.shouldRefresh?.() ?? true) {
        await this.loadProjectBoard(projectId, filters)
      }
      return task
    },
    removeTask(taskId: number): TaskCard | null {
      if (!this.projectBoard) {
        return null
      }
      for (const column of this.projectBoard.columns) {
        const index = column.tasks.findIndex((task) => task.id === taskId)
        if (index >= 0) {
          const [task] = column.tasks.splice(index, 1)
          return task
        }
      }
      return null
    },
    insertTask(task: TaskCard, columnId: number, sortOrder: number) {
      const column = this.projectBoard?.columns.find((candidate) => candidate.id === columnId)
      if (!column) {
        return
      }
      column.tasks.splice(sortOrder, 0, task)
      column.tasks.forEach((candidate, index) => {
        candidate.sortOrder = index
      })
    },
    removePersonalTask(taskId: number): TaskCard | null {
      if (!this.myTaskBoard) {
        return null
      }
      for (const group of this.myTaskBoard.groups) {
        const index = group.tasks.findIndex((task) => task.id === taskId)
        if (index >= 0) {
          const [task] = group.tasks.splice(index, 1)
          group.tasks.forEach((candidate, taskIndex) => {
            candidate.sortOrder = taskIndex
          })
          return task
        }
      }
      return null
    },
    insertPersonalTask(task: TaskCard, templateKey: string, sortOrder: number) {
      const group = this.myTaskBoard?.groups.find((candidate) => candidate.templateKey === templateKey)
      if (!group) {
        return false
      }
      const targetIndex = Math.max(0, Math.min(sortOrder, group.tasks.length))
      group.tasks.splice(targetIndex, 0, task)
      group.tasks.forEach((candidate, index) => {
        candidate.sortOrder = index
        candidate.columnTemplateKey = templateKey
      })
      return true
    },
    reconcilePersonalTask(taskId: number, templateKey: string, updatedTask: TaskResponse) {
      const group = this.myTaskBoard?.groups.find((candidate) => candidate.templateKey === templateKey)
      const task = group?.tasks.find((candidate) => candidate.id === taskId)
      if (!task) {
        return
      }
      task.columnId = updatedTask.columnId
      task.sortOrder = updatedTask.sortOrder
      task.columnTemplateKey = templateKey
    },
  },
})
