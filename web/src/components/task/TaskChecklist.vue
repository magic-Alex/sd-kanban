<script setup lang="ts">
import { computed, ref } from 'vue'
import type { TaskChecklistItem } from '../../api/checklist'

const props = defineProps<{
  items: TaskChecklistItem[]
  actionLoading?: boolean
  addItem: (title: string) => Promise<void> | void
  toggleItem: (itemId: number) => Promise<void> | void
  renameItem: (itemId: number, title: string) => Promise<void> | void
  moveItem: (itemId: number, direction: 'up' | 'down') => Promise<void> | void
  deleteItem: (itemId: number) => Promise<void> | void
}>()

const newTitle = ref('')
const submitting = ref(false)
const pendingItemIds = ref(new Set<number>())
const editingItemId = ref<number | null>(null)
const editingTitle = ref('')
const localError = ref<string | null>(null)

const doneCount = computed(() => props.items.filter((item) => item.done).length)
const totalCount = computed(() => props.items.length)

function itemPending(itemId: number) {
  return pendingItemIds.value.has(itemId)
}

function setItemPending(itemId: number, pending: boolean) {
  const nextPendingIds = new Set(pendingItemIds.value)
  if (pending) {
    nextPendingIds.add(itemId)
  } else {
    nextPendingIds.delete(itemId)
  }
  pendingItemIds.value = nextPendingIds
}

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
  if (props.actionLoading || itemPending(itemId)) {
    return
  }
  setItemPending(itemId, true)
  localError.value = null
  try {
    await props.toggleItem(itemId)
  } catch (error) {
    localError.value = '检查项更新失败'
  } finally {
    setItemPending(itemId, false)
  }
}

async function deleteItem(itemId: number) {
  if (props.actionLoading || itemPending(itemId)) {
    return
  }
  setItemPending(itemId, true)
  localError.value = null
  try {
    await props.deleteItem(itemId)
  } catch (error) {
    localError.value = '检查项更新失败'
  } finally {
    setItemPending(itemId, false)
  }
}

function startRename(item: TaskChecklistItem) {
  if (props.actionLoading || itemPending(item.id)) {
    return
  }
  editingItemId.value = item.id
  editingTitle.value = item.title
  localError.value = null
}

function cancelRename() {
  editingItemId.value = null
  editingTitle.value = ''
}

async function saveRename(item: TaskChecklistItem) {
  const title = editingTitle.value.trim()
  if (!title || props.actionLoading || itemPending(item.id)) {
    return
  }
  if (title === item.title) {
    cancelRename()
    return
  }
  setItemPending(item.id, true)
  localError.value = null
  try {
    await props.renameItem(item.id, title)
    cancelRename()
  } catch (error) {
    localError.value = '检查项更新失败'
  } finally {
    setItemPending(item.id, false)
  }
}

async function moveItem(item: TaskChecklistItem, direction: 'up' | 'down') {
  if (props.actionLoading || itemPending(item.id)) {
    return
  }
  setItemPending(item.id, true)
  localError.value = null
  try {
    await props.moveItem(item.id, direction)
  } catch (error) {
    localError.value = '检查项排序失败'
  } finally {
    setItemPending(item.id, false)
  }
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
      <li v-for="(item, index) in items" :key="item.id" class="checklist-item">
        <label v-if="editingItemId !== item.id" :class="{ done: item.done }">
          <input
            type="checkbox"
            :checked="item.done"
            :disabled="actionLoading || itemPending(item.id)"
            :aria-label="`切换检查项 ${item.title}`"
            @change="toggleItem(item.id)"
          />
          <span>{{ item.title }}</span>
        </label>
        <form
          v-else
          class="checklist-rename-form"
          @submit.prevent="saveRename(item)"
        >
          <input v-model="editingTitle" :aria-label="`编辑检查项标题 ${item.title}`" />
          <button
            class="secondary-button"
            type="submit"
            :disabled="actionLoading || itemPending(item.id) || !editingTitle.trim()"
            :aria-label="`保存检查项 ${item.title}`"
          >
            保存
          </button>
          <button class="secondary-button" type="button" :disabled="itemPending(item.id)" @click="cancelRename">
            取消
          </button>
        </form>
        <button
          class="secondary-button"
          type="button"
          :disabled="actionLoading || itemPending(item.id) || index === 0"
          :aria-label="`上移检查项 ${item.title}`"
          @click="moveItem(item, 'up')"
        >
          上移
        </button>
        <button
          class="secondary-button"
          type="button"
          :disabled="actionLoading || itemPending(item.id) || index === items.length - 1"
          :aria-label="`下移检查项 ${item.title}`"
          @click="moveItem(item, 'down')"
        >
          下移
        </button>
        <button
          class="secondary-button"
          type="button"
          :disabled="actionLoading || itemPending(item.id)"
          :aria-label="`编辑检查项 ${item.title}`"
          @click="startRename(item)"
        >
          编辑
        </button>
        <button
          class="secondary-button danger-button"
          type="button"
          :disabled="actionLoading || itemPending(item.id)"
          :aria-label="`删除检查项 ${item.title}`"
          @click="deleteItem(item.id)"
        >
          删除
        </button>
      </li>
    </ul>
  </section>
</template>
