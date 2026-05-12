<script setup lang="ts">
// 布局组件：负责统一页面外层结构与公共区域。

import { computed, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { useRoute, useRouter } from 'vue-router'
import * as Icons from '@element-plus/icons-vue'
import AiAssistantWidget from '@/components/shared/AiAssistantWidget.vue'
import NotificationCenter from '@/components/shared/NotificationCenter.vue'
import { reservationApi } from '@/api/modules/management'
import { suppressHttpErrorsFor } from '@/api/http'
import { menuItems } from '@/config/menu'
import { useAuthStore } from '@/stores/auth'
import { useConsultationStore } from '@/stores/consultation'
import { useMemberStore } from '@/stores/member'
import { useNotificationStore } from '@/stores/notification'
import type { ReservationRecord } from '@/types/business'
import { resolveAssetUrl } from '@/utils/asset'
import { useSystemBranding } from '@/composables/useSystemBranding'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const consultationStore = useConsultationStore()
const memberStore = useMemberStore()
const notificationStore = useNotificationStore()
const reservations = ref<ReservationRecord[]>([])
const { siteName, loadBranding } = useSystemBranding()

const menus = computed(() => menuItems.filter((item) => authStore.role && item.roles.includes(authStore.role)))
const isUser = computed(() => authStore.role === 'USER')
const showAssistant = computed(() => authStore.role === 'USER' || authStore.role === 'STAFF')
const activeReservationStatuses = ['PENDING', 'CONFIRMED', 'USER_CHECKED_IN', 'STAFF_CONFIRMED_CHECK_IN', 'CHARGING']
const memberLevelLabel = computed(() => {
  switch (memberStore.account?.memberLevel) {
    case 'SILVER':
      return '白银会员'
    case 'GOLD':
      return '黄金会员'
    case 'PLATINUM':
      return '铂金会员'
    case 'DIAMOND':
      return '钻石会员'
    default:
      return '普通会员'
  }
})

const activeReservations = computed(() =>
  reservations.value.filter((item) => activeReservationStatuses.includes(item.status)),
)

const nextReservation = computed(() =>
  [...activeReservations.value].sort((a, b) => dayjs(a.reservedStartTime).valueOf() - dayjs(b.reservedStartTime).valueOf())[0] ?? null,
)

const reservationShortcutTitle = computed(() => {
  if (!activeReservations.value.length) return '暂无预约'
  return `${activeReservations.value.length} 条预约进行中`
})

const reservationShortcutSubtitle = computed(() => {
  if (!nextReservation.value) return '去预约中心查看'
  return `最近 ${dayjs(nextReservation.value.reservedStartTime).format('MM-DD HH:mm')}`
})

const themeClass = computed(() => {
  if (authStore.role === 'ADMIN') return 'theme-admin'
  if (authStore.role === 'STAFF') return 'theme-staff'
  return 'theme-user'
})

const roleLabel = computed(() => {
  if (authStore.role === 'ADMIN') return '管理员控制台'
  if (authStore.role === 'STAFF') return '茶室员工作台'
  return '普通用户界面'
})

const userPrimaryMenus = computed(() => {
  const preferred = ['/home', '/tea-rooms', '/teas', '/activities', '/orders', '/favorites', '/consultations']
  return preferred
    .map((path) => menus.value.find((item) => item.path === path))
    .filter((item): item is NonNullable<typeof item> => Boolean(item))
})

const userDockMenus = computed(() => {
  const preferred = ['/home', '/tea-rooms', '/teas', '/orders', '/profile']
  return preferred
    .map((path) => menus.value.find((item) => item.path === path))
    .filter((item): item is NonNullable<typeof item> => Boolean(item))
})

function iconOf(name: string) {
  return Icons[name as keyof typeof Icons]
}

function hasUnread(itemPath: string) {
  return itemPath === '/consultations' && consultationStore.available && consultationStore.unreadCount > 0
}

function userInitial(name?: string) {
  return (name || authStore.user?.username || 'U').slice(0, 1).toUpperCase()
}

function avatarUrl(url?: string) {
  return resolveAssetUrl(url)
}

function formatCurrency(value?: number | null) {
  return Number(value ?? 0).toFixed(2)
}

async function loadMemberAccount() {
  await memberStore.refreshAccount()
}

async function loadReservations() {
  if (authStore.role !== 'USER' || !authStore.user?.id) {
    reservations.value = []
    return
  }
  const page = await reservationApi.page({ page: 0, size: 20, userId: authStore.user.id })
  reservations.value = page.content ?? []
}

function logout() {
  suppressHttpErrorsFor(2000)
  notificationStore.reset()
  consultationStore.reset()
  memberStore.reset()
  reservations.value = []
  authStore.logout()
  router.push('/login')
}

onMounted(async () => {
  await loadBranding().catch(() => undefined)
  if (authStore.isAuthenticated) {
    await Promise.all([
      consultationStore.init().catch(() => undefined),
      loadMemberAccount().catch(() => undefined),
      loadReservations().catch(() => undefined),
    ])
  }
})

watch(
  () => [authStore.isAuthenticated, authStore.user?.id, authStore.role] as const,
  async ([isAuthenticated]) => {
    if (!isAuthenticated) {
      consultationStore.reset()
      memberStore.reset()
      reservations.value = []
      return
    }
    await Promise.all([
      consultationStore.init().catch(() => undefined),
      loadMemberAccount().catch(() => undefined),
      loadReservations().catch(() => undefined),
    ])
  },
)
</script>

<template>
  <div class="app-layout" :class="themeClass">
    <template v-if="!isUser">
      <div class="app-layout__glow app-layout__glow--one"></div>
      <div class="app-layout__glow app-layout__glow--two"></div>

      <aside class="app-sidebar">
        <div class="brand-panel">
          <div class="brand-mark">TEA</div>
          <div>
            <div class="brand-title">{{ siteName }}</div>
            <div class="brand-subtitle">Tea Room Platform</div>
          </div>
        </div>

        <el-scrollbar class="sidebar-scroll">
          <div class="menu-list">
            <RouterLink
              v-for="item in menus"
              :key="item.path"
              :to="item.path"
              class="menu-link"
              :class="{ 'is-active': route.path === item.path }"
            >
              <component :is="iconOf(item.icon)" class="menu-icon" />
              <span>{{ item.title }}</span>
              <span v-if="hasUnread(item.path)" class="menu-dot">
                {{ consultationStore.unreadCount > 99 ? '99+' : consultationStore.unreadCount }}
              </span>
            </RouterLink>
          </div>
        </el-scrollbar>

        <div class="sidebar-footer">
          <div class="user-card">
            <div class="user-avatar">
              <img v-if="authStore.user?.avatarUrl" :src="avatarUrl(authStore.user.avatarUrl)" alt="avatar" />
              <span v-else>{{ userInitial(authStore.user?.nickname) }}</span>
            </div>
            <div>
              <div class="user-name">{{ authStore.user?.nickname }}</div>
              <div class="user-role">{{ authStore.user?.role }}</div>
            </div>
          </div>
        </div>
      </aside>

      <div class="app-main">
        <main class="app-content">
          <div class="app-toolbar">
            <div class="app-toolbar__title">
              <span class="role-badge">{{ roleLabel }}</span>
            </div>
            <div class="app-toolbar__actions">
              <NotificationCenter />
              <el-button text @click="router.push('/profile')">个人中心</el-button>
              <el-button type="primary" @click="logout">退出登录</el-button>
            </div>
          </div>
          <router-view />
        </main>
      </div>
    </template>

    <template v-else>
      <div class="user-shell">
        <header class="user-topbar">
          <div class="user-brand" @click="router.push('/home')">
            <div class="user-brand__mark">茶</div>
            <div>
              <div class="user-brand__title">茶室服务</div>
              <div class="user-brand__sub">Tea Service</div>
            </div>
          </div>

          <nav class="user-nav">
            <RouterLink
              v-for="item in userPrimaryMenus"
              :key="item.path"
              :to="item.path"
              class="user-nav__link"
              :class="{ 'is-active': route.path === item.path, 'is-home': item.path === '/home' }"
            >
              <component :is="iconOf(item.icon)" class="user-nav__icon" />
              <span>{{ item.title }}</span>
              <span v-if="hasUnread(item.path)" class="user-nav__dot">
                {{ consultationStore.unreadCount > 99 ? '99+' : consultationStore.unreadCount }}
              </span>
            </RouterLink>
          </nav>

          <div class="user-actions">
            <NotificationCenter />
            <button type="button" class="reservation-chip" @click="router.push('/reservations')">
              <div class="reservation-chip__icon">
                <component :is="iconOf('Calendar')" />
              </div>
              <div class="reservation-chip__text">
                <strong>{{ reservationShortcutTitle }}</strong>
                <span>{{ reservationShortcutSubtitle }}</span>
              </div>
            </button>
            <button type="button" class="wallet-chip" @click="router.push('/recharge')">
              <div class="wallet-chip__icon">
                <component :is="iconOf('CreditCard')" />
              </div>
              <div class="wallet-chip__text">
                <strong>¥{{ formatCurrency(memberStore.account?.balance) }}</strong>
                <span>{{ memberLevelLabel }} · 去充值</span>
              </div>
            </button>
            <button type="button" class="user-profile" @click="router.push('/profile')">
              <div class="user-profile__avatar">
                <img v-if="authStore.user?.avatarUrl" :src="avatarUrl(authStore.user.avatarUrl)" alt="avatar" />
                <span v-else>{{ userInitial(authStore.user?.nickname) }}</span>
              </div>
              <div class="user-profile__text">
                <strong>{{ authStore.user?.nickname }}</strong>
                <span>{{ roleLabel }}</span>
              </div>
            </button>
            <el-button class="user-logout" @click="logout">退出</el-button>
          </div>
        </header>

        <main class="user-content">
          <router-view />
        </main>

        <nav class="user-dock">
          <RouterLink
            v-for="item in userDockMenus"
            :key="item.path"
            :to="item.path"
            class="user-dock__item"
            :class="{ 'is-active': route.path === item.path }"
          >
            <component :is="iconOf(item.icon)" class="user-dock__icon" />
            <span>{{ item.title }}</span>
          </RouterLink>
        </nav>
      </div>
    </template>

    <AiAssistantWidget v-if="showAssistant" />
  </div>
</template>

<style scoped>
.app-layout {
  --sidebar-bg: linear-gradient(180deg, rgba(64, 43, 22, 0.96), rgba(49, 33, 18, 0.98));
  --sidebar-overlay: linear-gradient(180deg, rgba(215, 162, 85, 0.08), transparent);
  --sidebar-text: #f4eee3;
  --sidebar-muted: rgba(244, 238, 227, 0.7);
  --menu-text: rgba(244, 238, 227, 0.82);
  --menu-hover-border: rgba(215, 162, 85, 0.16);
  --menu-hover-bg: linear-gradient(135deg, rgba(215, 162, 85, 0.16), rgba(255, 255, 255, 0.04));
  --menu-radius: 16px;
  --user-card-bg: rgba(255, 255, 255, 0.04);
  --user-card-border: rgba(255, 255, 255, 0.08);
  --sidebar-border: var(--line);
  --avatar-bg: rgba(215, 162, 85, 0.18);
  --avatar-text: #ffd9a2;
  --brand-mark-start: #d8a457;
  --brand-mark-end: #94632f;
  --brand-mark-text: #231608;
  --surface-bg:
    radial-gradient(circle at top left, rgba(214, 178, 129, 0.28), transparent 28%),
    linear-gradient(135deg, #f1ede4, #f8f5ef 60%, #efe7da);
  --glow-one: rgba(194, 140, 68, 0.24);
  --glow-two: rgba(123, 88, 50, 0.12);
  display: flex;
  min-height: 100vh;
  background: var(--surface-bg);
}

.app-layout__glow {
  position: fixed;
  z-index: 0;
  pointer-events: none;
  filter: blur(18px);
  opacity: 0.8;
}

.app-layout__glow--one {
  top: -120px;
  right: -40px;
  width: 340px;
  height: 340px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--glow-one), transparent 68%);
}

