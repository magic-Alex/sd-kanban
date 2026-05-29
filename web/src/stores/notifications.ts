import { defineStore } from 'pinia'
import {
  fetchNotifications,
  fetchUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationItem,
} from '../api/notifications'

export const useNotificationsStore = defineStore('notifications', {
  state: () => ({
    items: [] as NotificationItem[],
    unreadCount: 0,
    loading: false,
    error: null as string | null,
  }),
  actions: {
    async load(status: 'all' | 'unread' = 'all') {
      this.loading = true
      this.error = null
      try {
        this.items = await fetchNotifications(status)
      } catch (error) {
        this.error = '通知加载失败'
        throw error
      } finally {
        this.loading = false
      }
    },
    async loadUnreadCount() {
      const result = await fetchUnreadNotificationCount()
      this.unreadCount = result.count
    },
    async markRead(notificationId: number) {
      const updated = await markNotificationRead(notificationId)
      this.items = this.items.map((item) => item.id === notificationId ? updated : item)
      await this.loadUnreadCount()
    },
    async markAllRead() {
      await markAllNotificationsRead()
      this.items = this.items.map((item) => ({
        ...item,
        read: true,
        readAt: item.readAt ?? new Date().toISOString(),
      }))
      this.unreadCount = 0
    },
  },
})
