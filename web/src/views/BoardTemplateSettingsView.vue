<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { BoardColumnTemplate, SaveBoardColumnTemplateRequest } from '../api/settings'
import { useSettingsStore } from '../stores/settings'

const settings = useSettingsStore()
const editingKey = ref<string | null>(null)
const saving = ref(false)
const deletingKey = ref<string | null>(null)
const reorderingKey = ref<string | null>(null)
const formError = ref<string | null>(null)

const form = reactive({
  templateKey: '',
  nameZh: '',
  nameEn: '',
  color: '#0ea5e9',
  wipLimit: '',
  isDone: false,
})

const sortedTemplates = computed(() =>
  [...settings.boardTemplates].sort((left, right) => left.sortOrder - right.sortOrder),
)
const reorderPending = computed(() => Boolean(reorderingKey.value))
const templateMutationPending = computed(() => saving.value || Boolean(deletingKey.value) || reorderPending.value)

onMounted(async () => {
  try {
    await settings.loadBoardTemplates()
  } catch (error) {
    // Store exposes the user-facing error.
  }
})

async function refreshTemplates() {
  if (templateMutationPending.value) {
    return
  }
  await settings.loadBoardTemplates()
}

function resetForm() {
  editingKey.value = null
  form.templateKey = ''
  form.nameZh = ''
  form.nameEn = ''
  form.color = '#0ea5e9'
  form.wipLimit = ''
  form.isDone = false
  formError.value = null
}

function editTemplate(template: BoardColumnTemplate) {
  if (reorderPending.value) {
    return
  }
  editingKey.value = template.templateKey
  form.templateKey = template.templateKey
  form.nameZh = template.nameZh
  form.nameEn = template.nameEn
  form.color = template.color
  form.wipLimit = template.wipLimit === null ? '' : String(template.wipLimit)
  form.isDone = template.isDone
  formError.value = null
}

function templatePayload(): SaveBoardColumnTemplateRequest {
  const trimmedWipLimit = String(form.wipLimit).trim()
  return {
    templateKey: editingKey.value ?? form.templateKey.trim(),
    nameZh: form.nameZh.trim(),
    nameEn: form.nameEn.trim(),
    color: form.color,
    wipLimit: trimmedWipLimit === '' ? null : Number(trimmedWipLimit),
    isDone: form.isDone,
  }
}

async function saveTemplate() {
  if (saving.value || reorderPending.value) {
    return
  }
  formError.value = null
  const payload = templatePayload()
  if (!payload.templateKey || !payload.nameZh || !payload.nameEn) {
    formError.value = '请填写模板编码、中文名和英文名'
    return
  }
  if (payload.wipLimit !== null && (!Number.isInteger(payload.wipLimit) || payload.wipLimit < 0)) {
    formError.value = 'WIP 限制必须为空或非负整数'
    return
  }

  saving.value = true
  try {
    await settings.saveBoardTemplate(editingKey.value, payload)
    resetForm()
  } catch (error) {
    formError.value = settings.error
  } finally {
    saving.value = false
  }
}

async function removeTemplate(templateKey: string) {
  if (deletingKey.value || reorderPending.value) {
    return
  }
  if (!window.confirm(`确认删除模板 ${templateKey}？`)) {
    return
  }
  deletingKey.value = templateKey
  try {
    await settings.remove(templateKey)
    if (editingKey.value === templateKey) {
      resetForm()
    }
  } catch (error) {
    formError.value = settings.error
  } finally {
    deletingKey.value = null
  }
}

async function moveTemplate(templateKey: string, direction: -1 | 1) {
  if (reorderPending.value) {
    return
  }
  const templates = sortedTemplates.value
  const currentIndex = templates.findIndex((template) => template.templateKey === templateKey)
  const targetIndex = currentIndex + direction
  if (currentIndex < 0 || targetIndex < 0 || targetIndex >= templates.length) {
    return
  }
  const nextKeys = templates.map((template) => template.templateKey)
  const [moved] = nextKeys.splice(currentIndex, 1)
  nextKeys.splice(targetIndex, 0, moved)
  reorderingKey.value = templateKey
  try {
    await settings.reorder(nextKeys)
  } catch (error) {
    formError.value = settings.error
  } finally {
    reorderingKey.value = null
  }
}
</script>

