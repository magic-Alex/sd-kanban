<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import BoardColumn from '../components/board/BoardColumn.vue'
import BoardFilters from '../components/board/BoardFilters.vue'
import TaskDrawer from '../components/task/TaskDrawer.vue'
import type { BoardQuery } from '../api/board'
import { useBoardStore } from '../stores/board'
import { useTasksStore } from '../stores/tasks'

const route = useRoute()
const board = useBoardStore()
const tasks = useTasksStore()
const filters = ref<BoardQuery>({})
const projectId = String(route.params.projectId)

onMounted(() => {
  board.loadProjectBoard(projectId, filters.value)
})

function applyFilters(value: BoardQuery) {
  board.loadProjectBoard(projectId, value)
}

function moveTask(taskId: number, columnId: number, sortOrder: number) {
  board.moveTask(taskId, columnId, sortOrder)
}
</script>

<template>
  <main class="page-surface board-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Board</p>
        <h1>项目看板</h1>
      </div>
    </header>

    <BoardFilters v-model="filters" @apply="applyFilters" />
    <p v-if="board.error" class="form-error">{{ board.error }}</p>
    <p v-else-if="board.loading" class="muted">正在加载看板...</p>

    <section class="board-lane" aria-label="项目总体看板">
      <BoardColumn
        v-for="column in board.projectBoard?.columns ?? []"
        :key="column.id"
        :column="column"
        @open-task="tasks.openTask"
        @move-task="moveTask"
      />
    </section>

    <TaskDrawer
      :open="tasks.drawerOpen"
      :task="tasks.activeTask"
      :comments="tasks.comments"
      :activities="tasks.activities"
      :add-comment="tasks.addComment"
      @close="tasks.closeDrawer"
    />
  </main>
</template>
