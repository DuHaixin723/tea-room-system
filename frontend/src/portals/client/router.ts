// 入口文件：负责初始化某一端口或某一角色的前端应用。

import { ElMessage } from 'element-plus'
import { createRouter, createWebHistory } from 'vue-router'
import { menuItems } from '@/config/menu'
import { useAuthStore } from '@/stores/auth'
import type { UserRole } from '@/types/auth'

function homeOf(role: UserRole | null | undefined) {
  if (role === 'STAFF') return '/staff'
  return '/home'
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/auth/ClientPortalView.vue'), meta: { public: true } },
    { path: '/login/user', name: 'login-user', component: () => import('@/views/auth/UserLoginView.vue'), meta: { public: true } },
    { path: '/login/staff', name: 'login-staff', component: () => import('@/views/auth/StaffLoginView.vue'), meta: { public: true } },
    { path: '/register/user', name: 'register-user', component: () => import('@/views/auth/UserRegisterView.vue'), meta: { public: true } },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      redirect: '/home',
      children: [
        { path: 'home', component: () => import('@/views/home/UserHomeView.vue') },
        { path: 'staff', component: () => import('@/views/home/StaffWorkbenchView.vue') },
        { path: 'dashboard', redirect: '/profile' },
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
        { path: 'favorites', component: () => import('@/views/favorites/FavoritesView.vue') },
        { path: 'coupons', component: () => import('@/views/coupons/CouponsView.vue') },
        { path: 'recharge', component: () => import('@/views/recharge/RechargeView.vue') },
        { path: 'profile', component: () => import('@/views/profile/ProfileView.vue') },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.meta.public) {
    if (authStore.isAuthenticated && (to.path.startsWith('/login') || to.path.startsWith('/register'))) return homeOf(authStore.role)
    return true
  }

  if (!authStore.isAuthenticated) return { path: '/login', query: { redirect: to.fullPath } }

  if (authStore.role === 'ADMIN') {
    authStore.logout()
    ElMessage.warning('当前是客户端入口，请使用普通用户或茶室员账号登录。')
    return '/login'
  }

  const currentRole = authStore.role
  const matchedMenus = menuItems.filter((item) => item.path === to.path)
  if (matchedMenus.length && currentRole && !matchedMenus.some((item) => item.roles.includes(currentRole))) return homeOf(currentRole)

  if (to.path === '/dashboard') return '/profile'
  if (to.path === '/home' && currentRole === 'STAFF') return '/staff'

  return true
})

export default router
