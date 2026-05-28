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
  }),
  actions: {
    async openTask(taskId: number) {
      this.drawerOpen = true
      this.loading = true
      this.error = null
      try {
        this.activeTask = await fetchTask(taskId)
        this.comments = []
        this.activities = []
      } catch (error) {
        this.error = '任务详情加载失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    closeDrawer() {
      this.drawerOpen = false
    },
    async saveTask(update: UpdateTaskRequest) {
      if (!this.activeTask) {
        return
      }
      this.actionLoading = true
      this.actionError = null
      try {
        this.activeTask = await updateTask(this.activeTask.id, update)
      } catch (error) {
        this.actionError = '任务保存失败，请重试'
        throw error
      } finally {
        this.actionLoading = false
      }
    },
    async archiveActiveTask() {
      if (!this.activeTask) {
        return
      }
      this.actionLoading = true
      this.actionError = null
      try {
        await archiveTask(this.activeTask.id)
        this.closeDrawer()
      } catch (error) {
        this.actionError = '任务归档失败，请重试'
        throw error
      } finally {
        this.actionLoading = false
      }
    },
    async deleteActiveTask() {
      if (!this.activeTask) {
        return
      }
      this.actionLoading = true
      this.actionError = null
      try {
        await deleteTask(this.activeTask.id)
        this.closeDrawer()
      } catch (error) {
        this.actionError = '任务删除失败，请重试'
        throw error
      } finally {
        this.actionLoading = false
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
