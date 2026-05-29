<script setup lang="ts">
import { computed, ref, watch } from 'vue'
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
const projectId = computed(() => String(route.params.projectId))
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
const archivedRequestId = ref(0)
const restoringTaskIds = ref<number[]>([])
const activeDrawerArchived = ref(false)

watch(
  projectId,
  () => {
    resetProjectContext()
    void board.loadProjectBoard(projectId.value, filters.value).catch(() => undefined)
    void loadMembers()
  },
  { immediate: true },
)

watch(
  () => [projectId.value, route.query.taskId],
  ([, taskId]) => {
    void openRouteTask(taskId)
  },
  { immediate: true },
)

function resetProjectContext() {
  filters.value = {}
  boardMode.value = 'board'
  archivedTasks.value = []
  archivedFilters.value = {}
  archivedLoading.value = false
  archivedError.value = null
  archivedRequestId.value += 1
  restoringTaskIds.value = []
  activeDrawerArchived.value = false
  createModalOpen.value = false
  createDefaultColumnId.value = null
  createError.value = null
  tasks.closeDrawer()
}

function applyFilters(value: BoardQuery) {
  filters.value = value
  board.loadProjectBoard(projectId.value, value)
}

function showBoard() {
  boardMode.value = 'board'
}

async function showArchivedTasks() {
  boardMode.value = 'archived'
  await loadArchivedTasks(archivedFilters.value)
}

async function loadArchivedTasks(value: ArchivedTaskQuery = archivedFilters.value) {
  const requestId = archivedRequestId.value + 1
  archivedRequestId.value = requestId
  archivedFilters.value = value
  archivedLoading.value = true
  archivedError.value = null
  try {
    const taskList = await fetchArchivedTasks(projectId.value, value)
    if (archivedRequestId.value === requestId) {
      archivedTasks.value = taskList
    }
  } catch (error) {
    if (archivedRequestId.value === requestId) {
      archivedError.value = '已归档任务加载失败'
    }
  } finally {
    if (archivedRequestId.value === requestId) {
      archivedLoading.value = false
    }
  }
}

function moveTask(taskId: number, columnId: number, sortOrder: number) {
  board.moveTask(taskId, columnId, sortOrder)
}

async function loadMembers() {
  const activeProjectId = projectId.value
  try {
    const projectMembers = await fetchProjectMembers(activeProjectId)
    if (projectId.value === activeProjectId) {
      members.value = projectMembers
    }
  } catch (error) {
    if (projectId.value === activeProjectId) {
      members.value = []
    }
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
    const task = await board.createTask(projectId.value, request, filters.value)
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

async function openBoardTask(taskId: number) {
  try {
    await tasks.openTask(taskId)
    if (tasks.activeTask?.id === taskId) {
      activeDrawerArchived.value = false
    }
  } catch (error) {
    // Keep the previous drawer task and source if the replacement task cannot load.
  }
}

async function openArchivedTask(taskId: number) {
  try {
    await tasks.openTask(taskId)
    if (tasks.activeTask?.id === taskId) {
      activeDrawerArchived.value = true
    }
  } catch (error) {
    // Keep the previous drawer task and source if the replacement task cannot load.
  }
}

function routeTaskId(value: unknown) {
  const rawValue = Array.isArray(value) ? value[0] : value
  const numericTaskId = Number(rawValue)
  return Number.isFinite(numericTaskId) && numericTaskId > 0 ? numericTaskId : null
}

async function openRouteTask(value: unknown) {
  const taskId = routeTaskId(value)
  if (taskId === null) {
    return
  }
  try {
    await tasks.openTask(taskId)
    if (tasks.activeTask?.id !== taskId || String(tasks.activeTask.projectId) !== projectId.value) {
      tasks.closeDrawer()
      activeDrawerArchived.value = false
      return
    }
    activeDrawerArchived.value = Boolean(tasks.activeTask.archived)
    if (activeDrawerArchived.value) {
      boardMode.value = 'archived'
      await loadArchivedTasks(archivedFilters.value)
    } else {
      boardMode.value = 'board'
    }
  } catch (error) {
    // Keep the page usable if a stale notification points at an inaccessible task.
  }
}

async function restoreArchivedTask(taskId: number) {
  if (restoringTaskIds.value.includes(taskId)) {
    return
  }
  restoringTaskIds.value = [...restoringTaskIds.value, taskId]
  archivedError.value = null
  try {
    await restoreTask(taskId)
    archivedTasks.value = archivedTasks.value.filter((task) => task.id !== taskId)
    try {
      await board.loadProjectBoard(projectId.value, filters.value)
    } catch (error) {
      archivedError.value = '任务已恢复，但看板刷新失败'
    }
    if (tasks.activeTask?.id === taskId) {
      tasks.closeDrawer()
      activeDrawerArchived.value = false
    }
  } catch (error) {
    archivedError.value = '任务恢复失败，请重试'
  } finally {
    restoringTaskIds.value = restoringTaskIds.value.filter((id) => id !== taskId)
  }
}

async function restoreActiveTask() {
  const taskId = tasks.activeTask?.id
  await tasks.restoreActiveTask()
  if (taskId) {
    archivedTasks.value = archivedTasks.value.filter((task) => task.id !== taskId)
    try {
      await board.loadProjectBoard(projectId.value, filters.value)
    } catch (error) {
      archivedError.value = '任务已恢复，但看板刷新失败'
    }
    activeDrawerArchived.value = false
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
          @open-task="openBoardTask"
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
      :restoring-task-ids="restoringTaskIds"
      @open-task="openArchivedTask"
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
      :archived="activeDrawerArchived"
      :action-loading="tasks.actionLoading || completingTask"
      :action-error="tasks.actionError"
      :add-comment="tasks.addComment"
      :add-checklist-item="tasks.addChecklistItem"
      :toggle-checklist-item="tasks.toggleChecklistItem"
      :rename-checklist-item="tasks.renameChecklistItem"
      :move-checklist-item="tasks.moveChecklistItem"
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
