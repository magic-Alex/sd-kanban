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
    <section class="login-visual-panel" aria-label="SD Kanban 产品预览">
      <div class="login-brand-lockup">
        <span>SD</span>
        <strong>Kanban</strong>
      </div>
      <div class="login-hero-copy">
        <p class="eyebrow">敏捷交付工作台</p>
        <h1>项目、任务、成员和节奏都在同一张看板里。</h1>
        <p>面向项目负责人、开发、测试和管理员，聚焦任务创建、个人看板、WIP 限制、通知和团队账号管理。</p>
      </div>
      <div class="login-preview-board" aria-hidden="true">
        <div>
          <span>需求池</span>
          <i></i>
        </div>
        <div>
          <span>开发中</span>
          <i></i>
        </div>
        <div>
          <span>测试中</span>
          <i></i>
        </div>
      </div>
    </section>

    <section class="login-panel" aria-labelledby="login-title">
      <div>
        <p class="eyebrow">Secure sign in</p>
        <h1 id="login-title">登录工作台</h1>
        <p>使用管理员或团队账号进入 SD Kanban。</p>
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
        <p class="login-credential-hint">默认管理员：sd-robot / 1</p>
      </form>
    </section>
  </main>
</template>
