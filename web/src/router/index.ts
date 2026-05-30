import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '../views/DashboardView.vue'
import BoardTemplateSettingsView from '../views/BoardTemplateSettingsView.vue'
import LoginView from '../views/LoginView.vue'
import MyTaskBoardView from '../views/MyTaskBoardView.vue'
import ProjectBoardView from '../views/ProjectBoardView.vue'
import ProjectDetailView from '../views/ProjectDetailView.vue'
import ProjectListView from '../views/ProjectListView.vue'
import UserAdminView from '../views/UserAdminView.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: DashboardView,
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true },
    },
    {
      path: '/projects',
      name: 'projects',
      component: ProjectListView,
      meta: { requiresAuth: true },
    },
    {
      path: '/projects/:projectId',
      name: 'project-detail',
      component: ProjectDetailView,
      meta: { requiresAuth: true },
    },
    {
      path: '/projects/:projectId/board',
      name: 'project-board',
      component: ProjectBoardView,
      meta: { requiresAuth: true },
    },
    {
      path: '/my-tasks',
      name: 'my-tasks',
      component: MyTaskBoardView,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: UserAdminView,
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/settings/board-template',
      name: 'admin-board-template-settings',
      component: BoardTemplateSettingsView,
      meta: { requiresAuth: true, requiresAdmin: true },
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'dashboard' }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
