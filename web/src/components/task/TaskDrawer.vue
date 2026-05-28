<script setup lang="ts">
import { ref } from 'vue'
import type { TaskActivity, TaskComment, TaskResponse } from '../../api/tasks'

const props = defineProps<{
  open: boolean
  task: TaskResponse | null
  comments: TaskComment[]
  activities: TaskActivity[]
  addComment: (content: string) => Promise<void> | void
}>()

const emit = defineEmits<{
  close: []
}>()

const comment = ref('')
const commentError = ref<string | null>(null)
const submittingComment = ref(false)

async function submitComment() {
  const content = comment.value.trim()
  if (!content || submittingComment.value) {
    return
  }
  submittingComment.value = true
  commentError.value = null
  try {
    await props.addComment(content)
    comment.value = ''
  } catch (error) {
    commentError.value = '评论保存失败，请重试'
  } finally {
    submittingComment.value = false
  }
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
            <h2>任务信息</h2>
            <dl class="task-detail-grid">
              <div>
                <dt>类型</dt>
                <dd>{{ task.taskType }}</dd>
              </div>
              <div>
                <dt>优先级</dt>
                <dd>{{ task.priority }}</dd>
              </div>
              <div>
                <dt>负责人</dt>
                <dd>{{ task.assignee?.nickname ?? '未分配' }}</dd>
              </div>
              <div>
                <dt>故事点</dt>
                <dd>{{ task.storyPoints ?? '-' }}</dd>
              </div>
              <div>
                <dt>预计工时</dt>
                <dd>{{ task.estimatedHours ?? '-' }}</dd>
              </div>
              <div>
                <dt>截止日期</dt>
                <dd>{{ task.dueDate ?? '-' }}</dd>
              </div>
            </dl>
          </section>

          <section class="drawer-section">
            <h2>描述</h2>
            <p>{{ task.description || '暂无描述' }}</p>
          </section>

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
              <p v-if="commentError" class="form-error" aria-live="polite">{{ commentError }}</p>
              <button class="primary-button" type="submit" :disabled="submittingComment || !comment.trim()">
                {{ submittingComment ? '添加中...' : '添加评论' }}
              </button>
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
