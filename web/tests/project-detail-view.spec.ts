import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ProjectDetailView from '../src/views/ProjectDetailView.vue'
import {
  addProjectMember,
  fetchProject,
  fetchProjectMembers,
  removeProjectMember,
  type ProjectMember,
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

const tester = {
  id: 3,
  account: 'tester',
  nickname: 'Tester',
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

const members: ProjectMember[] = [
  { user: owner, role: 'OWNER', joinedAt: '2026-05-30T10:00:00' },
  { user: member, role: 'MEMBER', joinedAt: '2026-05-30T11:00:00' },
]

const testerMember: ProjectMember = {
  user: tester,
  role: 'MEMBER',
  joinedAt: '2026-05-30T12:00:00',
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

function deferred<T>() {
  let resolve: (value: T) => void = () => undefined
  let reject: (reason?: unknown) => void = () => undefined
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })

  return { promise, resolve, reject }
}

async function mountDetailView() {
  const wrapper = mount(ProjectDetailView, mountOptions)
  await flushPromises()
  return wrapper
}

async function searchForTester(wrapper: Awaited<ReturnType<typeof mountDetailView>>) {
  await wrapper.get('.member-search-form input').setValue('tester')
  await wrapper.get('form.member-search-form').trigger('submit')
  await flushPromises()
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
    vi.mocked(addProjectMember).mockResolvedValue(testerMember)
    vi.mocked(removeProjectMember).mockResolvedValue(undefined)
    vi.mocked(searchActiveUsers).mockResolvedValue([
      { id: tester.id, account: tester.account, nickname: tester.nickname, avatarUrl: null },
    ])
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('loads project metadata and renders member management', async () => {
    const wrapper = await mountDetailView()

    expect(fetchProject).toHaveBeenCalledWith('7')
    expect(fetchProjectMembers).toHaveBeenCalledWith('7')
    expect(wrapper.text()).toContain('OPS')
    expect(wrapper.findAll('.panel-block')[0].text()).toContain('Daily work board')
    expect(wrapper.text()).toContain('负责人')
    expect(wrapper.text()).toContain('成员')
    expect(wrapper.text()).toContain('Developer')
    const swatch = wrapper.get('.project-metadata .project-color-swatch')
    expect((swatch.element as HTMLElement).style.backgroundColor).toBe('#0ea5e9')
    expect(swatch.attributes('aria-hidden')).toBe('true')
    expect(wrapper.get('a.primary-link').attributes('href')).toBe('/projects/7/board')
  })

  it('hides member management controls from non-owner users', async () => {
    localStorage.setItem('sd-kanban-user', JSON.stringify(member))
    const wrapper = await mountDetailView()

    expect(wrapper.text()).toContain('Developer')
    expect(wrapper.find('form.member-search-form').exists()).toBe(false)
    expect(wrapper.find('.candidate-button').exists()).toBe(false)
    expect(wrapper.find('.member-row .danger-button').exists()).toBe(false)
  })

  it('searches active users and wires add and remove member actions', async () => {
    const wrapper = await mountDetailView()

    await searchForTester(wrapper)

    expect(searchActiveUsers).toHaveBeenCalledWith('tester')
    await wrapper.get('.candidate-button').trigger('click')
    await flushPromises()

    expect(addProjectMember).toHaveBeenCalledWith('7', 3)
    expect(fetchProjectMembers).toHaveBeenCalledTimes(2)
    expect(wrapper.get('.large-number').text()).toBe('3')

    await wrapper.get('.member-row .danger-button').trigger('click')
    await flushPromises()

    expect(removeProjectMember).toHaveBeenCalledWith('7', 2)
    expect(fetchProjectMembers).toHaveBeenCalledTimes(3)
    expect(wrapper.get('.large-number').text()).toBe('2')
    expect(wrapper.find('[aria-label="移除 Owner"]').exists()).toBe(false)
  })

  it('disables member action buttons and guards duplicate adds while pending', async () => {
    const addRequest = deferred<ProjectMember>()
    vi.mocked(addProjectMember).mockReturnValue(addRequest.promise)
    const wrapper = await mountDetailView()
    await searchForTester(wrapper)

    const addButton = wrapper.get('.candidate-button')
    await addButton.trigger('click')
    await addButton.trigger('click')

    expect(addProjectMember).toHaveBeenCalledTimes(1)
    expect((addButton.element as HTMLButtonElement).disabled).toBe(true)
    expect((wrapper.get('.member-row .danger-button').element as HTMLButtonElement).disabled).toBe(true)

    addRequest.resolve(testerMember)
    await flushPromises()
  })

  it('does not search the user directory for a blank keyword', async () => {
    const wrapper = await mountDetailView()

    await wrapper.get('form.member-search-form').trigger('submit')
    await flushPromises()

    expect(searchActiveUsers).not.toHaveBeenCalled()
  })
})
