<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { BoardColumn } from '../../api/board'
import type { ProjectMember } from '../../api/projects'
import type { TaskActivity, TaskComment, TaskResponse, UpdateTaskRequest } from '../../api/tasks'

const props = defineProps<{
  open: boolean
  task: TaskResponse | null
  comments: TaskComment[]
  activities: TaskActivity[]
  members: ProjectMember[]
  columns: BoardColumn[]
  actionLoading?: boolean
  actionError?: string | null
  addComment: (content: string) => Promise<void> | void
  saveTask: (request: UpdateTaskRequest) => Promise<void> | void
  completeTask: () => Promise<void> | void
  archiveTask: () => Promise<void> | void
  deleteTask: () => Promise<void> | void
}>()

const emit = defineEmits<{
  close: []
}>()

const comment = ref('')
const commentError = ref<string | null>(null)
const submittingComment = ref(false)
const editing = ref(false)
const editError = ref<string | null>(null)
const draft = reactive({
  title: '',
  description: '',
  taskType: 'STORY',
  priority: 'MEDIUM',
  assigneeId: '',
  storyPoints: '',
  estimatedHours: '',
  dueDate: '',
  acceptanceCriteria: '',
})

const hasDoneColumn = computed(() => props.columns.some((column) => column.isDone))
const actionErrorMessage = computed(() => editError.value ?? props.actionError ?? null)

watch(
  () => [props.task?.id, props.task?.updatedAt, props.open] as const,
  () => {
    resetDraft()
    editError.value = null
    editing.value = false
  },
  { immediate: true },
)

function resetDraft() {
  const task = props.task
  draft.title = task?.title ?? ''
  draft.description = task?.description ?? ''
  draft.taskType = task?.taskType ?? 'STORY'
  draft.priority = task?.priority ?? 'MEDIUM'
  draft.assigneeId = task?.assignee?.id ? String(task.assignee.id) : ''
  draft.storyPoints = task?.storyPoints === null || task?.storyPoints === undefined ? '' : String(task.storyPoints)
  draft.estimatedHours = task?.estimatedHours === null || task?.estimatedHours === undefined
    ? ''
    : String(task.estimatedHours)
  draft.dueDate = task?.dueDate ?? ''
  draft.acceptanceCriteria = task?.acceptanceCriteria ?? ''
}

function nullableText(value: string) {
  const trimmed = value.trim()
  return trimmed ? trimmed : null
}

function nullableNumber(value: string) {
  const trimmed = value.trim()
  return trimmed ? Number(trimmed) : null
}

function addClearField(clearFields: string[], field: string, value: unknown) {
  if (value === null || value === '') {
    clearFields.push(field)
  }
}

function buildUpdateRequest(): UpdateTaskRequest {
  const description = nullableText(draft.description)
  const assigneeId = draft.assigneeId ? Number(draft.assigneeId) : null
  const storyPoints = nullableNumber(draft.storyPoints)
  const estimatedHours = nullableNumber(draft.estimatedHours)
  const dueDate = nullableText(draft.dueDate)
  const acceptanceCriteria = nullableText(draft.acceptanceCriteria)
  const clearFields: string[] = []

  addClearField(clearFields, 'description', description)
  addClearField(clearFields, 'assigneeId', assigneeId)
  addClearField(clearFields, 'storyPoints', storyPoints)
  addClearField(clearFields, 'estimatedHours', estimatedHours)
  addClearField(clearFields, 'dueDate', dueDate)
  addClearField(clearFields, 'acceptanceCriteria', acceptanceCriteria)

  return {
    title: draft.title.trim(),
    description,
    taskType: draft.taskType,
    priority: draft.priority,
    assigneeId,
    storyPoints,
    estimatedHours,
    dueDate,
    acceptanceCriteria,
    clearFields,
  }
}

async function saveEdits() {
  if (props.actionLoading) {
    return
  }
  editError.value = null
  if (!draft.title.trim()) {
    editError.value = '任务标题不能为空'
    return
  }
  const taskId = props.task?.id
  try {
    await props.saveTask(buildUpdateRequest())
    if (props.open && props.task?.id === taskId) {
      editing.value = false
    }
  } catch (error) {
    if (props.open && props.task?.id === taskId) {
      editError.value = '任务保存失败，请重试'
    }
  }
}

async function completeCurrentTask() {
  if (props.actionLoading || !hasDoneColumn.value) {
    return
  }
  editError.value = null
  const taskId = props.task?.id
  try {
    await props.completeTask()
  } catch (error) {
    if (props.open && props.task?.id === taskId) {
      editError.value = '任务完成失败，请重试'
    }
  }
}

