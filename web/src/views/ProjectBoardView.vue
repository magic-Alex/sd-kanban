<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import BoardColumn from '../components/board/BoardColumn.vue'
import BoardFilters from '../components/board/BoardFilters.vue'
import ArchivedTaskList from '../components/task/ArchivedTaskList.vue'
import TaskCreateModal from '../components/task/TaskCreateModal.vue'
import TaskDrawer from '../components/task/TaskDrawer.vue'
import type { BoardQuery } from '../api/board'
import { fetchProjectMembers, type ProjectMember } from '../api/projects'
import {
  fetchArchivedTasks,
  restoreTask,
  type ArchivedTaskQuery,
  type CreateTaskRequest,
  type TaskResponse,
  type UpdateTaskRequest,
} from '../api/tasks'
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
const completingTask = ref(false)
const boardMode = ref<'board' | 'archived'>('board')
const archivedTasks = ref<TaskResponse[]>([])
const archivedFilters = ref<ArchivedTaskQuery>({})
const archivedLoading = ref(false)
const archivedError = ref<string | null>(null)

onMounted(() => {
  board.loadProjectBoard(projectId, filters.value)
  loadMembers()
})

function applyFilters(value: BoardQuery) {
  filters.value = value
  board.loadProjectBoard(projectId, value)
}

function showBoard() {
  boardMode.value = 'board'
}

async function showArchivedTasks() {
  boardMode.value = 'archived'
  await loadArchivedTasks(archivedFilters.value)
}

async function loadArchivedTasks(value: ArchivedTaskQuery = archivedFilters.value) {
  archivedFilters.value = value
  archivedLoading.value = true
  archivedError.value = null
  try {
    archivedTasks.value = await fetchArchivedTasks(projectId, value)
  } catch (error) {
    archivedError.value = '已归档任务加载失败'
  } finally {
    archivedLoading.value = false
  }
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
  try {
    await board.refreshProjectBoard()
  } catch (error) {
    board.error = board.error ?? '看板刷新失败'
  }
}

async function completeActiveTask() {
  const taskId = tasks.activeTask?.id
  if (!taskId || completingTask.value) {
    return
  }
  completingTask.value = true
  try {
    await board.markTaskComplete(taskId)
    if (tasks.drawerOpen && tasks.activeTask?.id === taskId) {
      await tasks.openTask(taskId)
    }
  } finally {
    completingTask.value = false
  }
}

async function archiveActiveTask() {
  const taskId = tasks.activeTask?.id
  await tasks.archiveActiveTask()
  if (taskId) {
    board.removeTaskFromBoard(taskId)
  }
}

async function restoreArchivedTask(taskId: number) {
  archivedError.value = null
  try {
    await restoreTask(taskId)
    archivedTasks.value = archivedTasks.value.filter((task) => task.id !== taskId)
    await board.loadProjectBoard(projectId, filters.value)
    if (tasks.activeTask?.id === taskId) {
      tasks.closeDrawer()
    }
  } catch (error) {
    archivedError.value = '任务恢复失败，请重试'
  }
}

async function restoreActiveTask() {
  const taskId = tasks.activeTask?.id
  await tasks.restoreActiveTask()
  if (taskId) {
    archivedTasks.value = archivedTasks.value.filter((task) => task.id !== taskId)
    await board.loadProjectBoard(projectId, filters.value)
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
      <div class="header-actions">
        <div class="segmented-control" aria-label="看板视图切换">
          <button type="button" :class="{ active: boardMode === 'board' }" @click="showBoard">
            当前看板
          </button>
          <button
            type="button"
            aria-label="查看已归档任务"
            :class="{ active: boardMode === 'archived' }"
            @click="showArchivedTasks"
          >
            已归档
          </button>
        </div>
        <button class="primary-button" type="button" @click="openCreateTask()">新增任务</button>
      </div>
    </header>

    <template v-if="boardMode === 'board'">
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
    </template>

    <ArchivedTaskList
      v-else
      :tasks="archivedTasks"
      :members="members"
      :loading="archivedLoading"
      :error="archivedError"
      @open-task="tasks.openTask"
      @restore-task="restoreArchivedTask"
      @apply-filters="loadArchivedTasks"
      />

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
      :checklist-items="tasks.checklistItems"
      :members="members"
      :columns="board.projectBoard?.columns ?? []"
      :archived="boardMode === 'archived'"
      :action-loading="tasks.actionLoading || completingTask"
      :action-error="tasks.actionError"
      :add-comment="tasks.addComment"
      :add-checklist-item="tasks.addChecklistItem"
      :toggle-checklist-item="tasks.toggleChecklistItem"
      :rename-checklist-item="tasks.renameChecklistItem"
      :delete-checklist-item="tasks.removeChecklistItem"
      :save-task="saveActiveTask"
      :complete-task="completeActiveTask"
      :archive-task="archiveActiveTask"
      :restore-task="restoreActiveTask"
      :delete-task="deleteActiveTask"
      @close="tasks.closeDrawer"
    />
  </main>
</template>
