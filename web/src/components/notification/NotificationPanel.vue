<template>
  <aside v-if="open" class="notification-panel" aria-label="通知列表">
    <header class="notification-panel-header">
      <h2>通知</h2>
      <div class="notification-panel-actions">
        <button
          type="button"
          class="secondary-button"
          :disabled="loading || !items.some((item) => !item.read)"
          @click="$emit('markAllRead')"
        >
          全部已读
        </button>
        <button type="button" class="icon-button" aria-label="关闭通知" @click="$emit('close')">
          ×
        </button>
      </div>
    </header>

    <p v-if="loading" class="notification-state">通知加载中...</p>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <p v-else-if="items.length === 0" class="notification-state">暂无通知</p>

    <ul v-else class="notification-list">
      <li
        v-for="item in items"
        :key="item.id"
        class="notification-item"
        :class="{ unread: !item.read }"
      >
        <button
          v-if="item.taskId"
          type="button"
          class="notification-content-button"
          @click="$emit('openTask', item.taskId)"
        >
          <strong>{{ item.title }}</strong>
          <span>{{ item.content }}</span>
        </button>
        <div v-else class="notification-content">
          <strong>{{ item.title }}</strong>
          <span>{{ item.content }}</span>
        </div>
        <div class="notification-item-footer">
          <small>{{ item.read ? '已读' : '未读' }}</small>
          <button
            v-if="!item.read"
            type="button"
            class="link-button"
            @click="$emit('markRead', item.id)"
          >
            标记已读
          </button>
        </div>
      </li>
    </ul>
  </aside>
</template>

<script setup lang="ts">
import type { NotificationItem } from '../../api/notifications'

defineProps<{
  open: boolean
  items: NotificationItem[]
  loading?: boolean
  error?: string | null
}>()

defineEmits<{
  close: []
  markRead: [notificationId: number]
  markAllRead: []
  openTask: [taskId: number]
}>()
</script>
