<script setup lang="ts">
import type { BoardColumn, MyTaskBoardGroup } from '../../api/board'
import TaskCard from './TaskCard.vue'

const props = defineProps<{
  column: BoardColumn | MyTaskBoardGroup
  showCreateButton?: boolean
}>()

const emit = defineEmits<{
  openTask: [taskId: number]
  moveTask: [taskId: number, columnId: number | null, sortOrder: number, templateKey: string]
  createTask: [columnId: number]
}>()

function columnId() {
  return 'id' in props.column ? props.column.id : null
}

function createTask() {
  const id = columnId()
  if (id !== null) {
    emit('createTask', id)
  }
}

function allowDrop(event: DragEvent) {
  event.preventDefault()
}

function dropTask(event: DragEvent) {
  event.preventDefault()
  const taskId = Number(event.dataTransfer?.getData('application/sd-kanban-task') || event.dataTransfer?.getData('text/plain'))
  if (Number.isFinite(taskId)) {
    emit('moveTask', taskId, columnId(), props.column.tasks.length, props.column.templateKey)
  }
}
</script>

<template>
  <section
    class="board-column"
    :data-template-key="column.templateKey"
    @dragover="allowDrop"
    @drop="dropTask"
  >
    <header class="board-column-header">
      <span class="column-swatch" :style="{ background: column.color }"></span>
      <h2>{{ column.name }}</h2>
      <small>{{ column.tasks.length }}</small>
      <button
        v-if="showCreateButton !== false && columnId() !== null"
        class="column-add-button"
        type="button"
        :aria-label="`在 ${column.name} 新增任务`"
        @click="createTask"
      >
        +任务
      </button>
    </header>
    <div class="task-stack" :class="{ empty: column.tasks.length === 0 }">
      <p v-if="column.tasks.length === 0" class="empty-column">暂无任务</p>
      <TaskCard
        v-for="task in column.tasks"
        :key="task.id"
        :task="task"
        @open="emit('openTask', $event)"
      />
    </div>
  </section>
</template>
