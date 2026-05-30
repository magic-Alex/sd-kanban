<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { useUsersStore } from '../stores/users'

const users = useUsersStore()
const form = reactive({
  account: '',
  nickname: '',
  email: '',
  password: '1',
  role: 'MEMBER',
})

onMounted(() => {
  void users.load()
})

async function submit() {
  await users.create({
    account: form.account.trim(),
    nickname: form.nickname.trim(),
    email: form.email.trim(),
    password: form.password,
    role: form.role,
  })
  form.account = ''
  form.nickname = ''
  form.email = ''
  form.password = '1'
  form.role = 'MEMBER'
}

async function toggleStatus(account: string, status: string) {
  await users.updateStatus(account, status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')
}

function roleText(role: string) {
  return role === 'ADMIN' ? '管理员' : '成员'
}

function statusText(status: string) {
  return status === 'ACTIVE' ? '启用中' : '已停用'
}
</script>

<template>
  <main class="page-surface user-admin-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Admin</p>
        <h1>用户管理</h1>
      </div>
    </header>

    <section class="split-layout user-admin-layout">
      <form class="panel-block user-form" aria-label="创建用户表单" @submit.prevent="submit">
        <h2>新建用户</h2>
        <label>
          账号
          <input v-model="form.account" aria-label="用户账号" required maxlength="64" />
        </label>
        <label>
          昵称
          <input v-model="form.nickname" aria-label="用户昵称" required maxlength="100" />
        </label>
        <label>
          邮箱
          <input v-model="form.email" aria-label="用户邮箱" type="email" maxlength="255" />
        </label>
        <label>
          初始密码
          <input v-model="form.password" aria-label="初始密码" required maxlength="100" />
        </label>
        <label>
          角色
          <select v-model="form.role" aria-label="用户角色">
            <option value="MEMBER">成员</option>
            <option value="ADMIN">管理员</option>
          </select>
        </label>
        <button class="primary-button" type="submit" :disabled="users.saving">
          {{ users.saving ? '创建中' : '创建用户' }}
        </button>
        <p v-if="users.error" class="form-error">{{ users.error }}</p>
      </form>

      <section class="panel-block user-list-panel" aria-label="用户列表">
        <div class="section-heading">
          <h2>用户列表</h2>
          <span>{{ users.users.length }} 人</span>
        </div>
        <p v-if="users.loading" class="muted">正在加载用户...</p>
        <div v-else class="user-table">
          <article v-for="user in users.users" :key="user.id" class="user-row">
            <div class="user-row-main">
              <strong>{{ user.nickname }}</strong>
              <span>{{ user.account }}</span>
            </div>
            <span class="status-pill">{{ roleText(user.role) }}</span>
            <span class="status-pill" :class="{ disabled: user.status !== 'ACTIVE' }">
              {{ statusText(user.status) }}
            </span>
            <span>{{ user.email || '未设置邮箱' }}</span>
            <button
              type="button"
              class="secondary-button"
              :aria-label="`${user.status === 'ACTIVE' ? '停用' : '启用'} ${user.account}`"
              @click="toggleStatus(user.account, user.status)"
            >
              {{ user.status === 'ACTIVE' ? '停用' : '启用' }}
            </button>
          </article>
        </div>
      </section>
    </section>
  </main>
</template>
