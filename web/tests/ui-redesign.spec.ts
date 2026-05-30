import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import App from '../src/App.vue'
import TaskCard from '../src/components/board/TaskCard.vue'
import LoginView from '../src/views/LoginView.vue'
import { useAuthStore } from '../src/stores/auth'

vi.mock('../src/api/auth', () => ({
  fetchCurrentUser: vi.fn(async () => ({
    id: 1,
    account: 'sd-robot',
    nickname: '系统管理员',
    email: null,
    avatarUrl: null,
    role: 'ADMIN',
  })),
  login: vi.fn(),
}))

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
  fetchNotifications: vi.fn(async () => []),
  fetchUnreadNotificationCount: vi.fn(async () => ({ count: 0 })),
  markNotificationRead: vi.fn(),
  markAllNotificationsRead: vi.fn(),
}))

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'dashboard', component: { template: '<main />' }, meta: { requiresAuth: true } },
      { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
      { path: '/projects', name: 'projects', component: { template: '<main />' }, meta: { requiresAuth: true } },
      { path: '/my-tasks', name: 'my-tasks', component: { template: '<main />' }, meta: { requiresAuth: true } },
      { path: '/admin/users', name: 'admin-users', component: { template: '<main />' }, meta: { requiresAuth: true, requiresAdmin: true } },
      { path: '/admin/settings/board-template', name: 'admin-board-template-settings', component: { template: '<main />' }, meta: { requiresAuth: true, requiresAdmin: true } },
    ],
  })
}

function authenticateAdmin() {
  const auth = useAuthStore()
  auth.token = 'jwt-token'
  auth.user = {
    id: 1,
    account: 'sd-robot',
    nickname: '系统管理员',
    email: null,
    avatarUrl: null,
    role: 'ADMIN',
  }
}

describe('precision workspace redesign', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('renders the login page as a product workspace entry with the default admin hint', () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [createPinia(), createTestRouter()],
      },
    })

    expect(wrapper.find('.login-visual-panel').exists()).toBe(true)
    expect(wrapper.find('.login-credential-hint').text()).toContain('默认管理员：sd-robot / 1')
    expect(wrapper.text()).toContain('敏捷交付工作台')
  })

  it('renders a responsive app shell with workspace subtitle and compact mobile header', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    authenticateAdmin()
    const router = createTestRouter()
    await router.push('/')

    const wrapper = mount(App, {
      global: {
        plugins: [pinia, router],
      },
    })
    await flushPromises()

    expect(wrapper.find('.workspace-shell.precision-shell').exists()).toBe(true)
    expect(wrapper.find('.brand-subtitle').text()).toBe('敏捷交付工作台')
    expect(wrapper.find('.mobile-shell-header').exists()).toBe(true)
    expect(wrapper.text()).toContain('系统管理员')
  })

  it('gives task cards dedicated readable metadata regions', () => {
    const wrapper = mount(TaskCard, {
      props: {
        task: {
          id: 12,
          projectId: 7,
          projectCode: null,
          projectName: null,
          projectColor: null,
          sprintId: null,
          columnId: 1,
          columnTemplateKey: 'READY',
          assigneeId: 3,
          assignee: {
            id: 3,
            account: 'mei',
            nickname: 'Mei',
            email: 'mei@example.com',
            avatarUrl: null,
          },
          title: '修复任务抽屉布局',
          taskType: 'BUG',
          priority: 'HIGH',
          storyPoints: 3,
          dueDate: '2026-06-01',
          sortOrder: 0,
          checklistDoneCount: 1,
          checklistTotalCount: 3,
        },
      },
    })

    expect(wrapper.find('.task-priority').text()).toContain('高')
    expect(wrapper.find('.task-assignee').text()).toContain('Mei')
    expect(wrapper.find('.task-due-date').text()).toContain('2026-06-01')
    expect(wrapper.find('.task-checklist-progress').text()).toContain('清单 1/3')
  })
})
