<script setup lang="ts">
import { ref } from 'vue'
import type { TaskActivity, TaskComment, TaskResponse } from '../../api/tasks'

defineProps<{
  open: boolean
  task: TaskResponse | null
  comments: TaskComment[]
  activities: TaskActivity[]
}>()

const emit = defineEmits<{
  close: []
  addComment: [content: string]
}>()

const comment = ref('')

function submitComment() {
  const content = comment.value.trim()
  if (!content) {
    return
  }
  emit('addComment', content)
  comment.value = ''
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="drawer-backdrop">
      <aside class="task-drawer" aria-label="任务详情">
        <header class="drawer-header">
          <div>
            <p class="eyebrow">Task</p>
            <h1>{{ task?.title ?? '任务详情' }}</h1>
          </div>
          <button class="secondary-button" type="button" @click="emit('close')">关闭</button>
        </header>

        <template v-if="task">
          <section class="drawer-section">
            <h2>验收标准</h2>
            <p>{{ task.acceptanceCriteria || '暂无验收标准' }}</p>
          </section>

          <section class="drawer-section">
            <h2>标签</h2>
            <div class="tag-list">
              <span v-for="tag in task.tags" :key="tag.id" :style="{ borderColor: tag.color }">
                {{ tag.name }}
              </span>
            </div>
          </section>

          <section class="drawer-section">
            <h2>评论</h2>
            <form class="comment-form" @submit.prevent="submitComment">
              <label>
                新增评论
                <textarea v-model="comment" rows="3" />
              </label>
              <button class="primary-button" type="submit">添加评论</button>
            </form>
            <ul class="drawer-list">
              <li v-for="comment in comments" :key="comment.id">
                <strong>{{ comment.author.nickname }}</strong>
                <p>{{ comment.content }}</p>
              </li>
            </ul>
          </section>

          <section class="drawer-section">
            <h2>动态</h2>
            <ul class="drawer-list">
              <li v-for="activity in activities" :key="activity.id">
                <strong>{{ activity.actor?.nickname ?? '系统' }}</strong>
                <p>{{ activity.actionType }} {{ activity.fieldName }} {{ activity.oldValue }} → {{ activity.newValue }}</p>
              </li>
            </ul>
          </section>
        </template>
      </aside>
    </div>
  </Teleport>
</template>
