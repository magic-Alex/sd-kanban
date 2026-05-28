import { afterEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick, reactive } from 'vue'
import TaskDrawer from '../src/components/task/TaskDrawer.vue'

function userFixture(overrides = {}) {
  return {
    id: 1,
    account: 'alex',
    nickname: 'Alex',
    email: 'alex@sd-robot.com',
    avatarUrl: null,
    ...overrides,
  }
}

function taskFixture(overrides = {}) {
  return {
    id: 12,
    projectId: 7,
    sprintId: null,
    columnId: 1,
    assignee: null,
    creator: userFixture(),
    title: 'Build board',
    description: 'Create the board workflow',
    taskType: 'STORY',
    priority: 'HIGH',
    storyPoints: 5,
    estimatedHours: 8,
    dueDate: null,
    acceptanceCriteria: 'Cards can move between columns',
    sortOrder: 0,
    tags: [{ id: 2, projectId: 7, name: 'Frontend', color: '#0ea5e9' }],
    createdAt: '2026-05-21T10:00:00',
    updatedAt: '2026-05-21T10:00:00',
    ...overrides,
  }
}

function drawerProps(overrides = {}) {
  return {
    open: true,
    task: taskFixture(),
    comments: [],
    activities: [],
    members: [
      {
        user: userFixture({ id: 3, account: 'mei', nickname: 'Mei', email: 'mei@sd-robot.com' }),
        role: 'member',
        joinedAt: '2026-05-21T09:00:00',
      },
    ],
    columns: [
      { id: 1, name: 'Backlog', color: '#64748b', sortOrder: 0, isDone: false, tasks: [] },
      { id: 2, name: 'Done', color: '#16a34a', sortOrder: 1, isDone: true, tasks: [] },
    ],
    addComment: async () => undefined,
    saveTask: vi.fn(),
    completeTask: vi.fn(),
    archiveTask: vi.fn(),
    deleteTask: vi.fn(),
    actionLoading: false,
    actionError: null,
    ...overrides,
  }
}

function getByLabel(label: string) {
  const element = document.body.querySelector(`[aria-label="${label}"]`)
  if (!element) {
    throw new Error(`Unable to find aria-label="${label}"`)
  }
  return element
}

describe('TaskDrawer', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    document.body.innerHTML = ''
  })

  it('renders acceptance criteria, comments, tags, and activities', () => {
    mount(TaskDrawer, {
      attachTo: document.body,
      props: {
        ...drawerProps(),
        comments: [
          {
            id: 1,
            taskId: 12,
            author: userFixture(),
            content: 'Please keep drag updates optimistic.',
            createdAt: '2026-05-21T10:10:00',
            updatedAt: '2026-05-21T10:10:00',
          },
        ],
        activities: [
          {
            id: 3,
            taskId: 12,
            actor: userFixture(),
            actionType: 'TASK_UPDATED',
            fieldName: 'priority',
            oldValue: 'MEDIUM',
            newValue: 'HIGH',
            createdAt: '2026-05-21T10:12:00',
          },
        ],
      },
    })

    expect(document.body.textContent).toContain('Build board')
    expect(document.body.textContent).toContain('Cards can move between columns')
    expect(document.body.textContent).toContain('Frontend')
    expect(document.body.textContent).toContain('Please keep drag updates optimistic.')
    expect(document.body.textContent).toContain('priority')
    expect(document.body.textContent).toContain('HIGH')
  })

  it('keeps the draft and shows an error when comment save fails', async () => {
    mount(TaskDrawer, {
      attachTo: document.body,
      props: {
        ...drawerProps({
          task: taskFixture({
            description: null,
            storyPoints: null,
            estimatedHours: null,
            dueDate: null,
            acceptanceCriteria: null,
            tags: [],
          }),
        }),
        addComment: async () => {
          throw new Error('network unavailable')
        },
      },
    })

    const textarea = document.body.querySelector('textarea') as HTMLTextAreaElement
    const form = document.body.querySelector('form') as HTMLFormElement

    textarea.value = 'Keep this comment'
    textarea.dispatchEvent(new Event('input'))
    form.dispatchEvent(new Event('submit'))
    await flushPromises()

    expect(textarea.value).toBe('Keep this comment')
    expect(document.body.textContent).toContain('评论保存失败，请重试')
  })

  it('saves edited task fields and clears nullable empty fields', async () => {
    const saveTask = vi.fn()
    mount(TaskDrawer, {
      attachTo: document.body,
      props: drawerProps({
        task: taskFixture({
          assignee: userFixture({ id: 3, account: 'mei', nickname: 'Mei', email: 'mei@sd-robot.com' }),
          dueDate: '2026-06-01',
        }),
        saveTask,
      }),
    })

    await (getByLabel('编辑任务') as HTMLButtonElement).click()
    await flushPromises()

    const title = getByLabel('编辑任务标题') as HTMLInputElement
    title.value = 'Build board V2'
    title.dispatchEvent(new Event('input'))

    const description = getByLabel('编辑任务描述') as HTMLTextAreaElement
    description.value = ''
    description.dispatchEvent(new Event('input'))

    const assignee = getByLabel('编辑任务负责人') as HTMLSelectElement
    assignee.value = ''
    assignee.dispatchEvent(new Event('change'))

    ;(getByLabel('保存任务') as HTMLButtonElement).click()
    await flushPromises()

    expect(saveTask).toHaveBeenCalledWith(expect.objectContaining({
      title: 'Build board V2',
      assigneeId: null,
      clearFields: expect.arrayContaining(['description', 'assigneeId']),
    }))
  })

  it('confirms before deleting the task', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    const deleteTask = vi.fn()
    mount(TaskDrawer, {
      attachTo: document.body,
      props: drawerProps({ deleteTask }),
    })

    ;(getByLabel('删除任务') as HTMLButtonElement).click()
    await flushPromises()

    expect(deleteTask).toHaveBeenCalled()
  })

  it('does not delete the task when confirmation is cancelled', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    const deleteTask = vi.fn()
    mount(TaskDrawer, {
      attachTo: document.body,
      props: drawerProps({ deleteTask }),
    })

    ;(getByLabel('删除任务') as HTMLButtonElement).click()
    await flushPromises()

    expect(deleteTask).not.toHaveBeenCalled()
  })

  it('resets edit state and draft when the same task refreshes with a new update time', async () => {
    const task = reactive(taskFixture({
      id: 12,
      title: 'Original title',
      updatedAt: '2026-05-21T10:00:00',
    }))
    mount(TaskDrawer, {
      attachTo: document.body,
      props: drawerProps({
        task,
      }),
    })

    await (getByLabel('编辑任务') as HTMLButtonElement).click()
    await flushPromises()
    const title = getByLabel('编辑任务标题') as HTMLInputElement
    title.value = 'Local unsaved title'
    title.dispatchEvent(new Event('input'))

    task.title = 'Server refreshed title'
    task.updatedAt = '2026-05-21T10:05:00'
    await nextTick()

    expect(document.body.querySelector('[aria-label="编辑任务标题"]')).toBeNull()

    await (getByLabel('编辑任务') as HTMLButtonElement).click()
    await flushPromises()

    expect((getByLabel('编辑任务标题') as HTMLInputElement).value).toBe('Server refreshed title')
  })
})
