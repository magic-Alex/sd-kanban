import { defineStore } from 'pinia'
import {
  createProject,
  fetchProject,
  fetchProjects,
  type CreateProjectRequest,
  type Project,
} from '../api/projects'

export const useProjectsStore = defineStore('projects', {
  state: () => ({
    projects: [] as Project[],
    currentProject: null as Project | null,
    loading: false,
    error: null as string | null,
  }),
  actions: {
    async fetchProjects() {
      this.loading = true
      this.error = null
      try {
        this.projects = await fetchProjects()
      } catch (error) {
        this.error = '项目列表加载失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    async fetchProject(projectId: number | string) {
      this.loading = true
      this.error = null
      try {
        this.currentProject = await fetchProject(projectId)
      } catch (error) {
        this.error = '项目详情加载失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    async createProject(request: CreateProjectRequest) {
      const project = await createProject(request)
      this.projects = [project, ...this.projects]
      return project
    },
  },
})
