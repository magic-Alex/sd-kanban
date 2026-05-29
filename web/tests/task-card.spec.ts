import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import TaskCard from '../src/components/board/TaskCard.vue'

function taskCard(overrides = {}) {
  return {
    id: 12,
    projectId: 7,
    sprintId: null,
    columnId: 1,
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
})
