import type { UserSummary } from './auth'
import { deleteData, getData, postData } from './http'

export interface Project {
  id: number
  projectCode: string
  projectColor: string
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
  projectCode: string
  projectColor: string
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

export function addProjectMember(projectId: number | string, userId: number): Promise<ProjectMember> {
  return postData<ProjectMember, { userId: number }>(`/projects/${projectId}/members`, { userId })
}

export function removeProjectMember(projectId: number | string, userId: number | string): Promise<void> {
  return deleteData<void>(`/projects/${projectId}/members/${userId}`)
}