<template>
  <main class="page-surface settings-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Settings</p>
        <h1>系统设置</h1>
      </div>
      <button class="secondary-button" type="button" :disabled="settings.loading || templateMutationPending" @click="refreshTemplates">
        刷新
      </button>
    </header>

    <p v-if="settings.error" class="form-error">{{ settings.error }}</p>
    <p v-else-if="settings.loading" class="muted">正在加载看板模板...</p>

    <section class="split-layout settings-layout">
      <section class="panel-block">
        <div class="section-heading">
          <div>
            <h2>看板模板</h2>
            <small>全局列配置会影响个人任务看板分组</small>
          </div>
        </div>

        <div class="template-list">
          <article v-for="(template, index) in sortedTemplates" :key="template.templateKey" class="template-row">
            <span class="template-swatch" :style="{ background: template.color }"></span>
            <span class="template-main">
              <strong>{{ template.displayName }}</strong>
              <small>{{ template.templateKey }} · {{ template.nameEn }}</small>
            </span>
            <span class="template-meta">
              <small>排序 {{ template.sortOrder }}</small>
              <small>{{ template.wipLimit === null ? '无 WIP' : `WIP ${template.wipLimit}` }}</small>
              <small v-if="template.isDone" class="status-pill">完成列</small>
            </span>
            <span class="template-actions">
              <button
                class="secondary-button"
                type="button"
                :disabled="index === 0 || Boolean(reorderingKey)"
                :aria-label="`上移 ${template.templateKey}`"
                @click="moveTemplate(template.templateKey, -1)"
              >
                上移
              </button>
              <button
                class="secondary-button"
                type="button"
                :disabled="index === sortedTemplates.length - 1 || Boolean(reorderingKey)"
                :aria-label="`下移 ${template.templateKey}`"
                @click="moveTemplate(template.templateKey, 1)"
              >
                下移
              </button>
              <button
                class="secondary-button"
                type="button"
                :disabled="reorderPending"
                :aria-label="`编辑 ${template.templateKey}`"
                @click="editTemplate(template)"
              >
                编辑
              </button>
              <button
                class="danger-button"
                type="button"
                :disabled="deletingKey === template.templateKey || reorderPending"
                :aria-label="`删除模板 ${template.templateKey}`"
                @click="removeTemplate(template.templateKey)"
              >
                删除模板
              </button>
            </span>
          </article>
        </div>
      </section>

      <section class="panel-block">
        <div class="section-heading">
          <div>
            <h2>{{ editingKey ? '编辑模板' : '新建模板' }}</h2>
            <small>模板编码创建后不可修改</small>
          </div>
          <button v-if="editingKey" class="secondary-button" type="button" :disabled="reorderPending" @click="resetForm">取消编辑</button>
        </div>

        <form class="template-form" @submit.prevent="saveTemplate">
          <label>
            模板编码
            <input
              v-model.trim="form.templateKey"
              data-testid="template-key-input"
              :disabled="Boolean(editingKey) || saving || reorderPending"
              placeholder="READY"
              autocomplete="off"
            />
          </label>
          <label>
            中文名
            <input v-model.trim="form.nameZh" data-testid="name-zh-input" :disabled="saving || reorderPending" placeholder="准备" />
          </label>
          <label>
            英文名
            <input v-model.trim="form.nameEn" data-testid="name-en-input" :disabled="saving || reorderPending" placeholder="Ready" />
          </label>
          <label>
            颜色
            <input v-model="form.color" data-testid="color-input" :disabled="saving || reorderPending" type="color" />
          </label>
          <label>
            WIP 限制
            <input
              v-model.trim="form.wipLimit"
              data-testid="wip-limit-input"
              :disabled="saving || reorderPending"
              type="number"
              min="0"
              step="1"
              placeholder="留空表示不限"
            />
          </label>
          <label class="inline-check">
            <input v-model="form.isDone" data-testid="is-done-input" :disabled="saving || reorderPending" type="checkbox" />
            完成列
          </label>

          <p v-if="formError" class="form-error full-field">{{ formError }}</p>
          <div class="modal-actions full-field">
            <button class="secondary-button" type="button" :disabled="saving || reorderPending" @click="resetForm">重置</button>
            <button class="primary-button" type="submit" :disabled="saving || reorderPending">
              {{ saving ? '保存中...' : '保存模板' }}
            </button>
          </div>
        </form>
      </section>
    </section>
  </main>
</template>
