<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { Calendar, ChatDotRound, OfficeBuilding, Tools } from '@element-plus/icons-vue'
import { ElMessage, ElNotification } from 'element-plus'
import { reportApi, reservationApi, teaRoomApi } from '@/api/modules/management'
import { useAuthStore } from '@/stores/auth'
import { getToken } from '@/utils/storage'
import type { ReportRecord, ReservationRecord, TeaRoomRecord } from '@/types/business'
import { formatStatus } from '@/utils/status'

interface StaffNotification {
  type: string
  targetId: number
  title: string
  content: string
  status: string
  createdAt: string
}

const authStore = useAuthStore()

const loading = ref(true)
const myRooms = ref<TeaRoomRecord[]>([])
const recentReservations = ref<ReservationRecord[]>([])
const recentReports = ref<ReportRecord[]>([])
const connected = ref(false)
const client = ref<Client | null>(null)

const nickname = computed(() => authStore.user?.nickname || authStore.user?.username || '茶室员')
const activeRoomCount = computed(() => myRooms.value.filter((item) => item.enabled).length)
const pendingReservationCount = computed(() => recentReservations.value.filter((item) => ['PENDING', 'CONFIRMED'].includes(item.status)).length)
const pendingReportCount = computed(() => recentReports.value.filter((item) => item.status !== 'RESOLVED').length)

function websocketUrl() {
  if (typeof window === 'undefined') return '/ws-consultation'
  const token = getToken()
  const qs = token ? `?token=${encodeURIComponent(token)}` : ''
  return `${window.location.origin}/ws-consultation${qs}`
}

function reservationTagType(status: string) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'STAFF_CONFIRMED' || status === 'CONFIRMED') return 'warning'
  if (status === 'CANCELLED') return 'info'
  return 'primary'
}

function reportTagType(status: string) {
  if (status === 'RESOLVED') return 'success'
  if (status === 'PROCESSING') return 'warning'
  return 'danger'
}

function roomStatusType(enabled: boolean) {
  return enabled ? 'success' : 'info'
}

async function load() {
  loading.value = true
  try {
    const [roomPage, reservationPage, reportPage] = await Promise.all([
      teaRoomApi.rooms({ page: 0, size: 10 }),
      reservationApi.page({ page: 0, size: 8 }),
      reportApi.page({ page: 0, size: 8 }),
    ])

    myRooms.value = roomPage.content ?? []
    recentReservations.value = reservationPage.content ?? []
    recentReports.value = reportPage.content ?? []
  } catch (error) {
    console.error(error)
    ElMessage.error('加载工作台失败')
  } finally {
    loading.value = false
  }
}

function connectNotificationSocket() {
  if (!authStore.user?.id || client.value) return

  const stomp = new Client({
    webSocketFactory: () => new SockJS(websocketUrl()),
    reconnectDelay: 3000,
    onConnect: () => {
      connected.value = true
      stomp.subscribe(`/topic/staff-notice/${authStore.user?.id}`, async (frame) => {
        const payload = JSON.parse(frame.body) as StaffNotification
        ElNotification({
          title: payload.title,
          message: payload.content,
          type: 'info',
          duration: 6000,
        })
        await load()
      })
    },
    onStompError: () => {
      connected.value = false
    },
    onWebSocketClose: () => {
      connected.value = false
    },
  })

  stomp.activate()
  client.value = stomp
}

onMounted(async () => {
  await load()
  connectNotificationSocket()
})

onBeforeUnmount(() => {
  client.value?.deactivate()
  client.value = null
})
</script>

