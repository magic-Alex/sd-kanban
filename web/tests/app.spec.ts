import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import App from '../src/App.vue'
import { fetchCurrentUser } from '../src/api/auth'
import { fetchNotifications, fetchUnreadNotificationCount, markNotificationRead } from '../src/api/notifications'
import { useAuthStore } from '../src/stores/auth'
import { useNotificationsStore } from '../src/stores/notifications'

vi.mock('../src/api/dashboard', () => ({
  fetchDashboardSummary: vi.fn(async () => ({
    pendingTaskCount: 0,
    overdueTaskCount: 0,
    ownedProjectCount: 0,
    joinedProjectCount: 0,
    recentActivities: [],
  })),
  fetchDashboardTrends: vi.fn(async () => ({
    buckets: [],
  })),
}))

vi.mock('../src/api/notifications', () => ({
  fetchNotifications: vi.fn(),
  fetchUnreadNotificationCount: vi.fn(),
  markNotificationRead: vi.fn(),
  markAllNotificationsRead: vi.fn(),
}))

vi.mock('../src/api/auth', () => ({
  fetchCurrentUser: vi.fn(),
  login: vi.fn(),
}))

function deferred<T>() {
  let resolve: (value: T) => void = () => undefined
  let reject: (error: Error) => void = () => undefined
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })
  return { promise, resolve, reject }
}

function authenticateTestUser(role = 'MEMBER') {
  const auth = useAuthStore()
  auth.token = 'jwt-token'
  auth.user = { id: 1, account: 'alex', nickname: 'Alex', role }
}

function createTestRouter() {
  const testRouter = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<main />' }, meta: { requiresAuth: true } },
      { path: '/login', name: 'login', component: { template: '<main />' }, meta: { public: true } },
      { path: '/projects', name: 'projects', component: { template: '<main />' }, meta: { requiresAuth: true } },
      { path: '/projects/:projectId/board', name: 'project-board', component: { template: '<main />' }, meta: { requiresAuth: true } },
      { path: '/my-tasks', name: 'my-tasks', component: { template: '<main />' }, meta: { requiresAuth: true } },
      { path: '/admin/users', name: 'admin-users', component: { template: '<main />' }, meta: { requiresAuth: true, requiresAdmin: true } },
      { path: '/admin/settings/board-template', name: 'admin-board-template-settings', component: { template: '<main />' }, meta: { requiresAuth: true, requiresAdmin: true } },
    ],
  })
  testRouter.beforeEach((to) => {
    const auth = useAuthStore()
    if (to.meta.requiresAuth && !auth.isAuthenticated) {
      return {
        name: 'login',
        query: { redirect: to.fullPath },
      }
    }
    if (to.name === 'login' && auth.isAuthenticated) {
      return { name: 'dashboard' }
    }
    if (to.meta.requiresAdmin && !auth.isAdmin) {
      return { name: 'dashboard' }
    }
    return true
  })
  return testRouter
}

