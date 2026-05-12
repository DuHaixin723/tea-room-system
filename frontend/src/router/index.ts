// 路由配置：负责定义页面地址与页面组件之间的对应关系。

import { createRouter, createWebHistory } from 'vue-router'
import { menuItems } from '@/config/menu'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/auth/LoginPortalView.vue'), meta: { public: true } },
    { path: '/login/admin', name: 'login-admin', component: () => import('@/views/auth/AdminLoginView.vue'), meta: { public: true } },
    { path: '/login/client', name: 'login-client', component: () => import('@/views/auth/ClientLoginView.vue'), meta: { public: true } },
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
        { path: 'recommendations', component: () => import('@/views/recommendations/RecommendationsView.vue') },
        { path: 'favorites', component: () => import('@/views/favorites/FavoritesView.vue') },
        { path: 'coupons', component: () => import('@/views/coupons/CouponsView.vue') },
        { path: 'recharge', component: () => import('@/views/recharge/RechargeView.vue') },
        { path: 'settings', component: () => import('@/views/settings/SettingsView.vue') },
        { path: 'profile', component: () => import('@/views/profile/ProfileView.vue') },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.meta.public) {
    if (authStore.isAuthenticated && to.path.startsWith('/login')) {
      return authStore.role === 'ADMIN' ? '/dashboard' : '/profile'
    }
    return true
  }

  if (!authStore.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const currentRole = authStore.role
  const matchedMenus = menuItems.filter((item) => item.path === to.path)
  if (matchedMenus.length && currentRole && !matchedMenus.some((item) => item.roles.includes(currentRole))) {
    return currentRole === 'ADMIN' ? '/dashboard' : '/profile'
  }

  return true
})

export default router
