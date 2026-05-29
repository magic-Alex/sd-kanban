<script setup lang="ts">
import { computed, ref } from 'vue'
import type { TaskChecklistItem } from '../../api/checklist'

const props = defineProps<{
  items: TaskChecklistItem[]
  actionLoading?: boolean
  addItem: (title: string) => Promise<void> | void
  toggleItem: (itemId: number) => Promise<void> | void
  renameItem: (itemId: number, title: string) => Promise<void> | void
  deleteItem: (itemId: number) => Promise<void> | void
}>()

const newTitle = ref('')
const submitting = ref(false)
const localError = ref<string | null>(null)

const doneCount = computed(() => props.items.filter((item) => item.done).length)
const totalCount = computed(() => props.items.length)

async function submitItem() {
  const title = newTitle.value.trim()
  if (!title || props.actionLoading || submitting.value) {
    return
  }
  submitting.value = true
  localError.value = null
  try {
    await props.addItem(title)
    newTitle.value = ''
  } catch (error) {
    localError.value = '检查项保存失败，请重试'
  } finally {
    submitting.value = false
  }
}

async function toggleItem(itemId: number) {
  if (props.actionLoading) {
    return
  }
  await props.toggleItem(itemId)
}

async function deleteItem(itemId: number) {
  if (props.actionLoading) {
    return
  }
  await props.deleteItem(itemId)
}
</script>

<template>
  <section class="drawer-section task-checklist">
    <h2>检查清单 {{ doneCount }}/{{ totalCount }}</h2>

    <form class="checklist-form" @submit.prevent="submitItem">
      <label>
        新增检查项
        <input v-model="newTitle" aria-label="新增检查项" />
      </label>
      <button class="primary-button" type="submit" :disabled="actionLoading || submitting || !newTitle.trim()">
        {{ submitting ? '添加中...' : '添加' }}
      </button>
    </form>
    <p v-if="localError" class="form-error" aria-live="polite">{{ localError }}</p>

    <ul class="checklist-items">
      <li v-for="item in items" :key="item.id" class="checklist-item">
        <label :class="{ done: item.done }">
          <input
            type="checkbox"
            :checked="item.done"
            :disabled="actionLoading"
            :aria-label="`切换检查项 ${item.title}`"
            @change="toggleItem(item.id)"
          />
          <span>{{ item.title }}</span>
        </label>
        <button
          class="secondary-button danger-button"
          type="button"
          :disabled="actionLoading"
          :aria-label="`删除检查项 ${item.title}`"
          @click="deleteItem(item.id)"
        >
          删除
        </button>
      </li>
    </ul>
  </section>
</template>
