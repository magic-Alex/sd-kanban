import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProjectDetailView from '../src/views/ProjectDetailView.vue'
import {
  addProjectMember,
  fetchProject,
  fetchProjectMembers,
  removeProjectMember,
} from '../src/api/projects'
import { searchActiveUsers } from '../src/api/user-directory'

const mockedRoute = vi.hoisted(() => ({
  params: { projectId: '7' },
}))

vi.mock('vue-router', () => ({
  useRoute: () => mockedRoute,
  RouterLink: {
    props: ['to'],
    template: '<a :href="to"><slot /></a>',
  },
}))

vi.mock('../src/api/projects', () => ({
  addProjectMember: vi.fn(),
  createProject: vi.fn(),
  fetchProject: vi.fn(),
  fetchProjectMembers: vi.fn(),
  fetchProjects: vi.fn(),
  removeProjectMember: vi.fn(),
}))

vi.mock('../src/api/user-directory', () => ({
  searchActiveUsers: vi.fn(),
}))

const owner = {
  id: 1,
  account: 'owner',
  nickname: 'Owner',
  email: null,
  avatarUrl: null,
}

const member = {
  id: 2,
  account: 'developer',
  nickname: 'Developer',
  email: null,
  avatarUrl: null,
}

const project = {
  id: 7,
  projectCode: 'OPS',
  projectColor: '#0ea5e9',
  name: 'Operations',
  description: 'Daily work board',
  owner,
  creator: owner,
  status: 'ACTIVE',
  memberCount: 2,
  createdAt: '2026-05-30T10:00:00',
  updatedAt: '2026-05-30T10:00:00',
}

const members = [
  { user: owner, role: 'OWNER', joinedAt: '2026-05-30T10:00:00' },
  { user: member, role: 'MEMBER', joinedAt: '2026-05-30T11:00:00' },
]

enableAutoUnmount(afterEach)

const mountOptions = {
  attachTo: document.body,
  global: {
    stubs: {
      RouterLink: {
        props: ['to'],
        template: '<a :href="to"><slot /></a>',
      },
    },
  },
}

describe('ProjectDetailView', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    localStorage.setItem('sd-kanban-user', JSON.stringify(owner))
    setActivePinia(createPinia())
    mockedRoute.params.projectId = '7'
    vi.mocked(fetchProject).mockReset()
    vi.mocked(fetchProjectMembers).mockReset()
    vi.mocked(addProjectMember).mockReset()
    vi.mocked(removeProjectMember).mockReset()
    vi.mocked(searchActiveUsers).mockReset()
    vi.mocked(fetchProject).mockResolvedValue(project)
    vi.mocked(fetchProjectMembers).mockResolvedValue(members)
    vi.mocked(addProjectMember).mockResolvedValue({
      user: { id: 3, account: 'tester', nickname: 'Tester', email: null, avatarUrl: null },
      role: 'MEMBER',
      joinedAt: '2026-05-30T12:00:00',
    })
    vi.mocked(removeProjectMember).mockResolvedValue(undefined)
    vi.mocked(searchActiveUsers).mockResolvedValue([
      { id: 3, account: 'tester', nickname: 'Tester', avatarUrl: null },
    ])
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('loads project metadata and renders member management', async () => {
    const wrapper = mount(ProjectDetailView, mountOptions)
    await flushPromises()

    expect(fetchProject).toHaveBeenCalledWith('7')
    expect(fetchProjectMembers).toHaveBeenCalledWith('7')
    expect(wrapper.text()).toContain('OPS')
    expect(wrapper.text()).toContain('负责人')
    expect(wrapper.text()).toContain('成员')
    expect(wrapper.text()).toContain('Developer')
    const swatch = wrapper.get('[aria-label="项目颜色 #0ea5e9"]')
    expect((swatch.element as HTMLElement).style.backgroundColor).toBe('#0ea5e9')
  })

  it('searches active users and wires add and remove member actions', async () => {
    const wrapper = mount(ProjectDetailView, mountOptions)
    await flushPromises()

    await wrapper.get('[aria-label="搜索用户"]').setValue('tester')
    await wrapper.get('form[aria-label="搜索项目成员"]').trigger('submit')
    await flushPromises()

    expect(searchActiveUsers).toHaveBeenCalledWith('tester')
    await wrapper.get('[aria-label="添加 Tester"]').trigger('click')
    await flushPromises()

    expect(addProjectMember).toHaveBeenCalledWith('7', 3)
    expect(fetchProjectMembers).toHaveBeenCalledTimes(2)
    expect(wrapper.get('.large-number').text()).toBe('3')

    await wrapper.get('[aria-label="移除 Developer"]').trigger('click')
    await flushPromises()

    expect(removeProjectMember).toHaveBeenCalledWith('7', 2)
    expect(fetchProjectMembers).toHaveBeenCalledTimes(3)
    expect(wrapper.get('.large-number').text()).toBe('2')
    expect(wrapper.find('[aria-label="移除 Owner"]').exists()).toBe(false)
  })

  it('does not search the user directory for a blank keyword', async () => {
    const wrapper = mount(ProjectDetailView, mountOptions)
    await flushPromises()

    await wrapper.get('form[aria-label="搜索项目成员"]').trigger('submit')
    await flushPromises()

    expect(searchActiveUsers).not.toHaveBeenCalled()
  })
})
