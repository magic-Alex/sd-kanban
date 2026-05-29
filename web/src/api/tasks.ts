import type { UserSummary } from './auth'
import { getData, http, postData } from './http'

export interface TaskTag {
  id: number
  projectId: number
  name: string
  color: string
}

export interface TaskResponse {
  id: number
  projectId: number
  sprintId: number | null
  columnId: number
  assignee: UserSummary | null
  creator: UserSummary
  title: string
  description: string | null
  taskType: string
  priority: string
  storyPoints: number | null
  estimatedHours: number | null
  dueDate: string | null
  acceptanceCriteria: string | null
  sortOrder: number
  tags: TaskTag[]
  createdAt: string
  updatedAt: string
}

export interface UpdateTaskPositionRequest {
  columnId: number
  sortOrder: number
}

export interface CreateTaskRequest {
  title: string
  description?: string | null
  taskType?: string | null
  priority?: string | null
  storyPoints?: number | null
  estimatedHours?: number | null
  dueDate?: string | null
  acceptanceCriteria?: string | null
  assigneeId?: number | null
  sprintId?: number | null
  columnId: number
  tagIds?: number[]
}

export interface UpdateTaskRequest {
  title?: string
  description?: string | null
  taskType?: string
  priority?: string
  storyPoints?: number | null
  estimatedHours?: number | null
  dueDate?: string | null
  acceptanceCriteria?: string | null
  assigneeId?: number | null
  sprintId?: number | null
  columnId?: number
  clearFields?: string[]
}

export interface TaskComment {
  id: number
  taskId: number
  author: UserSummary
  content: string
  createdAt: string
  updatedAt: string
}

export interface TaskActivity {
  id: number
  taskId: number
  actor: UserSummary | null
  actionType: string
  fieldName: string | null
  oldValue: string | null
  newValue: string | null
  displayText: string
  createdAt: string
}

export interface ArchivedTaskQuery {
  assigneeId?: number | string | null
  type?: string
  priority?: string
  keyword?: string
}

function queryString(filters: ArchivedTaskQuery) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      params.set(key, String(value).trim())
    }
  })
  const query = params.toString()
  return query ? `?${query}` : ''
}

export function fetchTask(taskId: number | string): Promise<TaskResponse> {
  return getData<TaskResponse>(`/tasks/${taskId}`)
}

export function createTask(projectId: number | string, request: CreateTaskRequest): Promise<TaskResponse> {
  return postData<TaskResponse, CreateTaskRequest>(`/projects/${projectId}/tasks`, request)
}

export async function updateTaskPosition(
  taskId: number,
  request: UpdateTaskPositionRequest,
): Promise<unknown> {
  const response = await http.patch(`/tasks/${taskId}/position`, request)
  return response.data.data
}

export async function updateTask(taskId: number, request: UpdateTaskRequest): Promise<TaskResponse> {
  const response = await http.patch(`/tasks/${taskId}`, request)
  return response.data.data
}

export async function archiveTask(taskId: number): Promise<TaskResponse> {
  const response = await http.patch(`/tasks/${taskId}/archive`)
  return response.data.data
}

export async function restoreTask(taskId: number): Promise<TaskResponse> {
  const response = await http.patch(`/tasks/${taskId}/restore`)
  return response.data.data
}

export async function deleteTask(taskId: number): Promise<void> {
  await http.delete(`/tasks/${taskId}`)
}

export function addTaskComment(taskId: number, content: string): Promise<TaskComment> {
  return postData<TaskComment, { content: string }>(`/tasks/${taskId}/comments`, { content })
}

export function fetchTaskComments(taskId: number): Promise<TaskComment[]> {
  return getData<TaskComment[]>(`/tasks/${taskId}/comments`)
}

export function fetchTaskActivities(taskId: number): Promise<TaskActivity[]> {
  return getData<TaskActivity[]>(`/tasks/${taskId}/activities`)
}

export function fetchArchivedTasks(
  projectId: number | string,
  filters: ArchivedTaskQuery = {},
): Promise<TaskResponse[]> {
  return getData<TaskResponse[]>(`/projects/${projectId}/tasks/archived${queryString(filters)}`)
}
