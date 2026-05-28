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
  createdAt: string
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

export async function updateTask(taskId: number, request: Partial<TaskResponse>): Promise<TaskResponse> {
  const response = await http.patch(`/tasks/${taskId}`, request)
  return response.data.data
}

export function addTaskComment(taskId: number, content: string): Promise<TaskComment> {
  return postData<TaskComment, { content: string }>(`/tasks/${taskId}/comments`, { content })
}
