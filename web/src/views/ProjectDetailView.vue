<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useProjectsStore } from '../stores/projects'

const route = useRoute()
const projects = useProjectsStore()
const projectId = computed(() => String(route.params.projectId))

onMounted(() => {
  projects.fetchProject(projectId.value)
})
</script>

<template>
  <main class="page-surface">
    <header class="page-header">
      <div>
        <p class="eyebrow">Project</p>
        <h1>{{ projects.currentProject?.name ?? '项目详情' }}</h1>
      </div>
      <RouterLink class="primary-link" :to="`/projects/${projectId}/board`">进入看板</RouterLink>
    </header>

    <p v-if="projects.loading" class="muted">正在加载项目...</p>
    <p v-else-if="projects.error" class="form-error">{{ projects.error }}</p>

    <section v-if="projects.currentProject" class="detail-grid">
      <article class="panel-block">
        <h2>项目概览</h2>
        <p>{{ projects.currentProject.description || '暂无描述' }}</p>
      </article>
      <article class="panel-block">
        <h2>项目负责人</h2>
        <p>{{ projects.currentProject.owner.nickname }}</p>
        <small>{{ projects.currentProject.owner.email }}</small>
      </article>
      <article class="panel-block">
        <h2>成员数量</h2>
        <p class="large-number">{{ projects.currentProject.memberCount }}</p>
      </article>
    </section>
  </main>
</template>
