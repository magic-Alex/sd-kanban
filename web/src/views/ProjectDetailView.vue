<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import ProjectMemberManager from '../components/project/ProjectMemberManager.vue'
import { useAuthStore } from '../stores/auth'
import { useProjectsStore } from '../stores/projects'

const route = useRoute()
const auth = useAuthStore()
const projects = useProjectsStore()
const projectId = computed(() => String(route.params.projectId))
const ownerId = computed(() => projects.currentProject?.owner.id ?? 0)
const currentUserId = computed(() => auth.user?.id ?? null)

onMounted(() => {
  void loadProjectContext()
})

async function loadProjectContext() {
  await Promise.allSettled([
    projects.fetchProject(projectId.value),
    projects.fetchMembers(projectId.value),
  ])
}

async function addMember(userId: number) {
  if (projects.memberActionLoading) {
    return
  }
  await projects.addMember(projectId.value, userId)
  await projects.fetchMembers(projectId.value)
}

async function removeMember(userId: number) {
  if (projects.memberActionLoading) {
    return
  }
  await projects.removeMember(projectId.value, userId)
  await projects.fetchMembers(projectId.value)
}
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
        <div class="project-metadata">
          <span class="project-code">{{ projects.currentProject.projectCode }}</span>
          <span class="metadata-swatch">
            <i
              class="project-color-swatch"
              :style="{ backgroundColor: projects.currentProject.projectColor }"
              aria-hidden="true"
            />
            {{ projects.currentProject.projectColor }}
          </span>
        </div>
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
      <ProjectMemberManager
        class="detail-members"
        :members="projects.members"
        :owner-id="ownerId"
        :current-user-id="currentUserId"
        :loading="projects.membersLoading"
        :action-loading="projects.memberActionLoading"
        :error="projects.memberActionError"
        @add-member="addMember"
        @remove-member="removeMember"
      />
    </section>
  </main>
</template>
