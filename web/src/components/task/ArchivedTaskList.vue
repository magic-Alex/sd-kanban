<script setup lang="ts">
import { reactive } from 'vue'
import type { ProjectMember } from '../../api/projects'
import type { ArchivedTaskQuery, TaskResponse } from '../../api/tasks'

defineProps<{
  tasks: TaskResponse[]
  members: ProjectMember[]
  loading?: boolean
  error?: string | null
}>()

const emit = defineEmits<{
  openTask: [taskId: number]
  restoreTask: [taskId: number]
  applyFilters: [filters: ArchivedTaskQuery]
}>()

const filters = reactive<ArchivedTaskQuery>({
  keyword: '',
  assigneeId: '',
  priority: '',
  type: '',
})

function applyFilters() {
  emit('applyFilters', { ...filters })
}
</script>

<template>
  <section class="archived-tasks" aria-label="已归档任务">
    <header class="section-heading">
      <div>
        <p class="eyebrow">Archived</p>
        <h2>已归档任务</h2>
      </div>
      <button class="secondary-button" type="button" :disabled="loading" @click="applyFilters">
        刷新
      </button>
    </header>

    <form class="board-filters" aria-label="已归档任务筛选" @submit.prevent="applyFilters">
      <input v-model="filters.keyword" placeholder="搜索已归档任务" />
      <select v-model="filters.assigneeId" aria-label="已归档任务负责人筛选">
        <option value="">全部负责人</option>
        <option value="0">未分配</option>
        <option v-for="member in members" :key="member.user.id" :value="String(member.user.id)">
          {{ member.user.nickname }}
        </option>
      </select>
      <select v-model="filters.type" aria-label="已归档任务类型">
        <option value="">全部类型</option>
        <option value="TASK">任务</option>
        <option value="STORY">故事</option>
        <option value="BUG">缺陷</option>
      </select>
      <select v-model="filters.priority" aria-label="已归档任务优先级">
        <option value="">全部优先级</option>
        <option value="HIGH">高</option>
        <option value="MEDIUM">中</option>
        <option value="LOW">低</option>
        <option value="URGENT">紧急</option>
      </select>
      <button type="submit" :disabled="loading">筛选</button>
    </form>

    <p v-if="error" class="form-error" aria-live="polite">{{ error }}</p>
    <p v-else-if="loading" class="muted">正在加载已归档任务...</p>
    <p v-else-if="tasks.length === 0" class="muted">暂无已归档任务</p>

    <ul v-else class="archived-task-list">
      <li v-for="task in tasks" :key="task.id" class="archived-task-row">
        <button class="link-button archived-task-title" type="button" @click="emit('openTask', task.id)">
          {{ task.title }}
        </button>
        <span>{{ task.priority }}</span>
        <span>{{ task.assignee?.nickname ?? '未分配' }}</span>
        <button
          class="secondary-button"
          type="button"
          :disabled="loading"
          :aria-label="`恢复任务 ${task.title}`"
          @click="emit('restoreTask', task.id)"
        >
          恢复
        </button>
      </li>
    </ul>
  </section>
</template>
