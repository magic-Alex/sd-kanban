import { defineStore } from 'pinia'
import {
  addProjectMember,
  createProject,
  fetchProject,
  fetchProjectMembers,
  fetchProjects,
  removeProjectMember,
  type CreateProjectRequest,
  type Project,
  type ProjectMember,
} from '../api/projects'

export const useProjectsStore = defineStore('projects', {
  state: () => ({
    projects: [] as Project[],
    currentProject: null as Project | null,
    members: [] as ProjectMember[],
    loading: false,
    membersLoading: false,
    error: null as string | null,
    createError: null as string | null,
    memberActionLoading: false,
    memberActionError: null as string | null,
  }),
  actions: {
    async fetchProjects() {
      this.loading = true
      this.error = null
      this.createError = null
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
      this.memberActionError = null
      try {
        this.currentProject = await fetchProject(projectId)
      } catch (error) {
        this.error = '项目详情加载失败'
        this.currentProject = null
        this.members = []
        throw error
      } finally {
        this.loading = false
      }
    },
    async createProject(request: CreateProjectRequest) {
      this.createError = null
      try {
        const project = await createProject(request)
        this.projects = [project, ...this.projects]
        return project
      } catch (error) {
        this.createError = '项目创建失败，请检查项目编号是否重复或表单内容是否有效'
        throw error
      }
    },
    async fetchMembers(projectId: number | string) {
      this.membersLoading = true
      this.memberActionError = null
      this.members = []
      try {
        this.members = await fetchProjectMembers(projectId)
      } catch (error) {
        this.memberActionError = '项目成员加载失败'
        throw error
      } finally {
        this.membersLoading = false
      }
    },
    async addMember(projectId: number | string, userId: number) {
      if (this.memberActionLoading) {
        return undefined
      }
      this.memberActionError = null
      const existed = this.members.some((item) => item.user.id === userId)
      if (existed) {
        return this.members.find((item) => item.user.id === userId)
      }
      this.memberActionLoading = true
      try {
        const member = await addProjectMember(projectId, userId)
        this.members = [...this.members, member]
        if (this.currentProject) {
          this.currentProject = {
            ...this.currentProject,
            memberCount: this.currentProject.memberCount + 1,
          }
        }
        return member
      } catch (error) {
        this.memberActionError = '项目成员添加失败'
        throw error
      } finally {
        this.memberActionLoading = false
      }
    },
    async removeMember(projectId: number | string, userId: number | string) {
      if (this.memberActionLoading) {
        return
      }
      this.memberActionError = null
      const numericUserId = Number(userId)
      const existed = this.members.some((member) => member.user.id === numericUserId)
      if (!existed) {
        return
      }
      this.memberActionLoading = true
      try {
        await removeProjectMember(projectId, userId)
        this.members = this.members.filter((member) => member.user.id !== numericUserId)
        if (this.currentProject && existed) {
          this.currentProject = {
            ...this.currentProject,
            memberCount: Math.max(0, this.currentProject.memberCount - 1),
          }
        }
      } catch (error) {
        this.memberActionError = '项目成员移除失败'
        throw error
      } finally {
        this.memberActionLoading = false
      }
    },
  },
})
