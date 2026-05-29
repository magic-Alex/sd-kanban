<script setup lang="ts">
import { computed } from 'vue'
import type { TaskCard } from '../../api/board'

const props = defineProps<{
  task: TaskCard
}>()

const emit = defineEmits<{
  open: [taskId: number]
  dragStart: [taskId: number]
}>()

function startDrag(event: DragEvent) {
  event.dataTransfer?.setData('text/plain', String(props.task.id))
  event.dataTransfer?.setData('application/sd-kanban-task', String(props.task.id))
  emit('dragStart', props.task.id)
}

const isOverdue = computed(() => {
  if (!props.task.dueDate) {
    return false
  }
  return props.task.dueDate < new Date().toISOString().slice(0, 10)
})
</script>

<template>
  <article class="task-card" draggable="true" @dragstart="startDrag" @click="emit('open', task.id)">
    <div class="task-card-main">
      <strong>{{ task.title }}</strong>
      <span>{{ task.taskType }} · {{ task.priority }}</span>
      <span>{{ task.assignee?.nickname ?? '未分配' }}</span>
    </div>
    <div class="task-card-meta">
      <small v-if="task.storyPoints !== null">{{ task.storyPoints }} SP</small>
      <small v-if="task.dueDate" :class="{ overdue: isOverdue }">{{ task.dueDate }}</small>
      <small v-if="task.checklistTotalCount > 0">清单 {{ task.checklistDoneCount }}/{{ task.checklistTotalCount }}</small>
    </div>
  </article>
</template>
