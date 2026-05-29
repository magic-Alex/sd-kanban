import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import App from '../src/App.vue'
import router from '../src/router'
import { fetchNotifications, fetchUnreadNotificationCount } from '../src/api/notifications'
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

describe('app shell', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.mocked(fetchNotifications).mockReset()
    vi.mocked(fetchUnreadNotificationCount).mockReset()
    vi.mocked(fetchUnreadNotificationCount).mockResolvedValue({ count: 0 })
  })

  it('renders workspace navigation for authenticated users', async () => {
    localStorage.setItem('sd-kanban-token', 'jwt-token')
    localStorage.setItem(
      'sd-kanban-user',
      JSON.stringify({ id: 1, account: 'alex', nickname: 'Alex' }),
    )
    const pinia = createPinia()
    setActivePinia(pinia)
    router.push('/')
    await router.isReady()

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
    const notifications = useNotificationsStore()
    notifications.unreadCount = 2
    router.push('/')
    await router.isReady()

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
})
