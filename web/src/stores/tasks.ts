import { defineStore } from 'pinia'
import {
  addTaskComment,
  archiveTask,
  deleteTask,
  fetchTask,
  updateTask,
  type TaskActivity,
  type TaskComment,
  type TaskResponse,
  type UpdateTaskRequest,
} from '../api/tasks'

export const useTasksStore = defineStore('tasks', {
  state: () => ({
    activeTask: null as TaskResponse | null,
    comments: [] as TaskComment[],
    activities: [] as TaskActivity[],
    drawerOpen: false,
    loading: false,
    error: null as string | null,
    actionLoading: false,
    actionError: null as string | null,
    openTaskRequestId: 0,
  }),
  actions: {
    async openTask(taskId: number) {
      const requestId = this.openTaskRequestId + 1
      this.openTaskRequestId = requestId
      this.drawerOpen = true
      this.loading = true
      this.error = null
      this.actionLoading = false
      this.actionError = null
      try {
        const task = await fetchTask(taskId)
        if (this.drawerOpen && this.openTaskRequestId === requestId) {
          this.activeTask = task
          this.comments = []
          this.activities = []
        }
      } catch (error) {
        if (this.drawerOpen && this.openTaskRequestId === requestId) {
          this.error = '任务详情加载失败'
        }
        throw error
      } finally {
        if (this.openTaskRequestId === requestId) {
          this.loading = false
        }
      }
    },
    closeDrawer() {
      this.drawerOpen = false
    },
    isCurrentActionTask(taskId: number) {
      return this.drawerOpen && this.activeTask?.id === taskId
    },
    async saveTask(update: UpdateTaskRequest) {
      if (!this.activeTask) {
        return
      }
      const taskId = this.activeTask.id
      this.actionLoading = true
      this.actionError = null
      try {
        const task = await updateTask(taskId, update)
        if (this.isCurrentActionTask(taskId)) {
          this.activeTask = task
          this.actionLoading = false
        }
      } catch (error) {
        if (!this.isCurrentActionTask(taskId)) {
          throw error
        }
        if (this.isCurrentActionTask(taskId)) {
          this.actionError = '任务保存失败，请重试'
          this.actionLoading = false
        }
        throw error
      }
    },
    async archiveActiveTask() {
      if (!this.activeTask) {
        return
      }
      const taskId = this.activeTask.id
      this.actionLoading = true
      this.actionError = null
      try {
        await archiveTask(taskId)
        if (this.isCurrentActionTask(taskId)) {
          this.actionLoading = false
          this.closeDrawer()
        }
      } catch (error) {
        if (!this.isCurrentActionTask(taskId)) {
          throw error
        }
        this.actionError = '任务归档失败，请重试'
        this.actionLoading = false
        throw error
      }
    },
    async deleteActiveTask() {
      if (!this.activeTask) {
        return
      }
      const taskId = this.activeTask.id
      this.actionLoading = true
      this.actionError = null
      try {
        await deleteTask(taskId)
        if (this.isCurrentActionTask(taskId)) {
          this.actionLoading = false
          this.closeDrawer()
        }
      } catch (error) {
        if (!this.isCurrentActionTask(taskId)) {
          throw error
        }
        this.actionError = '任务删除失败，请重试'
        this.actionLoading = false
        throw error
      }
    },
    async addComment(content: string) {
      if (!this.activeTask) {
        return
      }
      const comment = await addTaskComment(this.activeTask.id, content)
      this.comments = [comment, ...this.comments]
    },
  },
})
