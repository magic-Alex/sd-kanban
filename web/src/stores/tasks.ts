import { defineStore } from 'pinia'
import {
  addTaskComment,
  archiveTask,
  deleteTask,
  fetchTask,
  fetchTaskActivities,
  fetchTaskComments,
  restoreTask,
  updateTask,
  type TaskActivity,
  type TaskComment,
  type TaskResponse,
  type UpdateTaskRequest,
} from '../api/tasks'
import {
  createChecklistItem,
  deleteChecklistItem,
  fetchChecklistItems,
  reorderChecklistItems,
  toggleChecklistItem as toggleChecklistItemApi,
  updateChecklistItem,
  type TaskChecklistItem,
} from '../api/checklist'

function sortChecklistItems(items: TaskChecklistItem[]) {
  return [...items].sort((left, right) => left.sortOrder - right.sortOrder)
}

export const useTasksStore = defineStore('tasks', {
  state: () => ({
    activeTask: null as TaskResponse | null,
    comments: [] as TaskComment[],
    activities: [] as TaskActivity[],
    checklistItems: [] as TaskChecklistItem[],
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
        const [comments, activities, checklistItems] = await Promise.all([
          fetchTaskComments(taskId),
          fetchTaskActivities(taskId),
          fetchChecklistItems(taskId),
        ])
        if (this.drawerOpen && this.openTaskRequestId === requestId) {
          this.activeTask = task
          this.comments = comments
          this.activities = activities
          this.checklistItems = checklistItems
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
    async restoreActiveTask() {
      if (!this.activeTask) {
        return
      }
      const taskId = this.activeTask.id
      this.actionLoading = true
      this.actionError = null
      try {
        await restoreTask(taskId)
        if (this.isCurrentActionTask(taskId)) {
          this.actionLoading = false
          this.closeDrawer()
        }
      } catch (error) {
        if (!this.isCurrentActionTask(taskId)) {
          throw error
        }
        this.actionError = '任务恢复失败，请重试'
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
    async addChecklistItem(title: string) {
      if (!this.activeTask) {
        return
      }
      const item = await createChecklistItem(this.activeTask.id, title)
      this.checklistItems = sortChecklistItems([...this.checklistItems, item])
    },
    async renameChecklistItem(itemId: number, title: string) {
      if (!this.activeTask) {
        return
      }
      const item = await updateChecklistItem(this.activeTask.id, itemId, title)
      this.checklistItems = sortChecklistItems(
        this.checklistItems.map((candidate) => candidate.id === itemId ? item : candidate),
      )
    },
    async toggleChecklistItem(itemId: number) {
      if (!this.activeTask) {
        return
      }
      const item = await toggleChecklistItemApi(this.activeTask.id, itemId)
      this.checklistItems = sortChecklistItems(
        this.checklistItems.map((candidate) => candidate.id === itemId ? item : candidate),
      )
    },
    async removeChecklistItem(itemId: number) {
      if (!this.activeTask) {
        return
      }
      await deleteChecklistItem(this.activeTask.id, itemId)
      this.checklistItems = this.checklistItems.filter((item) => item.id !== itemId)
    },
    async moveChecklistItem(itemId: number, direction: 'up' | 'down') {
      if (!this.activeTask) {
        return
      }
      const items = sortChecklistItems(this.checklistItems)
      const currentIndex = items.findIndex((item) => item.id === itemId)
      const nextIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1
      if (currentIndex < 0 || nextIndex < 0 || nextIndex >= items.length) {
        return
      }
      const [item] = items.splice(currentIndex, 1)
      items.splice(nextIndex, 0, item)
      const reordered = await reorderChecklistItems(this.activeTask.id, items.map((candidate) => candidate.id))
      this.checklistItems = sortChecklistItems(reordered)
    },
  },
})
