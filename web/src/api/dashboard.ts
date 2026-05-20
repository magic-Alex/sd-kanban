import type { UserSummary } from './auth'
import { getData } from './http'

export interface DashboardActivity {
  id: number
  taskId: number
  projectId: number
  projectName: string
  taskTitle: string
  actor: UserSummary | null
  actionType: string
  fieldName: string | null
  oldValue: string | null
  newValue: string | null
  createdAt: string
}

export interface DashboardSummary {
  pendingTaskCount: number
  overdueTaskCount: number
  ownedProjectCount: number
  joinedProjectCount: number
  recentActivities: DashboardActivity[]
}

export interface CompletionTrendBucket {
  date: string
  completedCount: number
}

export interface DashboardTrends {
  buckets: CompletionTrendBucket[]
}

export function fetchDashboardSummary(): Promise<DashboardSummary> {
  return getData<DashboardSummary>('/dashboard/summary')
}

export function fetchDashboardTrends(): Promise<DashboardTrends> {
  return getData<DashboardTrends>('/dashboard/trends')
}
