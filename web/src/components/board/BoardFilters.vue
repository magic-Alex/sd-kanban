<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { BoardQuery } from '../../api/board'

const props = defineProps<{
  modelValue: BoardQuery
}>()

const emit = defineEmits<{
  'update:modelValue': [value: BoardQuery]
  apply: [value: BoardQuery]
}>()

const filters = reactive<BoardQuery>({ ...props.modelValue })

watch(
  () => props.modelValue,
  (value) => Object.assign(filters, value),
)

function apply() {
  const value = { ...filters }
  emit('update:modelValue', value)
  emit('apply', value)
}
</script>

<template>
  <form class="board-filters" @submit.prevent="apply">
    <input v-model="filters.keyword" placeholder="搜索任务" />
    <select v-model="filters.type" aria-label="任务类型">
      <option value="">全部类型</option>
      <option value="TASK">任务</option>
      <option value="STORY">故事</option>
      <option value="BUG">缺陷</option>
    </select>
    <select v-model="filters.priority" aria-label="优先级">
      <option value="">全部优先级</option>
      <option value="HIGH">高</option>
      <option value="MEDIUM">中</option>
      <option value="LOW">低</option>
    </select>
    <button type="submit">筛选</button>
  </form>
</template>
