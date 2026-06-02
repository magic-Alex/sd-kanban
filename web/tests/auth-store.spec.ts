import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import router from '../src/router'
import { fetchCurrentUser, login as loginApi } from '../src/api/auth'
import { useAuthStore } from '../src/stores/auth'

vi.mock('../src/api/auth', () => ({
  login: vi.fn(),
  fetchCurrentUser: vi.fn(),
}))

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(loginApi).mockReset()
    vi.mocked(fetchCurrentUser).mockReset()
  })

  it('saves token after login', async () => {
    vi.mocked(loginApi).mockResolvedValue({
      token: 'jwt-token',
      user: {
        id: 1,
        account: 'alex',
        nickname: 'Alex',
        email: 'alex@example.com',
        avatarUrl: null,
        role: 'MEMBER',
      },
    })

    const auth = useAuthStore()
    await auth.login({ account: 'alex', password: 'secret123' })

    expect(auth.token).toBe('jwt-token')
    expect(auth.user?.nickname).toBe('Alex')
    expect(localStorage.getItem('sd-kanban-token')).toBe('jwt-token')
  })

  it('clears token after logout', () => {
    localStorage.setItem('sd-kanban-token', 'jwt-token')
    localStorage.setItem('sd-kanban-user', JSON.stringify({ id: 1, account: 'alex', nickname: 'Alex' }))
    const auth = useAuthStore()

    auth.logout()

    expect(auth.token).toBeNull()
    expect(auth.user).toBeNull()
    expect(localStorage.getItem('sd-kanban-token')).toBeNull()
    expect(localStorage.getItem('sd-kanban-user')).toBeNull()
  })

  it('refreshes the cached current user from the server', async () => {
    localStorage.setItem('sd-kanban-token', 'jwt-token')
    localStorage.setItem(
      'sd-kanban-user',
      JSON.stringify({ id: 1, account: 'admin', nickname: '???', role: 'ADMIN' }),
    )
    vi.mocked(fetchCurrentUser).mockResolvedValue({
      id: 1,
      account: 'admin',
      nickname: '管理员',
      email: 'admin@sd-kanban.local',
      avatarUrl: null,
      role: 'ADMIN',
    })

    const auth = useAuthStore()
    await auth.refreshCurrentUser()

    expect(fetchCurrentUser).toHaveBeenCalledTimes(1)
    expect(auth.user?.nickname).toBe('管理员')
    expect(JSON.parse(localStorage.getItem('sd-kanban-user') ?? '{}').nickname).toBe('管理员')
  })
})

describe('router guard', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('redirects anonymous users to login', async () => {
    await router.push('/projects')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/projects')
  })
})