.app-layout__glow--two {
  bottom: -120px;
  left: 22%;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(circle, var(--glow-two), transparent 70%);
}

.app-sidebar {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: var(--sidebar-width);
  min-height: 100vh;
  padding: 22px 16px;
  border-right: 1px solid var(--sidebar-border);
  background: var(--sidebar-bg), var(--sidebar-overlay);
  color: var(--sidebar-text);
  box-shadow: 24px 0 60px rgba(31, 22, 13, 0.14);
}

.brand-panel,
.user-card,
.app-toolbar,
.app-toolbar__actions,
.user-topbar,
.user-actions,
.user-brand,
.user-profile,
.user-nav {
  display: flex;
  align-items: center;
}

.brand-panel {
  gap: 14px;
  padding: 10px 10px 18px;
}

.brand-mark,
.user-brand__mark {
  display: grid;
  place-items: center;
}

.brand-mark {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  background: linear-gradient(145deg, var(--brand-mark-start), var(--brand-mark-end));
  color: var(--brand-mark-text);
  font-weight: 800;
  letter-spacing: 0.12em;
}

.brand-title {
  font-size: 20px;
  font-weight: 700;
}

.brand-subtitle {
  margin-top: 4px;
  color: var(--sidebar-muted);
  font-size: 13px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.sidebar-scroll {
  flex: 1;
  min-height: 0;
}

.menu-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 6px;
}

