import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProjectListView from '../src/views/ProjectListView.vue'
import { createProject, fetchProjects } from '../src/api/projects'

const mockedRouter = vi.hoisted(() => ({
  push: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => mockedRouter,
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

const owner = {
  id: 1,
  account: 'owner',
  nickname: 'Owner',
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

describe('ProjectListView', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    setActivePinia(createPinia())
    mockedRouter.push.mockReset()
    vi.mocked(fetchProjects).mockReset()
    vi.mocked(createProject).mockReset()
    vi.mocked(fetchProjects).mockResolvedValue([project])
    vi.mocked(createProject).mockResolvedValue({ ...project, id: 8, name: 'Platform' })
  })

  it('creates a project with code and color metadata', async () => {
    const wrapper = mount(ProjectListView, mountOptions)
    await flushPromises()

    expect(wrapper.get('[aria-label="项目编号"]').exists()).toBe(true)
    expect(wrapper.get('[aria-label="项目颜色"]').exists()).toBe(true)

    await wrapper.get('[aria-label="项目名称"]').setValue('Platform')
    await wrapper.get('[aria-label="项目编号"]').setValue('PF')
    await wrapper.get('[aria-label="项目颜色"]').setValue('#22c55e')
    await wrapper.get('[aria-label="项目描述"]').setValue('Build platform work')
    await wrapper.get('form[aria-label="新建项目表单"]').trigger('submit')
    await flushPromises()

    expect(createProject).toHaveBeenCalledWith({
      name: 'Platform',
      projectCode: 'PF',
      projectColor: '#22c55e',
      description: 'Build platform work',
    })
    expect(mockedRouter.push).toHaveBeenCalledWith('/projects/8')
  })

  it('shows project code and color swatch in the project list', async () => {
    const wrapper = mount(ProjectListView, mountOptions)
    await flushPromises()

    expect(wrapper.text()).toContain('OPS')
    const swatch = wrapper.get('[aria-label="项目颜色 #0ea5e9"]')
    expect((swatch.element as HTMLElement).style.backgroundColor).toBe('#0ea5e9')
  })
})
