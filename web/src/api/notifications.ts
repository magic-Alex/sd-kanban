import type { UserSummary } from './auth'
import { getData, http } from './http'

export interface NotificationItem {
  id: number
  actor: UserSummary | null
  projectId: number | null
  taskId: number | null
  type: string
  title: string
  content: string
  read: boolean
  createdAt: string
  readAt: string | null
}

export interface UnreadNotificationCount {
  count: number
}

export function fetchNotifications(status: 'all' | 'unread' = 'all'): Promise<NotificationItem[]> {
  return getData<NotificationItem[]>(`/notifications?status=${encodeURIComponent(status)}`)
}

export function fetchUnreadNotificationCount(): Promise<UnreadNotificationCount> {
  return getData<UnreadNotificationCount>('/notifications/unread-count')
}

export async function markNotificationRead(notificationId: number): Promise<NotificationItem> {
  const response = await http.patch(`/notifications/${notificationId}/read`)
  return response.data.data
}

export async function markAllNotificationsRead(): Promise<void> {
  await http.patch('/notifications/read-all')
}