.menu-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 16px;
  border: 1px solid transparent;
  border-radius: var(--menu-radius);
  color: var(--menu-text);
  font-size: 17px;
  font-weight: 600;
  transition: transform 0.2s ease, border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.menu-link:hover,
.menu-link.is-active {
  color: var(--menu-active-text, #fffdf6);
  border-color: var(--menu-hover-border);
  background: var(--menu-hover-bg);
  transform: translateX(2px);
}

.menu-icon,
.user-nav__icon,
.user-dock__icon {
  width: 20px;
  height: 20px;
}

.menu-dot,
.user-nav__dot {
  margin-left: auto;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: #ef4444;
  color: #fff;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
  font-weight: 700;
}

.sidebar-footer {
  padding: 14px 8px 0;
}

.user-card {
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--user-card-border);
  border-radius: 18px;
  background: var(--user-card-bg);
}

.user-avatar,
.user-profile__avatar {
  display: grid;
  place-items: center;
  overflow: hidden;
  width: 46px;
  height: 46px;
  border-radius: 14px;
  background: var(--avatar-bg);
  color: var(--avatar-text);
  font-size: 18px;
  font-weight: 700;
}

.user-avatar img,
.user-profile__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-name {
  font-size: 17px;
  font-weight: 700;
}

.user-role {
  margin-top: 4px;
  color: var(--sidebar-muted);
  font-size: 13px;
}

