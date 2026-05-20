<script setup lang="ts">
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const form = reactive({
  account: '',
  password: '',
})

async function submit() {
  await auth.login(form)
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  await router.replace(redirect)
}
</script>

<template>
  <main class="login-screen">
    <section class="login-panel" aria-labelledby="login-title">
      <div>
        <p class="eyebrow">SD Kanban</p>
        <h1 id="login-title">登录工作台</h1>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <label>
          账号
          <input v-model="form.account" autocomplete="username" required />
        </label>
        <label>
          密码
          <input v-model="form.password" autocomplete="current-password" required type="password" />
        </label>
        <p v-if="auth.error" class="form-error">{{ auth.error }}</p>
        <button type="submit" :disabled="auth.loading">
          {{ auth.loading ? '登录中' : '登录' }}
        </button>
      </form>
    </section>
  </main>
</template>