describe('app shell', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.mocked(fetchCurrentUser).mockReset()
    vi.mocked(fetchCurrentUser).mockResolvedValue({
      id: 1,
      account: 'alex',
      nickname: 'Alex',
      email: 'alex@sd-robot.com',
      avatarUrl: null,
      role: 'MEMBER',
    })
    vi.mocked(fetchNotifications).mockReset()
    vi.mocked(fetchUnreadNotificationCount).mockReset()
    vi.mocked(fetchUnreadNotificationCount).mockResolvedValue({ count: 0 })
    vi.mocked(markNotificationRead).mockReset()
    vi.mocked(markNotificationRead).mockResolvedValue({
      id: 1,
      actor: null,
      projectId: 7,
      taskId: 12,
      type: 'MENTION',
      title: '有人提到了你',
      content: '任务评论提到了你，请查看最新讨论。',
      read: true,
      createdAt: '2026-05-29T10:00:00',
      readAt: '2026-05-29T10:05:00',
    })
  })

  afterEach(async () => {
    useAuthStore().logout()
    localStorage.clear()
  })

  it('renders workspace navigation for authenticated users', async () => {
    localStorage.setItem('sd-kanban-token', 'jwt-token')
    localStorage.setItem(
      'sd-kanban-user',
      JSON.stringify({ id: 1, account: 'alex', nickname: 'Alex' }),
    )
    const pinia = createPinia()
    setActivePinia(pinia)
    authenticateTestUser()
    const router = createTestRouter()
    await router.push('/')

    const wrapper = mount(App, {
      global: {
        plugins: [pinia, router],
      },
    })
    await flushPromises()

    expect(wrapper.find('.brand-mark').text()).toContain('SD')
    expect(wrapper.find('.brand-mark').text()).toContain('Kanban')
    expect(wrapper.text()).toContain('仪表盘')
    expect(wrapper.text()).toContain('项目')
    expect(wrapper.text()).toContain('我的任务')
    expect(wrapper.text()).toContain('退出')
  })

  it('shows user management navigation for administrators only', async () => {
    localStorage.setItem('sd-kanban-token', 'jwt-token')
    localStorage.setItem(
      'sd-kanban-user',
      JSON.stringify({ id: 1, account: 'sd-robot', nickname: '系统管理员', role: 'ADMIN' }),
    )
    vi.mocked(fetchCurrentUser).mockResolvedValue({
      id: 1,
      account: 'sd-robot',
      nickname: '系统管理员',
      email: null,
      avatarUrl: null,
      role: 'ADMIN',
    })
    const pinia = createPinia()
    setActivePinia(pinia)
    authenticateTestUser('ADMIN')
    const router = createTestRouter()
    await router.push('/')

    const wrapper = mount(App, {
      global: {
        plugins: [pinia, router],
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('用户管理')
  })

  it('shows system settings navigation for administrators only', async () => {
    localStorage.setItem('sd-kanban-token', 'jwt-token')
    localStorage.setItem(
      'sd-kanban-user',
      JSON.stringify({ id: 1, account: 'sd-robot', nickname: '系统管理员', role: 'ADMIN' }),
    )
    vi.mocked(fetchCurrentUser).mockResolvedValue({
      id: 1,
      account: 'sd-robot',
      nickname: '系统管理员',
      email: null,
      avatarUrl: null,
      role: 'ADMIN',
    })
    const pinia = createPinia()
    setActivePinia(pinia)
    authenticateTestUser('ADMIN')
    const router = createTestRouter()
    await router.push('/')

    const adminWrapper = mount(App, {
      global: {
        plugins: [pinia, router],
      },
    })
    await flushPromises()

    expect(adminWrapper.text()).toContain('系统设置')
    expect(adminWrapper.find('a[href="/admin/settings/board-template"]').exists()).toBe(true)

    adminWrapper.unmount()
    vi.mocked(fetchCurrentUser).mockResolvedValue({
      id: 1,
      account: 'alex',
      nickname: 'Alex',
      email: 'alex@sd-robot.com',
      avatarUrl: null,
      role: 'MEMBER',
    })
    const memberPinia = createPinia()
    setActivePinia(memberPinia)
    authenticateTestUser('MEMBER')
    const memberRouter = createTestRouter()
    await memberRouter.push('/')

    const memberWrapper = mount(App, {
      global: {
        plugins: [memberPinia, memberRouter],
      },
    })
    await flushPromises()

    expect(memberWrapper.text()).not.toContain('系统设置')
  })

  it('redirects non-admin users away from board template settings', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    authenticateTestUser('MEMBER')
    const router = createTestRouter()

    await router.push('/admin/settings/board-template')
    await router.isReady()

    expect(router.currentRoute.value.name).toBe('dashboard')
  })

  it('opens the notification panel from the authenticated shell', async () => {
    localStorage.setItem('sd-kanban-token', 'jwt-token')
    localStorage.setItem(
      'sd-kanban-user',
      JSON.stringify({ id: 1, account: 'alex', nickname: 'Alex' }),
    )
    vi.mocked(fetchUnreadNotificationCount).mockResolvedValue({ count: 2 })
    vi.mocked(fetchNotifications).mockResolvedValue([
      {
        id: 1,
        actor: null,
        projectId: 7,
        taskId: 12,
        type: 'MENTION',
        title: '有人提到了你',
        content: '任务评论提到了你，请查看最新讨论。',
        read: false,
        createdAt: '2026-05-29T10:00:00',
        readAt: null,
      },
    ])
    const pinia = createPinia()
    setActivePinia(pinia)
    authenticateTestUser()
    const notifications = useNotificationsStore()
    notifications.unreadCount = 2
    const router = createTestRouter()
    await router.push('/')

    const wrapper = mount(App, {
      global: {
        plugins: [pinia, router],
      },
    })
    await flushPromises()

    const notificationButton = wrapper.get('[aria-label="通知"]')
    expect(notificationButton.text()).toContain('2')

    await notificationButton.trigger('click')
    await flushPromises()

    expect(wrapper.get('[aria-label="通知列表"]').text()).toContain('有人提到了你')
    expect(wrapper.get('[aria-label="通知列表"]').text()).toContain('任务评论提到了你')
  })

  it('opens the related task route from a task notification', async () => {
    localStorage.setItem('sd-kanban-token', 'jwt-token')
    localStorage.setItem(
      'sd-kanban-user',
      JSON.stringify({ id: 1, account: 'alex', nickname: 'Alex' }),
    )
    vi.mocked(fetchNotifications).mockResolvedValue([
      {
        id: 1,
        actor: null,
        projectId: 7,
        taskId: 12,
        type: 'MENTION',
        title: '有人提到了你',
        content: '任务评论提到了你，请查看最新讨论。',
        read: false,
        createdAt: '2026-05-29T10:00:00',
        readAt: null,
      },
    ])
    const pinia = createPinia()
    setActivePinia(pinia)
    authenticateTestUser()
    const router = createTestRouter()
    await router.push('/')

    const wrapper = mount(App, {
      global: {
        plugins: [pinia, router],
      },
    })
    await flushPromises()

    await wrapper.get('[aria-label="通知"]').trigger('click')
    await flushPromises()
    await wrapper.get('[aria-label="通知列表"]').get('button.notification-content-button').trigger('click')
    await flushPromises()

    expect(markNotificationRead).toHaveBeenCalledWith(1)
    expect(router.currentRoute.value.fullPath).toBe('/projects/7/board?taskId=12')
    expect(wrapper.find('[aria-label="通知列表"]').exists()).toBe(false)
  })

  it('does not refresh unread count when a pending notification load completes after logout', async () => {
    localStorage.setItem('sd-kanban-token', 'jwt-token')
    localStorage.setItem(
      'sd-kanban-user',
      JSON.stringify({ id: 1, account: 'alex', nickname: 'Alex' }),
    )
    vi.mocked(fetchUnreadNotificationCount).mockResolvedValue({ count: 2 })
    const pendingNotifications = deferred<Awaited<ReturnType<typeof fetchNotifications>>>()
    vi.mocked(fetchNotifications).mockReturnValue(pendingNotifications.promise)
    const pinia = createPinia()
    setActivePinia(pinia)
    authenticateTestUser()
    const router = createTestRouter()
    await router.push('/')

    const wrapper = mount(App, {
      global: {
        plugins: [pinia, router],
      },
    })
    await flushPromises()

    await wrapper.get('[aria-label="通知"]').trigger('click')
    const logoutButton = wrapper.findAll('button').find((button) => button.text().includes('退出'))
    expect(logoutButton?.exists()).toBe(true)
    await logoutButton?.trigger('click')
    await flushPromises()
    pendingNotifications.resolve([])
    await flushPromises()

    expect(fetchUnreadNotificationCount).toHaveBeenCalledTimes(1)
  })
})
