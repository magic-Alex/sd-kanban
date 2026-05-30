import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import BoardTemplateSettingsView from '../src/views/BoardTemplateSettingsView.vue'
import {
  createBoardTemplate,
  deleteBoardTemplate,
  fetchBoardTemplates,
  reorderBoardTemplates,
  updateBoardTemplate,
} from '../src/api/settings'

function deferred<T>() {
  let resolve: (value: T) => void = () => undefined
  let reject: (reason?: unknown) => void = () => undefined
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })
  return { promise, resolve, reject }
}

vi.mock('../src/api/settings', () => ({
  createBoardTemplate: vi.fn(),
  deleteBoardTemplate: vi.fn(),
  fetchBoardTemplates: vi.fn(),
  reorderBoardTemplates: vi.fn(),
  updateBoardTemplate: vi.fn(),
}))

const readyTemplate = {
  id: 1,
  templateKey: 'READY',
  nameZh: '准备',
  nameEn: 'Ready',
  displayName: '准备',
  color: '#0ea5e9',
  sortOrder: 0,
  wipLimit: 3,
  isDone: false,
}

const doneTemplate = {
  id: 2,
  templateKey: 'DONE',
  nameZh: '完成',
  nameEn: 'Done',
  displayName: '完成',
  color: '#22c55e',
  sortOrder: 1,
  wipLimit: null,
  isDone: true,
}

describe('BoardTemplateSettingsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(createBoardTemplate).mockReset()
    vi.mocked(deleteBoardTemplate).mockReset()
    vi.mocked(fetchBoardTemplates).mockReset()
    vi.mocked(reorderBoardTemplates).mockReset()
    vi.mocked(updateBoardTemplate).mockReset()
    vi.mocked(fetchBoardTemplates).mockResolvedValue([readyTemplate, doneTemplate])
    vi.mocked(createBoardTemplate).mockResolvedValue({
      id: 3,
      templateKey: 'REVIEW',
      nameZh: '评审',
      nameEn: 'Review',
      displayName: '评审',
      color: '#8b5cf6',
      sortOrder: 2,
      wipLimit: 2,
      isDone: false,
    })
    vi.mocked(updateBoardTemplate).mockResolvedValue({ ...readyTemplate, nameZh: '待处理', displayName: '待处理' })
    vi.mocked(deleteBoardTemplate).mockResolvedValue(undefined)
    vi.mocked(reorderBoardTemplates).mockResolvedValue([doneTemplate, readyTemplate])
    vi.spyOn(window, 'confirm').mockReturnValue(true)
  })

  it('loads templates and renders the list with the form', async () => {
    const wrapper = mount(BoardTemplateSettingsView)
    await flushPromises()

    expect(fetchBoardTemplates).toHaveBeenCalled()
    expect(wrapper.text()).toContain('准备')
    expect(wrapper.text()).toContain('READY')
    expect(wrapper.text()).toContain('WIP 3')
    expect(wrapper.text()).toContain('完成列')
    expect(wrapper.get('[data-testid="template-key-input"]').exists()).toBe(true)
  })

  it('creates a board template from the form', async () => {
    const wrapper = mount(BoardTemplateSettingsView)
    await flushPromises()

    await wrapper.get('[data-testid="template-key-input"]').setValue('REVIEW')
    await wrapper.get('[data-testid="name-zh-input"]').setValue('评审')
    await wrapper.get('[data-testid="name-en-input"]').setValue('Review')
    await wrapper.get('[data-testid="color-input"]').setValue('#8b5cf6')
    await wrapper.get('[data-testid="wip-limit-input"]').setValue('2')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createBoardTemplate).toHaveBeenCalledWith({
      templateKey: 'REVIEW',
      nameZh: '评审',
      nameEn: 'Review',
      color: '#8b5cf6',
      wipLimit: 2,
      isDone: false,
    })
    expect(wrapper.text()).toContain('REVIEW')
  })

  it('edits and deletes a board template', async () => {
    const wrapper = mount(BoardTemplateSettingsView)
    await flushPromises()

    await wrapper.get('[aria-label="编辑 READY"]').trigger('click')
    expect((wrapper.get('[data-testid="template-key-input"]').element as HTMLInputElement).disabled).toBe(true)
    await wrapper.get('[data-testid="name-zh-input"]').setValue('待处理')
    await wrapper.get('[data-testid="is-done-input"]').setValue(true)
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(updateBoardTemplate).toHaveBeenCalledWith('READY', {
      nameZh: '待处理',
      nameEn: 'Ready',
      color: '#0ea5e9',
      wipLimit: 3,
      isDone: true,
    })

    await wrapper.get('[aria-label="删除模板 DONE"]').trigger('click')
    await flushPromises()

    expect(deleteBoardTemplate).toHaveBeenCalledWith('DONE')
    expect(wrapper.text()).not.toContain('DONE')
  })

  it('does not delete a template when confirmation is cancelled', async () => {
    vi.mocked(window.confirm).mockReturnValue(false)
    const wrapper = mount(BoardTemplateSettingsView)
    await flushPromises()

    await wrapper.get('[aria-label="删除模板 DONE"]').trigger('click')
    await flushPromises()

    expect(window.confirm).toHaveBeenCalled()
    expect(deleteBoardTemplate).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('DONE')
  })

  it('disables conflicting actions while reorder is pending', async () => {
    const reorderRequest = deferred<typeof readyTemplate[]>()
    vi.mocked(reorderBoardTemplates).mockReturnValue(reorderRequest.promise)
    const wrapper = mount(BoardTemplateSettingsView)
    await flushPromises()

    await wrapper.get('[aria-label="下移 READY"]').trigger('click')
    await flushPromises()

    expect((wrapper.get('[aria-label="编辑 READY"]').element as HTMLButtonElement).disabled).toBe(true)
    expect((wrapper.get('[aria-label="删除模板 DONE"]').element as HTMLButtonElement).disabled).toBe(true)
    expect((wrapper.get('form button[type="submit"]').element as HTMLButtonElement).disabled).toBe(true)

    reorderRequest.resolve([doneTemplate, readyTemplate])
    await flushPromises()
  })
})
