import { defineStore } from 'pinia'
import {
  createUser,
  fetchUsers,
  updateUserStatus,
  type CreateUserRequest,
  type UserAdminItem,
} from '../api/users'

export const useUsersStore = defineStore('users', {
  state: () => ({
    users: [] as UserAdminItem[],
    loading: false,
    saving: false,
    error: null as string | null,
  }),
  actions: {
    async load() {
      this.loading = true
      this.error = null
      try {
        this.users = await fetchUsers()
      } catch (error) {
        this.error = '用户列表加载失败'
      } finally {
        this.loading = false
      }
    },
    async create(request: CreateUserRequest) {
      this.saving = true
      this.error = null
      try {
        const user = await createUser(request)
        this.users = [user, ...this.users.filter((item) => item.account !== user.account)]
        return user
      } catch (error) {
        this.error = '用户创建失败'
        throw error
      } finally {
        this.saving = false
      }
    },
    async updateStatus(account: string, status: string) {
      this.error = null
      const updated = await updateUserStatus(account, status)
      this.users = this.users.map((user) => user.account === account ? updated : user)
      return updated
    },
  },
})
