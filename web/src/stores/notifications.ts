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
    loadRequestId: 0,
    unreadCountRequestId: 0,
    unreadCountVersion: 0,
    markReadPendingIds: [] as number[],
    markAllReadPending: false,
  }),
  actions: {
    async load(status: 'all' | 'unread' = 'all') {
      const requestId = this.loadRequestId + 1
      this.loadRequestId = requestId
      this.loading = true
      this.error = null
      try {
        const items = await fetchNotifications(status)
        if (this.loadRequestId === requestId) {
          this.items = items
        }
      } catch (error) {
        if (this.loadRequestId === requestId) {
          this.error = '通知加载失败'
        }
      } finally {
        if (this.loadRequestId === requestId) {
          this.loading = false
        }
      }
    },
    async loadUnreadCount() {
      const requestId = this.unreadCountRequestId + 1
      const version = this.unreadCountVersion
      this.unreadCountRequestId = requestId
      try {
        const result = await fetchUnreadNotificationCount()
        if (this.unreadCountRequestId === requestId && this.unreadCountVersion === version) {
          this.unreadCount = result.count
        }
      } catch (error) {
        if (this.unreadCountRequestId === requestId && this.unreadCountVersion === version) {
          this.error = '通知加载失败'
        }
      }
    },
    async markRead(notificationId: number) {
      if (this.markReadPendingIds.includes(notificationId)) {
        return
      }
      this.error = null
      this.markReadPendingIds.push(notificationId)
      try {
        const updated = await markNotificationRead(notificationId)
        this.items = this.items.map((item) => item.id === notificationId ? updated : item)
        await this.loadUnreadCount()
      } catch (error) {
        this.error = '通知更新失败'
      } finally {
        this.markReadPendingIds = this.markReadPendingIds.filter((id) => id !== notificationId)
      }
    },
    async markAllRead() {
      if (this.markAllReadPending) {
        return
      }
      this.error = null
      this.markAllReadPending = true
      try {
        await markAllNotificationsRead()
        this.unreadCountVersion += 1
        this.unreadCountRequestId += 1
        this.items = this.items.map((item) => ({
          ...item,
          read: true,
          readAt: item.readAt ?? new Date().toISOString(),
        }))
        this.unreadCount = 0
      } catch (error) {
        this.error = '通知更新失败'
      } finally {
        this.markAllReadPending = false
      }
    },
  },
})
