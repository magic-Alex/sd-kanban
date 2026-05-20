<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useDashboardStore } from '../stores/dashboard'

const dashboard = useDashboardStore()

onMounted(() => {
  dashboard.fetchAll()
})

const summary = computed(() => dashboard.summary)
const trendTotal = computed(() =>
  dashboard.trends?.buckets.reduce((total, bucket) => total + bucket.completedCount, 0) ?? 0,
)

function activityText(actionType: string) {
  if (actionType === 'TASK_UPDATED') {
    return '更新了任务'
  }
  if (actionType === 'COMMENT_ADDED') {
    return '添加了评论'
  }
  if (actionType === 'TASK_CREATED') {
    return '创建了任务'
  }
  return actionType
}
</script>

<template>
  <main class="page-surface">
    <header class="page-header">
      <div>
        <p class="eyebrow">Dashboard</p>
        <h1>仪表盘</h1>
      </div>
      <RouterLink class="primary-link" to="/projects">查看项目</RouterLink>
    </header>

    <p v-if="dashboard.error" class="form-error">{{ dashboard.error }}</p>
    <p v-else-if="dashboard.loading" class="muted">正在加载仪表盘...</p>

    <section v-if="summary" class="metric-grid" aria-label="关键指标">
      <article class="metric-card">
        <span>待处理任务</span>
        <strong>{{ summary.pendingTaskCount }}</strong>
      </article>
      <article class="metric-card attention">
        <span>逾期任务</span>
        <strong>{{ summary.overdueTaskCount }}</strong>
      </article>
      <article class="metric-card">
        <span>负责项目</span>
        <strong>{{ summary.ownedProjectCount }}</strong>
      </article>
      <article class="metric-card">
        <span>参与项目</span>
        <strong>{{ summary.joinedProjectCount }}</strong>
      </article>
    </section>

    <section class="dashboard-grid">
      <div class="panel-block">
        <div class="section-heading">
          <h2>完成趋势</h2>
          <span>{{ trendTotal }} 项完成</span>
        </div>
        <div class="trend-bars" aria-label="最近完成任务">
          <div
            v-for="bucket in dashboard.trends?.buckets ?? []"
            :key="bucket.date"
            class="trend-bar"
          >
            <span :style="{ height: `${Math.max(8, bucket.completedCount * 18)}px` }"></span>
            <small>{{ bucket.date.slice(5) }}</small>
          </div>
        </div>
      </div>

      <div class="panel-block">
        <div class="section-heading">
          <h2>最近动态</h2>
          <span>{{ summary?.recentActivities.length ?? 0 }} 条</span>
        </div>
        <ul class="activity-list">
          <li v-for="activity in summary?.recentActivities ?? []" :key="activity.id">
            <span>{{ activity.actor?.nickname ?? '系统' }} {{ activityText(activity.actionType) }}</span>
            <strong>{{ activity.taskTitle }}</strong>
            <small>{{ activity.projectName }}</small>
          </li>
        </ul>
      </div>
    </section>
  </main>
</template>
