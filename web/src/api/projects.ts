import type { UserSummary } from './auth'
import { getData, postData } from './http'

export interface Project {
  id: number
  name: string
  description: string | null
  owner: UserSummary
  creator: UserSummary
  status: string
  memberCount: number
  createdAt: string
  updatedAt: string
}

export interface CreateProjectRequest {
  name: string
  description?: string
}

export interface ProjectMember {
  user: UserSummary
  role: string
  joinedAt: string
}

export function fetchProjects(): Promise<Project[]> {
  return getData<Project[]>('/projects')
}

export function fetchProject(projectId: number | string): Promise<Project> {
  return getData<Project>(`/projects/${projectId}`)
}

export function createProject(request: CreateProjectRequest): Promise<Project> {
  return postData<Project, CreateProjectRequest>('/projects', request)
}

export function fetchProjectMembers(projectId: number | string): Promise<ProjectMember[]> {
  return getData<ProjectMember[]>(`/projects/${projectId}/members`)
}
