import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import TaskCard from '../src/components/board/TaskCard.vue'

function taskCard(overrides = {}) {
  return {
    id: 12,
    projectId: 7,
    projectCode: null,
    projectName: null,
    projectColor: null,
    sprintId: null,
    columnId: 1,
    columnTemplateKey: 'READY',
    assigneeId: null,
    assignee: null,
    title: 'Build drawer',
    taskType: 'TASK',
    priority: 'HIGH',
    storyPoints: null,
    dueDate: null,
    sortOrder: 0,
    checklistDoneCount: 0,
    checklistTotalCount: 0,
    ...overrides,
  }
}

describe('TaskCard', () => {
  it('shows checklist progress when the task has checklist items', () => {
    const wrapper = mount(TaskCard, {
      props: {
        task: taskCard({
          checklistDoneCount: 1,
          checklistTotalCount: 2,
        }),
      },
    })

    expect(wrapper.text()).toContain('清单 1/2')
  })

  it('does not show checklist progress when the task has no checklist items', () => {
    const wrapper = mount(TaskCard, {
      props: {
        task: taskCard(),
      },
    })

    expect(wrapper.text()).not.toContain('清单')
  })

  it('renders a project badge while remaining clickable and draggable', async () => {
    const wrapper = mount(TaskCard, {
      props: {
        task: taskCard({
          projectCode: 'OPS',
          projectName: 'Operations',
          projectColor: '#f97316',
        }),
      },
    })

    const badge = wrapper.get('.task-project-badge')
    expect(badge.text()).toBe('OPS')
    expect(badge.attributes('title')).toContain('Operations')
    expect(badge.attributes('style')).toContain('--project-color: #f97316')
    expect(wrapper.get('article.task-card').attributes('style')).toBeUndefined()

    await wrapper.get('article.task-card').trigger('click')
    expect(wrapper.emitted('open')?.[0]).toEqual([12])

    const dataTransfer = {
      setData: vi.fn(),
    }
    await wrapper.get('article.task-card').trigger('dragstart', { dataTransfer })
    expect(dataTransfer.setData).toHaveBeenCalledWith('application/sd-kanban-task', '12')
  })
})
