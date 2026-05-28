<script setup lang="ts">
import { onMounted, ref } from 'vue'
import TaskCard from '../components/board/TaskCard.vue'
import TaskDrawer from '../components/task/TaskDrawer.vue'
import { fetchProjectBoard, type BoardColumn } from '../api/board'
import { fetchProjectMembers, type ProjectMember } from '../api/projects'
import { updateTaskPosition, type UpdateTaskRequest } from '../api/tasks'
import { useBoardStore } from '../stores/board'
import { useTasksStore } from '../stores/tasks'

const board = useBoardStore()
const tasks = useTasksStore()
const groupBy = ref('project')
const drawerMembers = ref<ProjectMember[]>([])
const drawerColumns = ref<BoardColumn[]>([])
const completingTask = ref(false)
let contextRequestId = 0

onMounted(() => {
  board.loadMyTaskBoard(groupBy.value)
})

async function reload() {
  await board.loadMyTaskBoard(groupBy.value)
}

async function loadDrawerContext(projectId: number) {
  const requestId = contextRequestId + 1
  contextRequestId = requestId
  drawerMembers.value = []
  drawerColumns.value = []
  try {
    const [projectBoard, members] = await Promise.all([
      fetchProjectBoard(projectId),
      fetchProjectMembers(projectId),
    ])
    if (contextRequestId === requestId && tasks.drawerOpen && tasks.activeTask?.projectId === projectId) {
      drawerColumns.value = projectBoard.columns
      drawerMembers.value = members
    }
  } catch (error) {
    if (contextRequestId === requestId) {
      board.error = board.error ?? '任务项目信息加载失败'
    }
  }
}

async function openTask(taskId: number) {
  await tasks.openTask(taskId)
  if (tasks.activeTask) {
    await loadDrawerContext(tasks.activeTask.projectId)
  }
}

async function saveTask(update: UpdateTaskRequest) {
  await tasks.saveTask(update)
  if (tasks.activeTask) {
    await loadDrawerContext(tasks.activeTask.projectId)
  }
  try {
    await reload()
  } catch (error) {
    board.error = board.error ?? '看板刷新失败'
  }
}

async function completeTask() {
  const taskId = tasks.activeTask?.id
  const projectId = tasks.activeTask?.projectId
  const doneColumn = drawerColumns.value.find((column) => column.isDone)
  if (!taskId || !projectId || !doneColumn || completingTask.value) {
    return
  }
  completingTask.value = true
  try {
    await updateTaskPosition(taskId, { columnId: doneColumn.id, sortOrder: doneColumn.tasks.length })
    if (tasks.drawerOpen && tasks.activeTask?.id === taskId) {
      await tasks.openTask(taskId)
      await loadDrawerContext(projectId)
    }
    await reload()
  } finally {
    completingTask.value = false
  }
}

async function archiveTask() {
  await tasks.archiveActiveTask()
  await reload()
}

async function deleteTask() {
  await tasks.deleteActiveTask()
  await reload()
}
</script>

<template>
  <main class="page-surface board-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">My Tasks</p>
        <h1>我的任务</h1>
      </div>
      <select v-model="groupBy" class="compact-select" @change="reload">
        <option value="project">按项目</option>
        <option value="column">按状态</option>
      </select>
    </header>

    <p v-if="board.error" class="form-error">{{ board.error }}</p>
    <p v-else-if="board.loading" class="muted">正在加载任务...</p>

    <section class="board-lane" aria-label="个人任务看板">
      <section v-for="group in board.myTaskBoard?.groups ?? []" :key="group.id" class="board-column">
        <header class="board-column-header">
          <span class="column-swatch"></span>
          <h2>{{ group.name }}</h2>
          <small>{{ group.tasks.length }}</small>
        </header>
        <div class="task-stack">
          <TaskCard
            v-for="task in group.tasks"
            :key="task.id"
            :task="task"
            @open="openTask"
          />
        </div>
      </section>
    </section>

    <TaskDrawer
      :open="tasks.drawerOpen"
      :task="tasks.activeTask"
      :comments="tasks.comments"
      :activities="tasks.activities"
      :members="drawerMembers"
      :columns="drawerColumns"
      :action-loading="tasks.actionLoading || completingTask"
      :action-error="tasks.actionError"
      :add-comment="tasks.addComment"
      :save-task="saveTask"
      :complete-task="completeTask"
      :archive-task="archiveTask"
      :delete-task="deleteTask"
      @close="tasks.closeDrawer"
    />
  </main>
</template>
