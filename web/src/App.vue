<template>
  <RouterView v-if="$route.meta.public" />
  <div v-else class="workspace-shell">
    <aside class="sidebar">
      <RouterLink class="brand-mark" to="/">
        <span>SD</span>
        <strong>Kanban</strong>
      </RouterLink>
      <nav class="primary-nav" aria-label="主导航">
        <RouterLink to="/">仪表盘</RouterLink>
        <RouterLink to="/projects">项目</RouterLink>
        <RouterLink to="/my-tasks">我的任务</RouterLink>
      </nav>
      <div class="account-block">
        <span>{{ auth.user?.nickname ?? auth.user?.account }}</span>
        <button type="button" class="notification-button" aria-label="通知" @click="openNotifications">
          通知
          <span v-if="notifications.unreadCount > 0">{{ notifications.unreadCount }}</span>
        </button>
        <button type="button" @click="logout">退出</button>
      </div>
    </aside>
    <RouterView />
    <NotificationPanel
      :open="notificationPanelOpen"
      :items="notifications.items"
      :loading="notifications.loading"
      :error="notifications.error"
      :mark-read-pending-ids="notifications.markReadPendingIds"
      :mark-all-read-pending="notifications.markAllReadPending"
      @close="notificationPanelOpen = false"
      @mark-read="markNotificationRead"
      @mark-all-read="markAllNotificationsRead"
      @open-task="openNotificationTask"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NotificationPanel from './components/notification/NotificationPanel.vue'
import { useAuthStore } from './stores/auth'
import { useNotificationsStore } from './stores/notifications'

const router = useRouter()
const auth = useAuthStore()
const notifications = useNotificationsStore()
const notificationPanelOpen = ref(false)
const openNotificationsRequestId = ref(0)

function canUseNotifications() {
  return !router.currentRoute.value.meta.public && auth.isAuthenticated
}

onMounted(() => {
  if (canUseNotifications()) {
    void notifications.loadUnreadCount()
  }
})

async function openNotifications() {
  const requestId = openNotificationsRequestId.value + 1
  openNotificationsRequestId.value = requestId
  notificationPanelOpen.value = true
  await notifications.load('all')
  if (openNotificationsRequestId.value !== requestId || !canUseNotifications()) {
    return
  }
  await notifications.loadUnreadCount()
}

async function markNotificationRead(notificationId: number) {
  if (!canUseNotifications()) {
    return
  }
  await notifications.markRead(notificationId)
}

async function markAllNotificationsRead() {
  if (!canUseNotifications()) {
    return
  }
  await notifications.markAllRead()
}

async function openNotificationTask(taskId: number) {
  if (!canUseNotifications()) {
    notificationPanelOpen.value = false
    return
  }
  const item = notifications.items.find((notification) => notification.taskId === taskId)
  if (item && !item.read) {
    await notifications.markRead(item.id)
  }
  notificationPanelOpen.value = false
  if (item?.projectId) {
    await router.push(`/projects/${item.projectId}/board`)
    return
  }
  await router.push('/my-tasks')
}

async function logout() {
  openNotificationsRequestId.value += 1
  notificationPanelOpen.value = false
  auth.logout()
  await router.replace('/login')
}
</script>
