import type { UserSummary } from './auth'
import { getData, http, postData } from './http'

export interface TaskChecklistItem {
  id: number
  taskId: number
  projectId: number
  title: string
  done: boolean
  sortOrder: number
  createdBy: UserSummary
  completedBy: UserSummary | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
}

export function fetchChecklistItems(taskId: number): Promise<TaskChecklistItem[]> {
  return getData<TaskChecklistItem[]>(`/tasks/${taskId}/checklist`)
}

export function createChecklistItem(taskId: number, title: string): Promise<TaskChecklistItem> {
  return postData<TaskChecklistItem, { title: string }>(`/tasks/${taskId}/checklist`, { title })
}

export async function updateChecklistItem(taskId: number, itemId: number, title: string): Promise<TaskChecklistItem> {
  const response = await http.patch(`/tasks/${taskId}/checklist/${itemId}`, { title })
  return response.data.data
}

export async function toggleChecklistItem(taskId: number, itemId: number): Promise<TaskChecklistItem> {
  const response = await http.patch(`/tasks/${taskId}/checklist/${itemId}/toggle`)
  return response.data.data
}

export async function reorderChecklistItems(taskId: number, itemIds: number[]): Promise<TaskChecklistItem[]> {
  const response = await http.patch(`/tasks/${taskId}/checklist/reorder`, { itemIds })
  return response.data.data
}

export async function deleteChecklistItem(taskId: number, itemId: number): Promise<void> {
  await http.delete(`/tasks/${taskId}/checklist/${itemId}`)
}
