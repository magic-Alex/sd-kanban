<script setup lang="ts">
import { onMounted, ref } from 'vue'
import TaskCard from '../components/board/TaskCard.vue'
import TaskDrawer from '../components/task/TaskDrawer.vue'
import { useBoardStore } from '../stores/board'
import { useTasksStore } from '../stores/tasks'

const board = useBoardStore()
const tasks = useTasksStore()
const groupBy = ref('project')

onMounted(() => {
  board.loadMyTaskBoard(groupBy.value)
})

function reload() {
  board.loadMyTaskBoard(groupBy.value)
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
            @open="tasks.openTask"
          />
        </div>
      </section>
    </section>

    <TaskDrawer
      :open="tasks.drawerOpen"
      :task="tasks.activeTask"
      :comments="tasks.comments"
      :activities="tasks.activities"
      @close="tasks.closeDrawer"
      @add-comment="tasks.addComment"
    />
  </main>
</template>