.app-main {
  flex: 1;
  position: relative;
  z-index: 1;
  min-width: 0;
  padding: 18px 20px 28px;
}

.app-toolbar {
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 8px;
}

.app-toolbar__title {
  margin-right: auto;
}

.role-badge {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
  padding: 0 16px;
  border: 1px solid var(--role-badge-border);
  border-radius: 999px;
  background: var(--role-badge-bg);
  box-shadow: 0 10px 24px var(--role-badge-shadow);
  color: var(--role-badge-text);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.app-toolbar__actions {
  gap: 10px;
}

.app-content {
  padding-top: 0;
}

.user-shell {
  width: 100%;
  min-height: 100vh;
  padding: 18px 18px 96px;
}

.user-topbar {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px 20px;
  padding: clamp(14px, 1.2vw, 18px) clamp(16px, 1.4vw, 22px);
  border: 1px solid rgba(27, 92, 72, 0.12);
  border-radius: 28px;
  background: rgba(255, 252, 246, 0.78);
  backdrop-filter: blur(16px);
  box-shadow: 0 20px 46px rgba(26, 62, 49, 0.08);
}

.user-brand {
  gap: 14px;
  cursor: pointer;
  min-width: 0;
}

.user-brand__mark {
  width: clamp(50px, 3.6vw, 58px);
  height: clamp(50px, 3.6vw, 58px);
  border-radius: 20px;
  background: linear-gradient(145deg, #245f4a, #9dcfb0);
  color: #fff9f0;
  font-size: clamp(20px, 1.6vw, 24px);
  font-weight: 800;
  flex: 0 0 auto;
}

.user-brand__title {
  font-size: clamp(18px, 1.4vw, 22px);
  font-weight: 800;
  color: #1b4d3d;
  white-space: nowrap;
}

.user-brand__sub {
  margin-top: 4px;
  color: #5f7d71;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  white-space: nowrap;
}

.user-nav {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
  min-width: 0;
  align-self: stretch;
}

.user-nav__link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid transparent;
  border-radius: 999px;
  color: #2a5a4c;
  background: rgba(255, 255, 255, 0.55);
  font-weight: 600;
  white-space: nowrap;
  transition: background 140ms ease, border-color 140ms ease, box-shadow 140ms ease, transform 140ms ease;
}

.user-nav__link.is-home {
  background: linear-gradient(135deg, rgba(31, 90, 71, 0.1), rgba(255, 245, 220, 0.92));
  border-color: rgba(31, 90, 71, 0.14);
  color: #1b4b3c;
}

.user-nav__link:hover,
.user-nav__link.is-active {
  color: #173e33;
  border-color: rgba(46, 117, 94, 0.18);
  background: linear-gradient(135deg, rgba(181, 230, 211, 0.55), rgba(255, 255, 255, 0.96));
  box-shadow: 0 10px 24px rgba(27, 92, 72, 0.08);
}

.user-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  min-width: 0;
}

