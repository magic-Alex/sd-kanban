import { defineStore } from 'pinia'
import {
  fetchMyTaskBoard,
  fetchProjectBoard,
  type BoardQuery,
  type ProjectBoard,
  type MyTaskBoard,
  type TaskCard,
} from '../api/board'
import { createTask, updateTaskPosition, type CreateTaskRequest } from '../api/tasks'

function cloneBoard(board: ProjectBoard | null): ProjectBoard | null {
  return board ? JSON.parse(JSON.stringify(board)) : null
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
  }),
  actions: {
    async loadProjectBoard(projectId: number | string, filters: BoardQuery = {}) {
      this.loading = true
      this.error = null
      try {
        this.projectBoard = await fetchProjectBoard(projectId, filters)
        this.lastProjectId = projectId
        this.lastFilters = { ...filters }
      } catch (error) {
        this.error = '看板加载失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    async loadMyTaskBoard(groupBy = 'project') {
      this.loading = true
      this.error = null
      try {
        this.myTaskBoard = await fetchMyTaskBoard(groupBy)
      } catch (error) {
        this.error = '我的任务加载失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    async moveTask(taskId: number, columnId: number, sortOrder: number) {
      const previous = cloneBoard(this.projectBoard)
      const task = this.removeTask(taskId)
      if (!task) {
        return
      }
      this.insertTask({ ...task, columnId, sortOrder }, columnId, sortOrder)
      this.movingTaskId = taskId
      try {
        await updateTaskPosition(taskId, { columnId, sortOrder })
      } catch (error) {
        this.projectBoard = previous
        throw error
      } finally {
        this.movingTaskId = null
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
    async createTask(projectId: number | string, request: CreateTaskRequest, filters: BoardQuery = {}) {
      const task = await createTask(projectId, request)
      await this.loadProjectBoard(projectId, filters)
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
  },
})
