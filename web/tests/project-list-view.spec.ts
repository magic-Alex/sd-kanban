import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import type { VueWrapper } from '@vue/test-utils'
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

function projectFormControls(wrapper: VueWrapper) {
  return {
    form: wrapper.get('form.project-form'),
    name: wrapper.get('input[required][maxlength="120"]'),
    code: wrapper.get('input[autocomplete="off"]'),
    color: wrapper.get('input[type="color"]'),
    description: wrapper.get('textarea'),
  }
}

async function fillProjectForm(wrapper: VueWrapper) {
  const controls = projectFormControls(wrapper)
  await controls.name.setValue('Platform')
  await controls.code.setValue('PF')
  await controls.color.setValue('#22c55e')
  await controls.description.setValue('Build platform work')
  return controls
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

    const controls = await fillProjectForm(wrapper)
    await controls.form.trigger('submit')
    await flushPromises()

    expect(createProject).toHaveBeenCalledWith({
      name: 'Platform',
      projectCode: 'PF',
      projectColor: '#22c55e',
      description: 'Build platform work',
    })
    expect(mockedRouter.push).toHaveBeenCalledWith('/projects/8')
    expect((controls.name.element as HTMLInputElement).value).toBe('')
    expect((controls.code.element as HTMLInputElement).value).toBe('')
    expect((controls.color.element as HTMLInputElement).value).toBe('#0ea5e9')
    expect((controls.description.element as HTMLTextAreaElement).value).toBe('')
  })

  it('shows create failures and preserves entered form values', async () => {
    vi.mocked(createProject).mockRejectedValueOnce(new Error('duplicate project code'))
    const wrapper = mount(ProjectListView, mountOptions)
    await flushPromises()

    const controls = await fillProjectForm(wrapper)
    await controls.form.trigger('submit')
    await flushPromises()

    expect(wrapper.find('.form-error').text()).not.toBe('')
    expect(mockedRouter.push).not.toHaveBeenCalled()
    expect((controls.name.element as HTMLInputElement).value).toBe('Platform')
    expect((controls.code.element as HTMLInputElement).value).toBe('PF')
    expect((controls.color.element as HTMLInputElement).value).toBe('#22c55e')
    expect((controls.description.element as HTMLTextAreaElement).value).toBe('Build platform work')
  })

  it('shows project code and a decorative color swatch in the project list', async () => {
    const wrapper = mount(ProjectListView, mountOptions)
    await flushPromises()

    expect(wrapper.text()).toContain('OPS')
    const swatch = wrapper.get('.project-color-swatch')
    expect((swatch.element as HTMLElement).style.backgroundColor).toBe('#0ea5e9')
    expect(swatch.attributes('aria-hidden')).toBe('true')
  })
})
