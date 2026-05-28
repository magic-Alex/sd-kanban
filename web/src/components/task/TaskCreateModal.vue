<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { BoardColumn } from '../../api/board'
import type { ProjectMember } from '../../api/projects'
import type { CreateTaskRequest } from '../../api/tasks'

const props = defineProps<{
  open: boolean
  columns: BoardColumn[]
  members: ProjectMember[]
  defaultColumnId?: number | null
  submitting?: boolean
  error?: string | null
}>()

const emit = defineEmits<{
  close: []
  submit: [request: CreateTaskRequest]
}>()

const form = reactive({
  title: '',
  description: '',
  taskType: 'TASK',
  priority: 'MEDIUM',
  columnId: '',
  assigneeId: '',
  storyPoints: '',
  estimatedHours: '',
  dueDate: '',
  acceptanceCriteria: '',
})

function initialColumnId() {
  return String(props.defaultColumnId ?? props.columns[0]?.id ?? '')
}

function resetForm() {
  form.title = ''
  form.description = ''
  form.taskType = 'TASK'
  form.priority = 'MEDIUM'
  form.columnId = initialColumnId()
  form.assigneeId = ''
  form.storyPoints = ''
  form.estimatedHours = ''
  form.dueDate = ''
  form.acceptanceCriteria = ''
}

function nullableText(value: string) {
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

function nullableNumber(value: string) {
  return value === '' ? null : Number(value)
}

function submit() {
  const title = form.title.trim()
  const columnId = Number(form.columnId)
  if (!title || !Number.isFinite(columnId)) {
    return
  }

  emit('submit', {
    title,
    description: nullableText(form.description),
    taskType: form.taskType,
    priority: form.priority,
    columnId,
    assigneeId: form.assigneeId ? Number(form.assigneeId) : null,
    storyPoints: nullableNumber(form.storyPoints),
    estimatedHours: nullableNumber(form.estimatedHours),
    dueDate: form.dueDate || null,
    acceptanceCriteria: nullableText(form.acceptanceCriteria),
    tagIds: [],
  })
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      resetForm()
    }
  },
)

watch(
  () => [props.defaultColumnId, props.columns.length],
  () => {
    if (props.open && !form.columnId) {
      form.columnId = initialColumnId()
    }
  },
)
</script>

<template>
  <div v-if="open" class="modal-backdrop">
    <section class="task-modal" role="dialog" aria-modal="true" aria-labelledby="task-create-title">
      <header class="modal-header">
        <div>
          <p class="eyebrow">Task</p>
          <h1 id="task-create-title">创建看板任务</h1>
        </div>
        <button class="secondary-button" type="button" @click="emit('close')">关闭</button>
      </header>

      <form class="task-create-form" aria-label="创建任务表单" @submit.prevent="submit">
        <label class="full-field">
          任务标题
          <input v-model="form.title" aria-label="任务标题" maxlength="200" placeholder="输入任务标题" />
        </label>

        <label class="full-field">
          描述
          <textarea v-model="form.description" aria-label="任务描述" rows="3" placeholder="补充背景、范围或上下文" />
        </label>

        <label>
          任务类型
          <select v-model="form.taskType" aria-label="任务类型">
            <option value="TASK">任务</option>
            <option value="STORY">故事</option>
            <option value="BUG">缺陷</option>
          </select>
        </label>

        <label>
          优先级
          <select v-model="form.priority" aria-label="任务优先级">
            <option value="HIGH">高</option>
            <option value="MEDIUM">中</option>
            <option value="LOW">低</option>
          </select>
        </label>

        <label>
          所属列
          <select v-model="form.columnId" aria-label="所属列">
            <option v-for="column in columns" :key="column.id" :value="String(column.id)">
              {{ column.name }}
            </option>
          </select>
        </label>

        <label>
          负责人
          <select v-model="form.assigneeId" aria-label="任务负责人">
            <option value="">暂不分配</option>
            <option v-for="member in members" :key="member.user.id" :value="String(member.user.id)">
              {{ member.user.nickname }}（{{ member.role }}）
            </option>
          </select>
        </label>

        <label>
          故事点
          <input v-model="form.storyPoints" aria-label="故事点" type="number" min="0" step="0.5" />
        </label>

        <label>
          预计工时
          <input v-model="form.estimatedHours" aria-label="预计工时" type="number" min="0" step="0.5" />
        </label>

        <label>
          截止日期
          <input v-model="form.dueDate" aria-label="截止日期" type="date" />
        </label>

        <label class="full-field">
          验收标准
          <textarea v-model="form.acceptanceCriteria" aria-label="验收标准" rows="3" placeholder="什么条件下算完成" />
        </label>

        <p v-if="error" class="form-error full-field" aria-live="polite">{{ error }}</p>

        <footer class="modal-actions full-field">
          <button class="secondary-button" type="button" @click="emit('close')">取消</button>
          <button class="primary-button" type="submit" :disabled="submitting || !form.title.trim() || !form.columnId">
            {{ submitting ? '创建中...' : '创建任务' }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
