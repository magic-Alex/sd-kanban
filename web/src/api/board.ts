import { getData } from './http'
import type { UserSummary } from './auth'

export interface TaskCard {
  id: number
  projectId: number
  sprintId: number | null
  columnId: number
  assigneeId: number | null
  assignee: UserSummary | null
  title: string
  taskType: string
  priority: string
  storyPoints: number | null
  dueDate: string | null
  sortOrder: number
  checklistDoneCount: number
  checklistTotalCount: number
}

export interface BoardColumn {
  id: number
  name: string
  color: string
  sortOrder: number
  isDone: boolean
  tasks: TaskCard[]
}

export interface ProjectBoard {
  projectId: number
  columns: BoardColumn[]
}

export interface MyTaskBoardGroup {
  id: number
  name: string
  tasks: TaskCard[]
}

export interface MyTaskBoard {
  groupBy: string
  groups: MyTaskBoardGroup[]
}

export interface BoardQuery {
  sprintId?: number | string | null
  assigneeId?: number | string | null
  type?: string
  priority?: string
  keyword?: string
}

function queryString(filters: BoardQuery) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      params.set(key, String(value).trim())
    }
  })
  const query = params.toString()
  return query ? `?${query}` : ''
}

export function fetchProjectBoard(projectId: number | string, filters: BoardQuery = {}): Promise<ProjectBoard> {
  return getData<ProjectBoard>(`/projects/${projectId}/board${queryString(filters)}`)
}

export function fetchMyTaskBoard(groupBy = 'project'): Promise<MyTaskBoard> {
  return getData<MyTaskBoard>(`/tasks/mine/board?groupBy=${encodeURIComponent(groupBy)}`)
}
