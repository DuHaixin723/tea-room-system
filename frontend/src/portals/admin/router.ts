// 入口文件：负责初始化某一端口或某一角色的前端应用。

import { ElMessage } from 'element-plus'
import { createRouter, createWebHistory } from 'vue-router'
import { menuItems } from '@/config/menu'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/auth/AdminLoginView.vue'), meta: { public: true } },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue') },
        { path: 'users', component: () => import('@/views/users/UsersView.vue') },
        { path: 'tea-rooms', component: () => import('@/views/teaRooms/TeaRoomsView.vue') },
        { path: 'teas', component: () => import('@/views/teas/TeasView.vue') },
        { path: 'reservations', component: () => import('@/views/reservations/ReservationsView.vue') },
        { path: 'reviews', component: () => import('@/views/reviews/ReviewsView.vue') },
        { path: 'reports', component: () => import('@/views/reports/ReportsView.vue') },
        { path: 'activities', component: () => import('@/views/activities/ActivitiesView.vue') },
        { path: 'orders', component: () => import('@/views/orders/OrdersView.vue') },
        { path: 'consultations', component: () => import('@/views/consultations/ConsultationsView.vue') },
        { path: 'complaints', component: () => import('@/views/complaints/ComplaintsView.vue') },
        { path: 'recommendations', component: () => import('@/views/recommendations/RecommendationsView.vue') },
        { path: 'coupons', component: () => import('@/views/coupons/CouponsView.vue') },
        { path: 'settings', component: () => import('@/views/settings/SettingsView.vue') },
        { path: 'profile', component: () => import('@/views/profile/ProfileView.vue') },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.meta.public) {
    if (authStore.isAuthenticated && to.path === '/login') return '/dashboard'
    return true
  }

  if (!authStore.isAuthenticated) return { path: '/login', query: { redirect: to.fullPath } }

  if (authStore.role !== 'ADMIN') {
    authStore.logout()
    ElMessage.warning('当前为管理员端入口，请使用管理员账号登录。')
    return '/login'
  }

  const currentRole = authStore.role
  const matchedMenus = menuItems.filter((item) => item.path === to.path)
  if (matchedMenus.length && currentRole && !matchedMenus.some((item) => item.roles.includes(currentRole))) return '/profile'

  return true
})

export default router
