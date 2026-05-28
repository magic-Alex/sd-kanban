<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import BoardColumn from '../components/board/BoardColumn.vue'
import BoardFilters from '../components/board/BoardFilters.vue'
import TaskCreateModal from '../components/task/TaskCreateModal.vue'
import TaskDrawer from '../components/task/TaskDrawer.vue'
import type { BoardQuery } from '../api/board'
import { fetchProjectMembers, type ProjectMember } from '../api/projects'
import type { CreateTaskRequest, UpdateTaskRequest } from '../api/tasks'
import { useBoardStore } from '../stores/board'
import { useTasksStore } from '../stores/tasks'

const route = useRoute()
const board = useBoardStore()
const tasks = useTasksStore()
const filters = ref<BoardQuery>({})
const projectId = String(route.params.projectId)
const members = ref<ProjectMember[]>([])
const createModalOpen = ref(false)
const createDefaultColumnId = ref<number | null>(null)
const createError = ref<string | null>(null)
const submittingTask = ref(false)

onMounted(() => {
  board.loadProjectBoard(projectId, filters.value)
  loadMembers()
})

function applyFilters(value: BoardQuery) {
  filters.value = value
  board.loadProjectBoard(projectId, value)
}

function moveTask(taskId: number, columnId: number, sortOrder: number) {
  board.moveTask(taskId, columnId, sortOrder)
}

async function loadMembers() {
  try {
    members.value = await fetchProjectMembers(projectId)
  } catch (error) {
    members.value = []
  }
}

function openCreateTask(columnId?: number) {
  createDefaultColumnId.value = columnId ?? board.projectBoard?.columns[0]?.id ?? null
  createError.value = null
  createModalOpen.value = true
}

async function submitTask(request: CreateTaskRequest) {
  submittingTask.value = true
  createError.value = null
  try {
    const task = await board.createTask(projectId, request, filters.value)
    createModalOpen.value = false
    await tasks.openTask(task.id)
  } catch (error) {
    createError.value = '任务创建失败，请检查字段后重试'
  } finally {
    submittingTask.value = false
  }
}

async function saveActiveTask(request: UpdateTaskRequest) {
  await tasks.saveTask(request)
  await board.refreshProjectBoard()
}

async function completeActiveTask() {
  if (!tasks.activeTask) {
    return
  }
  await board.markTaskComplete(tasks.activeTask.id)
  await tasks.openTask(tasks.activeTask.id)
}

async function archiveActiveTask() {
  const taskId = tasks.activeTask?.id
  await tasks.archiveActiveTask()
  if (taskId) {
    board.removeTaskFromBoard(taskId)
  }
}

async function deleteActiveTask() {
  const taskId = tasks.activeTask?.id
  await tasks.deleteActiveTask()
  if (taskId) {
    board.removeTaskFromBoard(taskId)
  }
}
</script>

<template>
  <main class="page-surface board-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Board</p>
        <h1>项目看板</h1>
      </div>
      <button class="primary-button" type="button" @click="openCreateTask()">新增任务</button>
    </header>

    <BoardFilters v-model="filters" :members="members" @apply="applyFilters" />
    <p v-if="board.error" class="form-error">{{ board.error }}</p>
    <p v-else-if="board.loading" class="muted">正在加载看板...</p>

    <section class="board-lane" aria-label="项目总体看板">
      <BoardColumn
        v-for="column in board.projectBoard?.columns ?? []"
        :key="column.id"
        :column="column"
        @open-task="tasks.openTask"
        @move-task="moveTask"
        @create-task="openCreateTask"
      />
    </section>

    <TaskCreateModal
      :open="createModalOpen"
      :columns="board.projectBoard?.columns ?? []"
      :members="members"
      :default-column-id="createDefaultColumnId"
      :submitting="submittingTask"
      :error="createError"
      @close="createModalOpen = false"
      @submit="submitTask"
    />

    <TaskDrawer
      :open="tasks.drawerOpen"
      :task="tasks.activeTask"
      :comments="tasks.comments"
      :activities="tasks.activities"
      :members="members"
      :columns="board.projectBoard?.columns ?? []"
      :action-loading="tasks.actionLoading"
      :action-error="tasks.actionError"
      :add-comment="tasks.addComment"
      :save-task="saveActiveTask"
      :complete-task="completeActiveTask"
      :archive-task="archiveActiveTask"
      :delete-task="deleteActiveTask"
      @close="tasks.closeDrawer"
    />
  </main>
</template>
