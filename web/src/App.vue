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
        <button type="button" @click="logout">退出</button>
      </div>
    </aside>
    <RouterView />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const router = useRouter()
const auth = useAuthStore()

async function logout() {
  auth.logout()
  await router.replace('/login')
}
</script>
