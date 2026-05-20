<script setup lang="ts">
import type { BoardColumn } from '../../api/board'
import TaskCard from './TaskCard.vue'

const props = defineProps<{
  column: BoardColumn
}>()

const emit = defineEmits<{
  openTask: [taskId: number]
  moveTask: [taskId: number, columnId: number, sortOrder: number]
}>()

function allowDrop(event: DragEvent) {
  event.preventDefault()
}

function dropTask(event: DragEvent) {
  event.preventDefault()
  const taskId = Number(event.dataTransfer?.getData('application/sd-kanban-task') || event.dataTransfer?.getData('text/plain'))
  if (Number.isFinite(taskId)) {
    emit('moveTask', taskId, props.column.id, props.column.tasks.length)
  }
}
</script>

<template>
  <section class="board-column" @dragover="allowDrop" @drop="dropTask">
    <header class="board-column-header">
      <span class="column-swatch" :style="{ background: column.color }"></span>
      <h2>{{ column.name }}</h2>
      <small>{{ column.tasks.length }}</small>
    </header>
    <div class="task-stack">
      <TaskCard
        v-for="task in column.tasks"
        :key="task.id"
        :task="task"
        @open="emit('openTask', $event)"
      />
    </div>
  </section>
</template>
