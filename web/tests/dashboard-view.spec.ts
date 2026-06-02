import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import DashboardView from '../src/views/DashboardView.vue'

vi.mock('../src/api/dashboard', () => ({
  fetchDashboardSummary: vi.fn(async () => ({
    pendingTaskCount: 3,
    overdueTaskCount: 1,
    ownedProjectCount: 2,
    joinedProjectCount: 4,
    recentActivities: [
      {
        id: 9,
        taskId: 12,
        projectId: 7,
        projectName: 'Delivery',
        taskTitle: 'Build dashboard',
        actor: { id: 1, account: 'alex', nickname: 'Alex', email: 'alex@example.com', avatarUrl: null },
        actionType: 'TASK_UPDATED',
        fieldName: 'priority',
        oldValue: 'MEDIUM',
        newValue: 'HIGH',
        createdAt: '2026-05-21T10:00:00',
      },
    ],
  })),
  fetchDashboardTrends: vi.fn(async () => ({
    buckets: [
      { date: '2026-05-15', completedCount: 0 },
      { date: '2026-05-16', completedCount: 1 },
      { date: '2026-05-17', completedCount: 2 },
    ],
  })),
}))

describe('DashboardView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders my pending tasks and owned projects from the API', async () => {
    const wrapper = mount(DashboardView, {
      global: {
        stubs: {
          RouterLink: true,
        },
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('待处理任务')
    expect(wrapper.text()).toContain('3')
    expect(wrapper.text()).toContain('逾期任务')
    expect(wrapper.text()).toContain('1')
    expect(wrapper.text()).toContain('负责项目')
    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).toContain('参与项目')
    expect(wrapper.text()).toContain('4')
    expect(wrapper.text()).toContain('Build dashboard')
  })
})
