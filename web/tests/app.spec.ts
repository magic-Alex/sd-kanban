import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import App from '../src/App.vue'
import router from '../src/router'

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

describe('app shell', () => {
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
})
