import { defineStore } from 'pinia'
import { addTaskComment, fetchTask, updateTask, type TaskActivity, type TaskComment, type TaskResponse } from '../api/tasks'

export const useTasksStore = defineStore('tasks', {
  state: () => ({
    activeTask: null as TaskResponse | null,
    comments: [] as TaskComment[],
    activities: [] as TaskActivity[],
    drawerOpen: false,
    loading: false,
    error: null as string | null,
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
    async saveTask(update: Partial<TaskResponse>) {
      if (!this.activeTask) {
        return
      }
      this.activeTask = await updateTask(this.activeTask.id, update)
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