<template>
  <div class="workbench">
    <section class="hero section-card">
      <div class="hero__content">
        <div class="hero__main">
          <div class="hero__eyebrow">Tea Service Desk</div>
          <h2 class="hero__title">你好，{{ nickname }}</h2>
          <p class="hero__desc">
            这里汇总你负责茶室的预约、报障和服务状态。管理员更新处理进度后，你会在工作台第一时间收到提醒。
          </p>
          <div class="hero__actions">
            <el-tag :type="connected ? 'success' : 'info'" round>
              {{ connected ? '通知已连接' : '通知未连接' }}
            </el-tag>
            <el-button type="primary" @click="$router.push('/reservations')">处理预约</el-button>
            <el-button @click="$router.push('/reports')">查看报障</el-button>
            <el-button @click="$router.push('/reviews')">查看评价</el-button>
          </div>
        </div>

        <div class="hero__metrics">
          <div class="hero-metric">
            <div class="hero-metric__label">负责茶室</div>
            <div class="hero-metric__value">{{ myRooms.length }}</div>
          </div>
          <div class="hero-metric">
            <div class="hero-metric__label">待处理预约</div>
            <div class="hero-metric__value">{{ pendingReservationCount }}</div>
          </div>
          <div class="hero-metric">
            <div class="hero-metric__label">待跟进报障</div>
            <div class="hero-metric__value">{{ pendingReportCount }}</div>
          </div>
        </div>
      </div>
    </section>

    <section class="summary-grid">
      <article class="summary-card section-card">
        <div class="summary-card__icon"><el-icon><OfficeBuilding /></el-icon></div>
        <div>
          <div class="summary-card__label">可用茶室</div>
          <div class="summary-card__value">{{ activeRoomCount }}</div>
          <div class="summary-card__sub">当前已启用的服务空间</div>
        </div>
      </article>

      <article class="summary-card section-card">
        <div class="summary-card__icon"><el-icon><Calendar /></el-icon></div>
        <div>
          <div class="summary-card__label">最近预约</div>
          <div class="summary-card__value">{{ recentReservations.length }}</div>
          <div class="summary-card__sub">优先查看确认中与待接待订单</div>
        </div>
      </article>

      <article class="summary-card section-card">
        <div class="summary-card__icon"><el-icon><Tools /></el-icon></div>
        <div>
          <div class="summary-card__label">报障提醒</div>
          <div class="summary-card__value">{{ pendingReportCount }}</div>
          <div class="summary-card__sub">处理中的设备问题需要持续跟进</div>
        </div>
      </article>

      <article class="summary-card section-card">
        <div class="summary-card__icon"><el-icon><ChatDotRound /></el-icon></div>
        <div>
          <div class="summary-card__label">协同状态</div>
          <div class="summary-card__value">{{ connected ? '在线' : '离线' }}</div>
          <div class="summary-card__sub">与管理员通知通道实时同步</div>
        </div>
      </article>
    </section>

    <div class="panel-grid">
      <section class="section-card">
        <div class="section-card__header">
          <h3 class="section-card__title">我负责的茶室</h3>
          <el-button text @click="$router.push('/tea-rooms')">管理</el-button>
        </div>
        <div class="section-card__body">
          <el-skeleton v-if="loading" :rows="5" animated />
          <div v-else class="room-list">
            <article v-for="room in myRooms" :key="room.id" class="room-item">
              <div class="room-item__top">
                <div class="room-name">{{ room.name }}</div>
                <el-tag :type="roomStatusType(room.enabled)" effect="plain" round>
                  {{ room.enabled ? '开放中' : '已停用' }}
                </el-tag>
              </div>
              <div class="room-meta">容量 {{ room.capacity }} 人</div>
              <div class="room-desc">{{ room.location || room.description || '暂未配置位置说明' }}</div>
            </article>
            <div v-if="!myRooms.length" class="empty-panel">暂无绑定茶室</div>
          </div>
        </div>
      </section>

      <section class="section-card">
        <div class="section-card__header">
          <h3 class="section-card__title">最近预约</h3>
          <el-button text @click="$router.push('/reservations')">全部</el-button>
        </div>
        <div class="section-card__body">
          <el-table v-loading="loading" :data="recentReservations" class="data-table workbench-table">
            <el-table-column prop="id" label="预约ID" width="96" />
            <el-table-column prop="teaRoomId" label="茶室ID" width="100" />
            <el-table-column prop="reservedStartTime" label="开始时间" min-width="180" />
            <el-table-column label="状态" width="140">
              <template #default="{ row }">
                <el-tag :type="reservationTagType(row.status)" effect="plain" round>
                  {{ formatStatus('reservation', row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>
    </div>

    <section class="section-card">
      <div class="section-card__header">
        <h3 class="section-card__title">设备报障</h3>
        <el-button text @click="$router.push('/reports')">全部</el-button>
      </div>
      <div class="section-card__body">
        <el-table v-loading="loading" :data="recentReports" class="data-table workbench-table">
          <el-table-column prop="id" label="报障ID" width="96" />
          <el-table-column prop="teaRoomId" label="茶室ID" width="100" />
          <el-table-column prop="title" label="问题标题" min-width="220" />
          <el-table-column label="状态" width="140">
            <template #default="{ row }">
              <el-tag :type="reportTagType(row.status)" effect="plain" round>
                {{ formatStatus('report', row.status) }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.workbench {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(115, 198, 155, 0.2), transparent 28%),
    linear-gradient(135deg, rgba(248, 253, 250, 0.96), rgba(236, 247, 241, 0.98));
}

.hero__content {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  gap: 22px;
  padding: 28px;
}

.hero__eyebrow {
  color: var(--success);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.hero__title {
  margin: 10px 0 0;
  font-size: 34px;
  line-height: 1.1;
  letter-spacing: -0.04em;
}

.hero__desc {
  max-width: 680px;
  margin: 12px 0 0;
  color: var(--muted);
  font-size: 15px;
  line-height: 1.75;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-top: 20px;
}

.hero__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hero-metric {
  padding: 18px 18px 16px;
  border: 1px solid rgba(46, 111, 87, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 16px 34px rgba(46, 111, 87, 0.08);
}

.hero-metric__label {
  color: var(--muted);
  font-size: 14px;
}

.hero-metric__value {
  margin-top: 12px;
  color: #1f4135;
  font-size: 32px;
  font-weight: 800;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
}

.summary-card__icon {
  display: grid;
  place-items: center;
  width: 50px;
  height: 50px;
  border-radius: 16px;
  background: linear-gradient(145deg, rgba(107, 193, 156, 0.2), rgba(55, 126, 102, 0.14));
  color: var(--success);
  font-size: 22px;
}

.summary-card__label {
  color: var(--muted);
  font-size: 14px;
}

.summary-card__value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 800;
  line-height: 1.1;
}

.summary-card__sub {
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.panel-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.3fr);
  gap: 16px;
}

.room-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.room-item {
  padding: 16px;
  border: 1px solid rgba(46, 111, 87, 0.12);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(241, 249, 244, 0.94));
}

.room-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.room-name {
  font-size: 18px;
  font-weight: 800;
}

.room-meta {
  margin-top: 10px;
  color: var(--brand);
  font-size: 14px;
  font-weight: 600;
}

.room-desc {
  margin-top: 8px;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.7;
}

.workbench-table :deep(.el-table__row td) {
  padding-top: 14px;
  padding-bottom: 14px;
}

@media (max-width: 1360px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero__content,
  .panel-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero__content {
    padding: 20px;
  }

  .hero__title {
    font-size: 28px;
  }

  .hero__metrics,
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
