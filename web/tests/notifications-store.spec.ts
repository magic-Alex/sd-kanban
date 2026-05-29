import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import {
  fetchNotifications,
  fetchUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
} from '../src/api/notifications'
import { useNotificationsStore } from '../src/stores/notifications'

vi.mock('../src/api/notifications', () => ({
  fetchNotifications: vi.fn(),
  fetchUnreadNotificationCount: vi.fn(),
  markNotificationRead: vi.fn(),
  markAllNotificationsRead: vi.fn(),
}))

describe('notifications store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(fetchNotifications).mockReset()
    vi.mocked(fetchUnreadNotificationCount).mockReset()
    vi.mocked(markNotificationRead).mockReset()
    vi.mocked(markAllNotificationsRead).mockReset()
  })

  it('loads notifications and unread count', async () => {
    vi.mocked(fetchNotifications).mockResolvedValue([{ id: 1, actor: null, projectId: 7, taskId: 12, type: 'MENTION', title: '有人提到了你', content: '任务评论提到了你', read: false, createdAt: '2026-05-29T10:00:00', readAt: null }])
    vi.mocked(fetchUnreadNotificationCount).mockResolvedValue({ count: 1 })

    const notifications = useNotificationsStore()
    await notifications.load()
    await notifications.loadUnreadCount()

    expect(notifications.items).toHaveLength(1)
    expect(notifications.unreadCount).toBe(1)
  })
})
