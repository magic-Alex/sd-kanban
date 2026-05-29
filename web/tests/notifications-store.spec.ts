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

function deferred<T>() {
  let resolve: (value: T) => void = () => undefined
  let reject: (error: Error) => void = () => undefined
  const promise = new Promise<T>((promiseResolve, promiseReject) => {
    resolve = promiseResolve
    reject = promiseReject
  })
  return { promise, resolve, reject }
}

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

  it('keeps the latest notification load result when requests resolve out of order', async () => {
    const unread = deferred<Awaited<ReturnType<typeof fetchNotifications>>>()
    const all = deferred<Awaited<ReturnType<typeof fetchNotifications>>>()
    vi.mocked(fetchNotifications)
      .mockReturnValueOnce(unread.promise)
      .mockReturnValueOnce(all.promise)
    const notifications = useNotificationsStore()

    const unreadPromise = notifications.load('unread')
    const allPromise = notifications.load('all')
    all.resolve([{ id: 2, actor: null, projectId: 7, taskId: 12, type: 'COMMENT', title: 'All notification', content: 'All result', read: true, createdAt: '2026-05-29T10:01:00', readAt: '2026-05-29T10:02:00' }])
    await allPromise
    unread.resolve([{ id: 1, actor: null, projectId: 7, taskId: 12, type: 'MENTION', title: 'Unread notification', content: 'Unread result', read: false, createdAt: '2026-05-29T10:00:00', readAt: null }])
    await unreadPromise

    expect(fetchNotifications).toHaveBeenNthCalledWith(1, 'unread')
    expect(fetchNotifications).toHaveBeenNthCalledWith(2, 'all')
    expect(notifications.items).toHaveLength(1)
    expect(notifications.items[0].title).toBe('All notification')
    expect(notifications.loading).toBe(false)
    expect(notifications.error).toBeNull()
  })

  it('records load errors without throwing to the UI caller', async () => {
    vi.mocked(fetchNotifications).mockRejectedValue(new Error('network failed'))
    const notifications = useNotificationsStore()

    await expect(notifications.load()).resolves.toBeUndefined()

    expect(notifications.error).toBe('通知加载失败')
    expect(notifications.loading).toBe(false)
  })

  it('records mark-read errors without throwing to the UI caller', async () => {
    vi.mocked(markNotificationRead).mockRejectedValue(new Error('network failed'))
    const notifications = useNotificationsStore()
    notifications.items = [
      { id: 1, actor: null, projectId: 7, taskId: 12, type: 'MENTION', title: '有人提到了你', content: '任务评论提到了你', read: false, createdAt: '2026-05-29T10:00:00', readAt: null },
    ]
    notifications.unreadCount = 1

    await expect(notifications.markRead(1)).resolves.toBeUndefined()

    expect(notifications.items[0].read).toBe(false)
    expect(notifications.unreadCount).toBe(1)
    expect(notifications.error).toBe('通知更新失败')
  })

  it('marks a notification read and refreshes unread count on success', async () => {
    vi.mocked(markNotificationRead).mockResolvedValue({ id: 1, actor: null, projectId: 7, taskId: 12, type: 'MENTION', title: '有人提到了你', content: '任务评论提到了你', read: true, createdAt: '2026-05-29T10:00:00', readAt: '2026-05-29T10:01:00' })
    vi.mocked(fetchUnreadNotificationCount).mockResolvedValue({ count: 1 })
    const notifications = useNotificationsStore()
    notifications.items = [
      { id: 1, actor: null, projectId: 7, taskId: 12, type: 'MENTION', title: '有人提到了你', content: '任务评论提到了你', read: false, createdAt: '2026-05-29T10:00:00', readAt: null },
      { id: 2, actor: null, projectId: 7, taskId: null, type: 'COMMENT', title: '新的评论', content: '任务有新的评论', read: false, createdAt: '2026-05-29T10:02:00', readAt: null },
    ]
    notifications.unreadCount = 2

    await notifications.markRead(1)

    expect(notifications.items[0].read).toBe(true)
    expect(notifications.items[0].readAt).toBe('2026-05-29T10:01:00')
    expect(notifications.items[1].read).toBe(false)
    expect(notifications.unreadCount).toBe(1)
  })

  it('marks all notifications read and clears unread count on success', async () => {
    vi.mocked(markAllNotificationsRead).mockResolvedValue()
    const notifications = useNotificationsStore()
    notifications.items = [
      { id: 1, actor: null, projectId: 7, taskId: 12, type: 'MENTION', title: '有人提到了你', content: '任务评论提到了你', read: false, createdAt: '2026-05-29T10:00:00', readAt: null },
      { id: 2, actor: null, projectId: 7, taskId: null, type: 'COMMENT', title: '新的评论', content: '任务有新的评论', read: false, createdAt: '2026-05-29T10:02:00', readAt: null },
    ]
    notifications.unreadCount = 2

    await notifications.markAllRead()

    expect(notifications.items.every((item) => item.read)).toBe(true)
    expect(notifications.items.every((item) => item.readAt)).toBe(true)
    expect(notifications.unreadCount).toBe(0)
  })

  it('ignores stale unread count responses after mark-all-read clears the count', async () => {
    const unread = deferred<Awaited<ReturnType<typeof fetchUnreadNotificationCount>>>()
    vi.mocked(fetchUnreadNotificationCount).mockReturnValue(unread.promise)
    vi.mocked(markAllNotificationsRead).mockResolvedValue()
    const notifications = useNotificationsStore()
    notifications.items = [
      { id: 1, actor: null, projectId: 7, taskId: 12, type: 'MENTION', title: '有人提到了你', content: '任务评论提到了你', read: false, createdAt: '2026-05-29T10:00:00', readAt: null },
    ]
    notifications.unreadCount = 1

    const unreadPromise = notifications.loadUnreadCount()
    await notifications.markAllRead()
    unread.resolve({ count: 5 })
    await unreadPromise

    expect(notifications.unreadCount).toBe(0)
  })
})