.reservation-chip,
.wallet-chip {
  appearance: none;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid rgba(27, 92, 72, 0.12);
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(255, 247, 231, 0.96), rgba(241, 250, 245, 0.92)),
    rgba(255, 255, 255, 0.88);
  cursor: pointer;
  box-shadow: 0 14px 32px rgba(27, 92, 72, 0.08);
  transition: transform 140ms ease, box-shadow 140ms ease, border-color 140ms ease;
  min-width: 0;
}

.reservation-chip:hover,
.wallet-chip:hover {
  transform: translateY(-1px);
  border-color: rgba(27, 92, 72, 0.22);
  box-shadow: 0 18px 36px rgba(27, 92, 72, 0.12);
}

.reservation-chip__icon,
.wallet-chip__icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  background: linear-gradient(145deg, #245f4a, #9dcfb0);
  color: #fff9f0;
}

.reservation-chip__icon :deep(svg),
.wallet-chip__icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.reservation-chip__text,
.wallet-chip__text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
}

.reservation-chip__text strong,
.wallet-chip__text strong {
  color: #1d4538;
  font-size: 16px;
  line-height: 1.1;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.reservation-chip__text span,
.wallet-chip__text span {
  margin-top: 4px;
  color: #678173;
  font-size: 12px;
  line-height: 1.2;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-profile {
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid rgba(27, 92, 72, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  min-width: 0;
}

.user-profile__text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-profile__text strong {
  color: #1d4538;
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-profile__text span {
  margin-top: 4px;
  color: #678173;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-logout {
  border-radius: 14px;
}

.user-content {
  margin-top: 12px;
}

.user-dock {
  position: fixed;
  right: 18px;
  bottom: 18px;
  left: 18px;
  z-index: 20;
  display: none;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  padding: 10px;
  border: 1px solid rgba(27, 92, 72, 0.14);
  border-radius: 24px;
  background: rgba(255, 252, 247, 0.92);
  backdrop-filter: blur(16px);
  box-shadow: 0 20px 44px rgba(27, 92, 72, 0.12);
}

.user-dock__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 6px;
  border-radius: 16px;
  color: #56786b;
  font-size: 12px;
  font-weight: 700;
}

.user-dock__item.is-active {
  color: #1d4f3f;
  background: linear-gradient(135deg, rgba(179, 227, 208, 0.58), rgba(255, 255, 255, 0.92));
}

.theme-admin {
  --sidebar-bg:
    linear-gradient(180deg, rgba(34, 28, 24, 0.97), rgba(18, 16, 15, 0.99)),
    linear-gradient(180deg, rgba(193, 141, 71, 0.08), transparent);
  --sidebar-overlay: linear-gradient(180deg, rgba(193, 141, 71, 0.1), transparent 42%);
  --sidebar-text: #f3ede3;
  --sidebar-muted: rgba(243, 237, 227, 0.64);
  --menu-text: rgba(243, 237, 227, 0.8);
  --menu-active-text: #fffdf6;
  --menu-hover-border: rgba(214, 164, 96, 0.22);
  --menu-hover-bg: linear-gradient(135deg, rgba(214, 164, 96, 0.18), rgba(255, 255, 255, 0.05));
  --user-card-bg: linear-gradient(135deg, rgba(255, 255, 255, 0.06), rgba(214, 164, 96, 0.08));
  --user-card-border: rgba(214, 164, 96, 0.12);
  --avatar-bg: linear-gradient(145deg, rgba(214, 164, 96, 0.22), rgba(103, 68, 34, 0.24));
  --avatar-text: #ffdfb4;
  --role-badge-bg: linear-gradient(135deg, rgba(223, 177, 110, 0.18), rgba(131, 82, 44, 0.08));
  --role-badge-border: rgba(131, 82, 44, 0.22);
  --role-badge-shadow: rgba(131, 82, 44, 0.12);
  --role-badge-text: #6f4a28;
  --brand-mark-start: #dfb16e;
  --brand-mark-end: #83522c;
  --brand-mark-text: #241509;
  --surface-bg: linear-gradient(140deg, #ede4d6 0%, #f7f2ea 44%, #e8dccd 100%);
  --glow-one: rgba(217, 165, 93, 0.24);
  --glow-two: rgba(91, 62, 33, 0.16);
}

.theme-staff {
  --sidebar-bg:
    linear-gradient(180deg, rgba(17, 64, 54, 0.96), rgba(10, 45, 38, 0.98)),
    linear-gradient(180deg, rgba(164, 211, 166, 0.12), transparent);
  --sidebar-overlay: linear-gradient(180deg, rgba(160, 221, 173, 0.12), transparent 45%);
  --sidebar-text: #edf8f3;
  --sidebar-muted: rgba(237, 248, 243, 0.68);
  --menu-text: rgba(237, 248, 243, 0.84);
  --menu-active-text: #fffdf6;
  --menu-hover-border: rgba(137, 212, 168, 0.24);
  --menu-hover-bg: linear-gradient(135deg, rgba(92, 182, 139, 0.28), rgba(255, 255, 255, 0.03));
  --user-card-bg: linear-gradient(135deg, rgba(255, 255, 255, 0.06), rgba(92, 182, 139, 0.12));
  --user-card-border: rgba(137, 212, 168, 0.16);
  --avatar-bg: linear-gradient(145deg, rgba(140, 219, 176, 0.22), rgba(22, 87, 69, 0.26));
  --avatar-text: #d4ffea;
  --role-badge-bg: linear-gradient(135deg, rgba(140, 219, 176, 0.2), rgba(61, 139, 108, 0.08));
  --role-badge-border: rgba(61, 139, 108, 0.22);
  --role-badge-shadow: rgba(34, 108, 82, 0.12);
  --role-badge-text: #21614b;
  --brand-mark-start: #a7e4be;
  --brand-mark-end: #3d8b6c;
  --brand-mark-text: #103327;
  --surface-bg:
    radial-gradient(circle at top left, rgba(132, 212, 177, 0.22), transparent 26%),
    linear-gradient(140deg, #edf6f1 0%, #f7fbf8 42%, #ddeee6 100%);
  --glow-one: rgba(102, 190, 150, 0.22);
  --glow-two: rgba(34, 108, 82, 0.14);
}

.theme-user {
  --surface-bg:
    radial-gradient(circle at top left, rgba(255, 225, 176, 0.36), transparent 24%),
    radial-gradient(circle at right center, rgba(168, 222, 201, 0.26), transparent 25%),
    linear-gradient(145deg, #f8f6ee 0%, #eef7f2 40%, #f7fbf8 100%);
}

@media (max-width: 1280px) {
  .user-topbar {
    grid-template-columns: minmax(0, 1fr);
  }

  .user-nav {
    width: 100%;
    justify-content: flex-start;
  }

  .user-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 1536px) {
  .user-topbar {
    gap: 14px 16px;
  }

  .user-nav__link {
    padding: 0 12px;
    font-size: 14px;
  }

  .user-actions {
    gap: 10px;
  }

  .reservation-chip,
  .wallet-chip,
  .user-profile {
    padding: 10px 12px;
  }
}

@media (max-width: 1366px) {
  .user-topbar {
    border-radius: 24px;
  }

  .user-nav {
    gap: 8px;
  }

  .user-nav__icon {
    width: 18px;
    height: 18px;
  }

  .user-nav__link {
    min-height: 42px;
    padding: 0 11px;
    font-size: 13px;
  }

  .reservation-chip__icon,
  .wallet-chip__icon {
    width: 36px;
    height: 36px;
    border-radius: 12px;
  }

  .reservation-chip__text strong,
  .wallet-chip__text strong {
    font-size: 15px;
  }

  .reservation-chip__text span,
  .wallet-chip__text span,
  .user-profile__text span {
    font-size: 11px;
  }
}

@media (max-width: 1024px) {
  .app-layout {
    flex-direction: column;
  }

  .app-layout__glow {
    display: none;
  }

  .app-sidebar {
    position: static;
    width: auto;
    min-height: auto;
  }

  .app-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .user-shell {
    padding-right: 12px;
    padding-left: 12px;
  }

  .user-topbar {
    padding: 14px;
    gap: 12px;
    border-radius: 22px;
  }

  .user-brand {
    gap: 12px;
  }

  .user-brand__sub {
    display: none;
  }

  .user-actions {
    width: 100%;
    justify-content: space-between;
  }

  .reservation-chip,
  .wallet-chip {
    flex: 1;
    min-width: 0;
  }

  .user-profile {
    flex: 1;
  }

  .user-profile__text span,
  .reservation-chip__text span,
  .wallet-chip__text span {
    display: none;
  }

  .user-dock {
    display: grid;
  }
}

@media (max-width: 560px) {
  .user-shell {
    padding-bottom: 104px;
  }

  .user-nav {
    display: none;
  }

  .user-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
  }

  .user-actions :deep(.notification-center) {
    grid-column: 1 / -1;
  }

  .reservation-chip,
  .wallet-chip,
  .user-profile,
  .user-logout {
    width: 100%;
  }

  .user-profile {
    order: 3;
  }

  .user-logout {
    order: 4;
  }
}
</style>
