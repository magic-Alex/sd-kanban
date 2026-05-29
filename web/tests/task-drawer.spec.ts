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

function checklistItemFixture(overrides = {}) {
  return {
    id: 1,
    taskId: 12,
    projectId: 7,
    title: 'Write tests',
    done: true,
    sortOrder: 0,
    createdBy: userFixture(),
    completedBy: userFixture(),
    completedAt: '2026-05-21T10:20:00',
    createdAt: '2026-05-21T10:00:00',
    updatedAt: '2026-05-21T10:20:00',
    ...overrides,
  }
}

function drawerProps(overrides = {}) {
  return {
    open: true,
    task: taskFixture(),
    comments: [],
    activities: [],
    checklistItems: [],
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
    addChecklistItem: vi.fn(),
    toggleChecklistItem: vi.fn(),
    renameChecklistItem: vi.fn(),
    deleteChecklistItem: vi.fn(),
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

function controllableRejectingPromise() {
  let rejectPromise: (error: Error) => void = () => undefined
  const promise = new Promise<void>((_resolve, reject) => {
    rejectPromise = reject
  })

  return { promise, rejectPromise }
}

function drawerActionButton(index: number) {
  return document.body.querySelectorAll('.drawer-actions button').item(index) as HTMLButtonElement
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
            displayText: 'Alex 将优先级从 MEDIUM 改为 HIGH',
            createdAt: '2026-05-21T10:12:00',
          },
        ],
      },
    })

    expect(document.body.textContent).toContain('Build board')
    expect(document.body.textContent).toContain('Cards can move between columns')
    expect(document.body.textContent).toContain('Frontend')
    expect(document.body.textContent).toContain('Please keep drag updates optimistic.')
    expect(document.body.textContent).toContain('Alex 将优先级从 MEDIUM 改为 HIGH')
    expect(document.body.textContent).toContain('HIGH')
  })

  it('renders checklist progress and toggles checklist items', async () => {
    const toggleChecklistItem = vi.fn()
    mount(TaskDrawer, {
      attachTo: document.body,
      props: drawerProps({
        checklistItems: [
          checklistItemFixture({ id: 1, title: 'Write tests', done: true, sortOrder: 0 }),
          checklistItemFixture({ id: 2, title: 'Build UI', done: false, sortOrder: 1 }),
        ],
        toggleChecklistItem,
      }),
    })

    expect(document.body.textContent).toContain('检查清单 1/2')
    expect(document.body.textContent).toContain('Write tests')
    expect(document.body.textContent).toContain('Build UI')

    ;(getByLabel('切换检查项 Build UI') as HTMLInputElement).click()
    await flushPromises()

    expect(toggleChecklistItem).toHaveBeenCalledWith(2)
  })

  it('renders activity display text without leaking raw action types', () => {
    mount(TaskDrawer, {
      attachTo: document.body,
      props: drawerProps({
        activities: [
          {
            id: 3,
            taskId: 12,
            actor: userFixture(),
            actionType: 'TASK_CREATED',
            fieldName: null,
            oldValue: null,
            newValue: null,
            displayText: 'Alex 创建了任务',
            createdAt: '2026-05-21T10:12:00',
          },
        ],
      }),
    })

    expect(document.body.textContent).toContain('Alex 创建了任务')
    expect(document.body.textContent).not.toContain('TASK_CREATED')
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

    const textarea = document.body.querySelector('.comment-form textarea') as HTMLTextAreaElement
    const form = document.body.querySelector('.comment-form') as HTMLFormElement

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

  it('saves numeric edits entered through number inputs', async () => {
    const saveTask = vi.fn()
    mount(TaskDrawer, {
      attachTo: document.body,
      props: drawerProps({ saveTask }),
    })

    await drawerActionButton(0).click()
    await flushPromises()

    const numberInputs = document.body.querySelectorAll<HTMLInputElement>('.task-edit-form input[type="number"]')
    numberInputs.item(0).value = '2'
    numberInputs.item(0).dispatchEvent(new Event('input'))
    numberInputs.item(1).value = '6'
    numberInputs.item(1).dispatchEvent(new Event('input'))

    ;(document.body.querySelector('.task-edit-form button[type="submit"]') as HTMLButtonElement).click()
    await flushPromises()

    expect(saveTask).toHaveBeenCalledWith(expect.objectContaining({
      storyPoints: 2,
      estimatedHours: 6,
    }))
  })

  it('keeps a newer task edit form open when an older save resolves', async () => {
    let resolveSave: () => void = () => undefined
    const saveTask = vi.fn(() => new Promise<void>((resolve) => {
      resolveSave = resolve
    }))
    const wrapper = mount(TaskDrawer, {
      attachTo: document.body,
      props: drawerProps({
        task: taskFixture({ id: 12, title: 'Task A' }),
        saveTask,
      }),
    })

    await (document.body.querySelector('.drawer-actions button') as HTMLButtonElement).click()
    await flushPromises()
    ;(document.body.querySelector('.task-edit-form button[type="submit"]') as HTMLButtonElement).click()
    await flushPromises()

    await wrapper.setProps({
      task: taskFixture({ id: 34, title: 'Task B' }),
    })
    await (document.body.querySelector('.drawer-actions button') as HTMLButtonElement).click()
    await flushPromises()

    resolveSave()
    await flushPromises()

    expect(document.body.querySelector('.task-edit-form')).toBeTruthy()
    expect((document.body.querySelector('.task-edit-form input') as HTMLInputElement).value).toBe('Task B')
  })

  it('does not show a stale save error after switching tasks', async () => {
    const { promise, rejectPromise } = controllableRejectingPromise()
    const saveTask = vi.fn(() => promise)
    const wrapper = mount(TaskDrawer, {
      attachTo: document.body,
      props: drawerProps({
        task: taskFixture({ id: 12, title: 'Task A' }),
        saveTask,
      }),
    })

    await drawerActionButton(0).click()
    await flushPromises()
    const title = document.body.querySelector('.task-edit-form input') as HTMLInputElement
    title.value = 'Task A edited'
    title.dispatchEvent(new Event('input'))
    ;(document.body.querySelector('.task-edit-form button[type="submit"]') as HTMLButtonElement).click()
    await flushPromises()
    expect(saveTask).toHaveBeenCalledTimes(1)

    await wrapper.setProps({
      task: taskFixture({ id: 34, title: 'Task B' }),
    })
    await drawerActionButton(0).click()
    await flushPromises()

    rejectPromise(new Error('stale save failed'))
    await flushPromises()

    expect(document.body.textContent).toContain('Task B')
    expect(document.body.querySelector('.task-edit-form')).toBeTruthy()
    expect((document.body.querySelector('.task-edit-form input') as HTMLInputElement).value).toBe('Task B')
    expect(document.body.querySelector('.form-error')).toBeNull()
    expect(document.body.textContent).not.toContain('任务保存失败，请重试')
  })

  it.each([
    {
      actionName: 'complete',
      buttonIndex: 1,
      propName: 'completeTask',
      errorText: '任务完成失败，请重试',
    },
    {
      actionName: 'archive',
      buttonIndex: 2,
      propName: 'archiveTask',
      errorText: '任务归档失败，请重试',
    },
    {
      actionName: 'delete',
      buttonIndex: 3,
      propName: 'deleteTask',
      errorText: '任务删除失败，请重试',
      confirmDelete: true,
    },
  ])('does not show a stale $actionName error after switching tasks', async ({
    buttonIndex,
    propName,
    errorText,
    confirmDelete,
  }) => {
    if (confirmDelete) {
      vi.spyOn(window, 'confirm').mockReturnValue(true)
    }
    const { promise, rejectPromise } = controllableRejectingPromise()
    const action = vi.fn(() => promise)
    const wrapper = mount(TaskDrawer, {
      attachTo: document.body,
      props: drawerProps({
        task: taskFixture({ id: 12, title: 'Task A' }),
        [propName]: action,
      }),
    })

    drawerActionButton(buttonIndex).click()
    await flushPromises()
    expect(action).toHaveBeenCalledTimes(1)

    await wrapper.setProps({
      task: taskFixture({ id: 34, title: 'Task B' }),
    })

    rejectPromise(new Error('stale action failed'))
    await flushPromises()

    expect(document.body.textContent).toContain('Task B')
    expect(document.body.querySelector('.form-error')).toBeNull()
    expect(document.body.textContent).not.toContain(errorText)
  })

  it('labels the edit form for assistive technology', async () => {
    mount(TaskDrawer, {
      attachTo: document.body,
      props: drawerProps(),
    })

    ;(document.body.querySelector('.drawer-actions button') as HTMLButtonElement).click()
    await flushPromises()

    expect(document.body.querySelector('form')?.getAttribute('aria-label')).toBe('\u7f16\u8f91\u4efb\u52a1\u8868\u5355')
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
