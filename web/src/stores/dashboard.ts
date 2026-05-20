import { defineStore } from 'pinia'
import {
  fetchDashboardSummary,
  fetchDashboardTrends,
  type DashboardSummary,
  type DashboardTrends,
} from '../api/dashboard'

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    summary: null as DashboardSummary | null,
    trends: null as DashboardTrends | null,
    loading: false,
    error: null as string | null,
  }),
  actions: {
    async fetchAll() {
      this.loading = true
      this.error = null
      try {
        const [summary, trends] = await Promise.all([
          fetchDashboardSummary(),
          fetchDashboardTrends(),
        ])
        this.summary = summary
        this.trends = trends
      } catch (error) {
        this.error = '仪表盘数据加载失败'
        throw error
      } finally {
        this.loading = false
      }
    },
  },
})
