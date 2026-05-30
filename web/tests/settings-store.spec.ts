import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import {
  createBoardTemplate,
  deleteBoardTemplate,
  fetchBoardTemplates,
  reorderBoardTemplates,
  updateBoardTemplate,
} from '../src/api/settings'
import { useSettingsStore } from '../src/stores/settings'

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
  ...readyTemplate,
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

describe('settings store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(createBoardTemplate).mockReset()
    vi.mocked(deleteBoardTemplate).mockReset()
    vi.mocked(fetchBoardTemplates).mockReset()
    vi.mocked(reorderBoardTemplates).mockReset()
    vi.mocked(updateBoardTemplate).mockReset()
  })

  it('loads board templates', async () => {
    vi.mocked(fetchBoardTemplates).mockResolvedValue([readyTemplate, doneTemplate])
    const settings = useSettingsStore()

    await settings.loadBoardTemplates()

    expect(fetchBoardTemplates).toHaveBeenCalled()
    expect(settings.boardTemplates.map((template) => template.templateKey)).toEqual(['READY', 'DONE'])
    expect(settings.loading).toBe(false)
    expect(settings.error).toBeNull()
  })

  it('creates and updates board templates in local state', async () => {
    const settings = useSettingsStore()
    settings.error = 'stale error'
    settings.boardTemplates = [readyTemplate]
    vi.mocked(createBoardTemplate).mockResolvedValue(doneTemplate)
    vi.mocked(updateBoardTemplate).mockResolvedValue({ ...readyTemplate, nameEn: 'Queued' })

    await settings.saveBoardTemplate({
      templateKey: 'DONE',
      nameZh: '完成',
      nameEn: 'Done',
      color: '#22c55e',
      isDone: true,
    })
    await settings.saveBoardTemplate(readyTemplate.templateKey, {
      nameZh: '准备',
      nameEn: 'Queued',
      color: '#0ea5e9',
      wipLimit: 3,
      isDone: false,
    })

    expect(createBoardTemplate).toHaveBeenCalledWith({
      templateKey: 'DONE',
      nameZh: '完成',
      nameEn: 'Done',
      color: '#22c55e',
      isDone: true,
    })
    expect(updateBoardTemplate).toHaveBeenCalledWith(readyTemplate.templateKey, {
      nameZh: '准备',
      nameEn: 'Queued',
      color: '#0ea5e9',
      wipLimit: 3,
      isDone: false,
    })
    expect(settings.boardTemplates.map((template) => template.nameEn)).toEqual(['Queued', 'Done'])
    expect(settings.error).toBeNull()
  })

  it('does not update an existing template when saving from create mode', async () => {
    const settings = useSettingsStore()
    settings.boardTemplates = [readyTemplate]
    vi.mocked(createBoardTemplate).mockRejectedValue(new Error('duplicate key'))

    await expect(settings.saveBoardTemplate(null, {
      templateKey: readyTemplate.templateKey,
      nameZh: 'Duplicate',
      nameEn: 'Duplicate',
      color: '#8b5cf6',
      wipLimit: null,
      isDone: false,
    })).rejects.toThrow('duplicate key')

    expect(createBoardTemplate).toHaveBeenCalledWith({
      templateKey: readyTemplate.templateKey,
      nameZh: 'Duplicate',
      nameEn: 'Duplicate',
      color: '#8b5cf6',
      wipLimit: null,
      isDone: false,
    })
    expect(updateBoardTemplate).not.toHaveBeenCalled()
    expect(settings.boardTemplates).toEqual([readyTemplate])
  })

  it('rejects a create save without a template key before calling the API', async () => {
    const settings = useSettingsStore()
    settings.boardTemplates = [readyTemplate]

    await expect(settings.saveBoardTemplate({
      nameZh: 'Review',
      nameEn: 'Review',
      color: '#8b5cf6',
      isDone: false,
    })).rejects.toThrow('Template key is required')

    expect(createBoardTemplate).not.toHaveBeenCalled()
    expect(updateBoardTemplate).not.toHaveBeenCalled()
    expect(settings.error).toBe('Template key is required')
  })

  it('reorders and removes board templates', async () => {
    const settings = useSettingsStore()
    settings.error = 'stale error'
    settings.boardTemplates = [readyTemplate, doneTemplate]
    vi.mocked(reorderBoardTemplates).mockResolvedValue([
      { ...doneTemplate, sortOrder: 0 },
      { ...readyTemplate, sortOrder: 1 },
    ])
    vi.mocked(deleteBoardTemplate).mockResolvedValue(undefined)

    await settings.reorder([doneTemplate.templateKey, readyTemplate.templateKey])
    await settings.remove(doneTemplate.templateKey)

    expect(reorderBoardTemplates).toHaveBeenCalledWith([doneTemplate.templateKey, readyTemplate.templateKey])
    expect(deleteBoardTemplate).toHaveBeenCalledWith(doneTemplate.templateKey)
    expect(settings.boardTemplates.map((template) => template.id)).toEqual([readyTemplate.id])
    expect(settings.error).toBeNull()
  })

  it('sets mutation errors without corrupting template state', async () => {
    const settings = useSettingsStore()
    settings.error = 'stale error'
    settings.boardTemplates = [readyTemplate, doneTemplate]
    vi.mocked(reorderBoardTemplates).mockRejectedValue(new Error('reorder failed'))
    vi.mocked(deleteBoardTemplate).mockRejectedValue(new Error('delete failed'))

    await expect(settings.reorder([doneTemplate.templateKey, readyTemplate.templateKey])).rejects.toThrow('reorder failed')

    expect(settings.error).toBe('Board templates reorder failed')
    expect(settings.boardTemplates.map((template) => template.templateKey)).toEqual(['READY', 'DONE'])

    settings.error = 'stale error'
    await expect(settings.remove(doneTemplate.templateKey)).rejects.toThrow('delete failed')

    expect(settings.error).toBe('Board template delete failed')
    expect(settings.boardTemplates.map((template) => template.templateKey)).toEqual(['READY', 'DONE'])
  })
})
