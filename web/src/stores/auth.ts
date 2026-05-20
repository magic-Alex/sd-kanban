import { defineStore } from 'pinia'
import { login as loginApi, type LoginRequest, type UserSummary } from '../api/auth'

const TOKEN_KEY = 'sd-kanban-token'
const USER_KEY = 'sd-kanban-user'

function readUser(): UserSummary | null {
  const value = localStorage.getItem(USER_KEY)
  if (!value) {
    return null
  }
  try {
    return JSON.parse(value) as UserSummary
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) as string | null,
    user: readUser() as UserSummary | null,
    loading: false,
    error: null as string | null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
  },
  actions: {
    async login(request: LoginRequest) {
      this.loading = true
      this.error = null
      try {
        const response = await loginApi(request)
        this.token = response.token
        this.user = response.user
        localStorage.setItem(TOKEN_KEY, response.token)
        localStorage.setItem(USER_KEY, JSON.stringify(response.user))
      } catch (error) {
        this.error = '账号或密码不正确'
        throw error
      } finally {
        this.loading = false
      }
    },
    logout() {
      this.token = null
      this.user = null
      this.error = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})
