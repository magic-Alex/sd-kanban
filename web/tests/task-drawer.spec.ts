import { afterEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import TaskDrawer from '../src/components/task/TaskDrawer.vue'

describe('TaskDrawer', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('renders acceptance criteria, comments, tags, and activities', () => {
    mount(TaskDrawer, {
      attachTo: document.body,
      props: {
        open: true,
        task: {
          id: 12,
          projectId: 7,
          sprintId: null,
          columnId: 1,
          assignee: null,
          creator: { id: 1, account: 'alex', nickname: 'Alex', email: 'alex@sd-robot.com', avatarUrl: null },
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
        },
        comments: [
          {
            id: 1,
            taskId: 12,
            author: { id: 1, account: 'alex', nickname: 'Alex', email: 'alex@sd-robot.com', avatarUrl: null },
            content: 'Please keep drag updates optimistic.',
            createdAt: '2026-05-21T10:10:00',
            updatedAt: '2026-05-21T10:10:00',
          },
        ],
        activities: [
          {
            id: 3,
            taskId: 12,
            actor: { id: 1, account: 'alex', nickname: 'Alex', email: 'alex@sd-robot.com', avatarUrl: null },
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
})
