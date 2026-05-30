<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useProjectsStore } from '../stores/projects'

const router = useRouter()
const projects = useProjectsStore()
const creating = ref(false)
const form = reactive({
  projectCode: '',
  projectColor: '#0ea5e9',
  name: '',
  description: '',
})

onMounted(() => {
  projects.fetchProjects()
})

async function submit() {
  creating.value = true
  try {
    const project = await projects.createProject({
      name: form.name,
      projectCode: form.projectCode,
      projectColor: form.projectColor,
      description: form.description,
    })
    form.projectCode = ''
    form.projectColor = '#0ea5e9'
    form.name = ''
    form.description = ''
    await router.push(`/projects/${project.id}`)
  } finally {
    creating.value = false
  }
}
</script>

<template>
  <main class="page-surface">
    <header class="page-header">
      <div>
        <p class="eyebrow">Projects</p>
        <h1>项目</h1>
      </div>
    </header>

    <section class="split-layout">
      <form class="panel-block project-form" aria-label="新建项目表单" @submit.prevent="submit">
        <h2>新建项目</h2>
        <label>
          名称
          <input v-model="form.name" aria-label="项目名称" required maxlength="120" />
        </label>
        <label>
          项目编号
          <input
            v-model="form.projectCode"
            aria-label="项目编号"
            autocomplete="off"
            required
            maxlength="32"
          />
        </label>
        <label>
          项目颜色
          <input v-model="form.projectColor" aria-label="项目颜色" type="color" required />
        </label>
        <label>
          描述
          <textarea v-model="form.description" aria-label="项目描述" rows="4" />
        </label>
        <button class="primary-button" type="submit" :disabled="creating">
          {{ creating ? '创建中' : '创建项目' }}
        </button>
      </form>

      <section class="project-list" aria-label="项目列表">
        <p v-if="projects.loading" class="muted">正在加载项目...</p>
        <p v-else-if="projects.error" class="form-error">{{ projects.error }}</p>
        <RouterLink
          v-for="project in projects.projects"
          :key="project.id"
          class="project-row"
          :to="`/projects/${project.id}`"
        >
          <span>
            <strong>
              <i
                class="project-color-swatch"
                :style="{ backgroundColor: project.projectColor }"
                :aria-label="`项目颜色 ${project.projectColor}`"
              />
              {{ project.name }}
            </strong>
            <small class="project-code">{{ project.projectCode }}</small>
            <small>{{ project.description || '暂无描述' }}</small>
          </span>
          <span class="row-meta">{{ project.owner.nickname }} · {{ project.memberCount }} 人</span>
        </RouterLink>
      </section>
    </section>
  </main>
</template>
