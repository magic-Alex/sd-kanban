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

const projectBadgeStyle = computed(() => ({
  '--project-color': props.task.projectColor ?? '#64748b',
}))
</script>

<template>
  <article class="task-card" draggable="true" @dragstart="startDrag" @click="emit('open', task.id)">
    <div class="task-card-main">
      <span
        v-if="task.projectCode"
        class="task-project-badge"
        :style="projectBadgeStyle"
        :title="task.projectName ? `${task.projectCode} · ${task.projectName}` : task.projectCode"
      >
        {{ task.projectCode }}
      </span>
      <strong>{{ task.title }}</strong>
      <div class="task-card-badges">
        <span class="task-type">{{ task.taskType }}</span>
        <span class="task-priority" :class="`priority-${task.priority.toLowerCase()}`">
          {{ task.priority === 'HIGH' ? '高' : task.priority === 'MEDIUM' ? '中' : task.priority === 'LOW' ? '低' : task.priority }}
        </span>
      </div>
    </div>
    <div class="task-card-meta">
      <small v-if="task.storyPoints !== null">{{ task.storyPoints }} SP</small>
      <small class="task-assignee">{{ task.assignee?.nickname ?? '未分配' }}</small>
      <small v-if="task.dueDate" class="task-due-date" :class="{ overdue: isOverdue }">{{ task.dueDate }}</small>
      <small v-if="task.checklistTotalCount > 0" class="task-checklist-progress">
        清单 {{ task.checklistDoneCount }}/{{ task.checklistTotalCount }}
      </small>
    </div>
  </article>
</template>