async function archiveCurrentTask() {
  if (props.actionLoading) {
    return
  }
  editError.value = null
  const taskId = props.task?.id
  try {
    await props.archiveTask()
  } catch (error) {
    if (props.open && props.task?.id === taskId) {
      editError.value = '任务归档失败，请重试'
    }
  }
}

async function deleteCurrentTask() {
  if (props.actionLoading || !window.confirm('确认删除该任务？删除后将从看板中隐藏。')) {
    return
  }
  editError.value = null
  const taskId = props.task?.id
  try {
    await props.deleteTask()
  } catch (error) {
    if (props.open && props.task?.id === taskId) {
      editError.value = '任务删除失败，请重试'
    }
  }
}

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
          <div class="drawer-actions">
            <button
              class="secondary-button"
              type="button"
              aria-label="编辑任务"
              :disabled="!task || actionLoading"
              @click="editing = true"
            >
              编辑
            </button>
            <button
              class="secondary-button"
              type="button"
              aria-label="标记完成"
              :disabled="!task || !hasDoneColumn || actionLoading"
              @click="completeCurrentTask"
            >
              标记完成
            </button>
            <button
              class="secondary-button"
              type="button"
              aria-label="归档任务"
              :disabled="!task || actionLoading"
              @click="archiveCurrentTask"
            >
              归档
            </button>
            <button
              class="secondary-button danger-button"
              type="button"
              aria-label="删除任务"
              :disabled="!task || actionLoading"
              @click="deleteCurrentTask"
            >
              删除
            </button>
            <button class="secondary-button" type="button" @click="emit('close')">关闭</button>
          </div>
        </header>

        <template v-if="task">
          <p v-if="actionErrorMessage" class="form-error" aria-live="polite">{{ actionErrorMessage }}</p>

          <form v-if="editing" class="task-edit-form" aria-label="编辑任务表单" @submit.prevent="saveEdits">
            <label class="full-field">
              标题
              <input v-model="draft.title" aria-label="编辑任务标题" />
            </label>
            <label class="full-field">
              描述
              <textarea v-model="draft.description" aria-label="编辑任务描述" rows="3" />
            </label>
            <label>
              类型
              <select v-model="draft.taskType" aria-label="编辑任务类型">
                <option value="STORY">STORY</option>
                <option value="TASK">TASK</option>
                <option value="BUG">BUG</option>
              </select>
            </label>
            <label>
              优先级
              <select v-model="draft.priority" aria-label="编辑任务优先级">
                <option value="LOW">LOW</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HIGH">HIGH</option>
                <option value="URGENT">URGENT</option>
              </select>
            </label>
            <label>
              负责人
              <select v-model="draft.assigneeId" aria-label="编辑任务负责人">
                <option value="">未分配</option>
                <option v-for="member in members" :key="member.user.id" :value="String(member.user.id)">
                  {{ member.user.nickname }}
                </option>
              </select>
            </label>
            <label>
              故事点
              <input v-model="draft.storyPoints" aria-label="编辑故事点" type="number" min="0" />
            </label>
            <label>
              预计工时
              <input v-model="draft.estimatedHours" aria-label="编辑预计工时" type="number" min="0" step="0.5" />
            </label>
            <label>
              截止日期
              <input v-model="draft.dueDate" aria-label="编辑截止日期" type="date" />
            </label>
            <label class="full-field">
              验收标准
              <textarea v-model="draft.acceptanceCriteria" aria-label="编辑验收标准" rows="3" />
            </label>
            <div class="modal-actions full-field">
              <button class="secondary-button" type="button" :disabled="actionLoading" @click="editing = false">
                取消
              </button>
              <button class="primary-button" type="submit" aria-label="保存任务" :disabled="actionLoading">
                {{ actionLoading ? '保存中...' : '保存任务' }}
              </button>
            </div>
          </form>

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
              <li v-for="commentItem in comments" :key="commentItem.id">
                <strong>{{ commentItem.author.nickname }}</strong>
                <p>{{ commentItem.content }}</p>
              </li>
            </ul>
          </section>

          <section class="drawer-section">
            <h2>动态</h2>
            <ul class="drawer-list">
              <li v-for="activity in activities" :key="activity.id">
                <strong>{{ activity.actor?.nickname ?? '系统' }}</strong>
                <p>{{ activity.actionType }} {{ activity.fieldName }} {{ activity.oldValue }} -> {{ activity.newValue }}</p>
              </li>
            </ul>
          </section>
        </template>
      </aside>
    </div>
  </Teleport>
</template>
