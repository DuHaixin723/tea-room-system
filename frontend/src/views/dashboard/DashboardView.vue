<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, ref } from 'vue'
import VChart from 'vue-echarts'
import { useAuthStore } from '@/stores/auth'
import { fetchOverview, fetchRecommendations } from '@/api/modules/dashboard'
import PageIntro from '@/components/shared/PageIntro.vue'
import MetricCard from '@/components/shared/MetricCard.vue'
import TrendChart from '@/components/chart/TrendChart.vue'
import { activityApi, favoriteApi, orderApi, reportApi, reservationApi, teaApi, teaRoomApi } from '@/api/modules/management'
import type {
  ActivityRegistrationRecord,
  FavoriteRecord,
  OrderDetailRecord,
  OrderRecord,
  RecommendationRecord,
  ReportRecord,
  ReservationRecord,
  StatisticOverview,
  TeaRecord,
  TeaRoomRecord,
} from '@/types/business'

const authStore = useAuthStore()
const overview = ref<StatisticOverview | null>(null)
const recommendations = ref<RecommendationRecord | null>(null)

const userOrders = ref<OrderRecord[]>([])
const userReservations = ref<ReservationRecord[]>([])
const userFavorites = ref<FavoriteRecord[]>([])
const userRegistrations = ref<ActivityRegistrationRecord[]>([])

const staffReservations = ref<ReservationRecord[]>([])
const staffReports = ref<ReportRecord[]>([])

const adminTeaRooms = ref<TeaRoomRecord[]>([])
const adminTeas = ref<TeaRecord[]>([])
const adminReservations = ref<ReservationRecord[]>([])
const adminOrders = ref<OrderRecord[]>([])
const adminOrderDetails = ref<OrderDetailRecord[]>([])

const userMetrics = computed(() => {
  const pendingOrders = userOrders.value.filter((o) => o.status === 'PENDING_PAYMENT').length
  const activeOrders = userOrders.value.filter((o) => ['PENDING_PAYMENT', 'PAID'].includes(o.status)).length
  const upcomingReservations = userReservations.value.filter((r) => ['PENDING', 'CONFIRMED', 'USER_CHECKED_IN'].includes(r.status)).length
  return {
    pendingOrders,
    activeOrders,
    upcomingReservations,
    favorites: userFavorites.value.length,
    registrations: userRegistrations.value.filter((r) => !r.cancelled).length,
  }
})

const staffMetrics = computed(() => {
  const pendingReservations = staffReservations.value.filter((r) => ['PENDING', 'USER_CHECKED_IN'].includes(r.status)).length
  const activeReports = staffReports.value.filter((r) => ['PENDING', 'PROCESSING'].includes(r.status)).length
  const today = new Date().toISOString().slice(0, 10)
  const todayReservations = staffReservations.value.filter((r) => r.reservedStartTime.startsWith(today)).length
  return { pendingReservations, activeReports, todayReservations }
})

const reservationCountsByRoom = computed(() => {
  const counts = new Map<number, number>()
  for (const reservation of adminReservations.value) {
    counts.set(reservation.teaRoomId, (counts.get(reservation.teaRoomId) ?? 0) + 1)
  }
  return adminTeaRooms.value
    .map((room) => ({
      name: room.name,
      value: counts.get(room.id) ?? 0,
    }))
    .filter((item) => item.value > 0)
    .sort((a, b) => b.value - a.value)
})

const teaPurchaseCounts = computed(() => {
  const counts = new Map<number, number>()
  for (const detail of adminOrderDetails.value) {
    const status = detail.order.status
    if (status === 'PENDING_PAYMENT' || status === 'CANCELLED' || status === 'REFUNDED') continue
    for (const item of detail.items) {
      counts.set(item.teaId, (counts.get(item.teaId) ?? 0) + Number(item.quantity ?? 0))
    }
  }
  return adminTeas.value
    .map((tea) => ({
      name: tea.name,
      value: counts.get(tea.id) ?? 0,
    }))
    .filter((item) => item.value > 0)
    .sort((a, b) => b.value - a.value)
})

const topReservationRooms = computed(() => reservationCountsByRoom.value.slice(0, 8))
const topTeaPurchases = computed(() => teaPurchaseCounts.value.slice(0, 8))
const reservationPieData = computed(() => reservationCountsByRoom.value.slice(0, 6))
const teaPurchasePieData = computed(() => teaPurchaseCounts.value.slice(0, 6))

const reservationBarOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 48, right: 24, top: 28, bottom: 72 },
  xAxis: {
    type: 'category',
    data: topReservationRooms.value.map((item) => item.name),
    axisLabel: { rotate: 18, color: '#6f6658' },
    axisLine: { lineStyle: { color: '#cdbda8' } },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(88,72,53,0.08)' } },
  },
  series: [
    {
      type: 'bar',
      data: topReservationRooms.value.map((item) => item.value),
      barMaxWidth: 42,
      itemStyle: {
        borderRadius: [10, 10, 0, 0],
        color: '#7b5832',
      },
    },
  ],
}))

const reservationPieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, textStyle: { color: '#6f6658' } },
  series: [
    {
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['50%', '45%'],
      data: reservationPieData.value,
      label: { formatter: '{b}\n{c} 次' },
    },
  ],
}))

const teaBarOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 48, right: 24, top: 28, bottom: 72 },
  xAxis: {
    type: 'category',
    data: topTeaPurchases.value.map((item) => item.name),
    axisLabel: { rotate: 18, color: '#557568' },
    axisLine: { lineStyle: { color: '#b6d0c5' } },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: 'rgba(27,92,72,0.08)' } },
  },
  series: [
    {
      type: 'bar',
      data: topTeaPurchases.value.map((item) => item.value),
      barMaxWidth: 42,
      itemStyle: {
        borderRadius: [10, 10, 0, 0],
        color: '#2f6a56',
      },
    },
  ],
}))

const teaPieOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, textStyle: { color: '#557568' } },
  series: [
    {
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['50%', '45%'],
      data: teaPurchasePieData.value,
      label: { formatter: '{b}\n{c} 件' },
    },
  ],
}))

onMounted(async () => {
  if (authStore.role === 'ADMIN') {
    const [overviewData, teaRoomPage, teaPage, reservationPage, orderPage] = await Promise.all([
      fetchOverview(),
      teaRoomApi.rooms({ page: 0, size: 500 }),
      teaApi.page({ page: 0, size: 500 }),
      reservationApi.page({ page: 0, size: 500 }),
      orderApi.page({ page: 0, size: 500 }),
    ])

    overview.value = overviewData
    adminTeaRooms.value = teaRoomPage.content ?? []
    adminTeas.value = teaPage.content ?? []
    adminReservations.value = reservationPage.content ?? []
    adminOrders.value = orderPage.content ?? []
    adminOrderDetails.value = (
      await Promise.all(adminOrders.value.map((order) => orderApi.detail(order.id).catch(() => null)))
    ).filter((item): item is OrderDetailRecord => Boolean(item))
  }

  if (authStore.user?.id && authStore.role === 'USER') {
    recommendations.value = await fetchRecommendations(authStore.user.id)
  }

  if (authStore.user?.id && authStore.role === 'USER') {
    const [orders, reservations, favorites, registrations] = await Promise.all([
      orderApi.page({ page: 0, size: 50, userId: authStore.user.id }),
      reservationApi.page({ page: 0, size: 50, userId: authStore.user.id }),
      favoriteApi.page({ page: 0, size: 50, userId: authStore.user.id }),
      activityApi.registrations({ page: 0, size: 50, userId: authStore.user.id }),
    ])
    userOrders.value = orders.content
    userReservations.value = reservations.content
    userFavorites.value = favorites.content
    userRegistrations.value = registrations.content
  }

  if (authStore.user?.id && authStore.role === 'STAFF') {
    const [reservations, reports] = await Promise.all([
      reservationApi.page({ page: 0, size: 50, staffUserId: authStore.user.id }),
      reportApi.page({ page: 0, size: 50, staffUserId: authStore.user.id }),
    ])
    staffReservations.value = reservations.content
    staffReports.value = reports.content
  }
})
</script>

<template>
  <div class="page-shell">
    <PageIntro
      title="运营总览"
      description="集中查看平台预约、订单、活动与资源数据。管理员首页重点展示茶室预约热度和茶叶购买热度。"
    />

    <div v-if="overview" class="metric-grid">
      <MetricCard label="用户数" :value="overview.userCount" sub="平台注册用户规模" />
      <MetricCard label="茶室数" :value="overview.teaRoomCount" sub="当前接入的茶室资源" />
      <MetricCard label="订单数" :value="overview.orderCount" sub="累计订单处理量" />
      <MetricCard label="交易额" :value="`¥ ${overview.totalOrderAmount}`" sub="平台累计订单总额" />
    </div>

    <div v-if="authStore.role === 'ADMIN'" class="chart-grid">
      <div class="section-card">
        <div class="section-card__header">
          <h3 class="section-card__title">茶室预约次数</h3>
        </div>
        <div class="section-card__body">
          <VChart class="dashboard-chart" :option="reservationBarOption" :init-options="{ renderer: 'canvas' }" autoresize />
        </div>
      </div>

      <div class="section-card">
        <div class="section-card__header">
          <h3 class="section-card__title">茶室预约占比</h3>
        </div>
        <div class="section-card__body">
          <VChart class="dashboard-chart" :option="reservationPieOption" :init-options="{ renderer: 'canvas' }" autoresize />
        </div>
      </div>

      <div class="section-card">
        <div class="section-card__header">
          <h3 class="section-card__title">茶叶购买次数</h3>
        </div>
        <div class="section-card__body">
          <VChart class="dashboard-chart" :option="teaBarOption" :init-options="{ renderer: 'canvas' }" autoresize />
        </div>
      </div>

      <div class="section-card">
        <div class="section-card__header">
          <h3 class="section-card__title">茶叶购买占比</h3>
        </div>
        <div class="section-card__body">
          <VChart class="dashboard-chart" :option="teaPieOption" :init-options="{ renderer: 'canvas' }" autoresize />
        </div>
      </div>
    </div>

    <div v-if="authStore.role === 'STAFF'" class="metric-grid">
      <MetricCard label="待处理预约" :value="staffMetrics.pendingReservations" sub="需要确认、跟进的预约记录" />
      <MetricCard label="处理中报障" :value="staffMetrics.activeReports" sub="待处理与处理中报障" />
      <MetricCard label="今日预约" :value="staffMetrics.todayReservations" sub="今天开始的预约数" />
      <MetricCard label="我的茶室" :value="Math.max(1, staffReservations.length ? 1 : 0)" sub="当前分配给我的茶室" />
    </div>

    <div v-if="authStore.role === 'USER'" class="metric-grid">
      <MetricCard label="待支付订单" :value="userMetrics.pendingOrders" sub="可在订单中心完成支付/取消" />
      <MetricCard label="进行中订单" :value="userMetrics.activeOrders" sub="待支付与已支付订单" />
      <MetricCard label="待出行预约" :value="userMetrics.upcomingReservations" sub="待确认与已确认预约" />
      <MetricCard label="我的收藏" :value="userMetrics.favorites" sub="已收藏的茶叶/茶室" />
    </div>

    <div class="section-card">
      <div class="section-card__header">
        <h3 class="section-card__title">业务趋势</h3>
      </div>
      <div class="section-card__body">
        <TrendChart title="预约热度" :values="[18, 25, 22, 30, 28, 35, 33]" />
      </div>
    </div>

    <div class="section-card" v-if="recommendations">
      <div class="section-card__header">
        <h3 class="section-card__title">个性化推荐</h3>
      </div>
      <div class="section-card__body">
        <el-table :data="recommendations.recommendedTeas" class="data-table" style="width: 100%">
          <el-table-column prop="name" label="茶叶名称" />
          <el-table-column prop="category" label="分类" />
          <el-table-column prop="price" label="价格" />
          <el-table-column prop="stock" label="库存" />
        </el-table>
      </div>
    </div>

    <div class="section-card" v-if="authStore.role === 'USER' && userRegistrations.length">
      <div class="section-card__header">
        <h3 class="section-card__title">我的活动报名</h3>
      </div>
      <div class="section-card__body">
        <el-table :data="userRegistrations" class="data-table" style="width: 100%">
          <el-table-column prop="activityId" label="活动ID" width="120" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.cancelled ? 'danger' : 'success'">{{ row.cancelled ? '已取消' : '有效' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.metric-grid,
.chart-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.chart-grid {
  margin-top: 18px;
  margin-bottom: 18px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.dashboard-chart {
  height: 340px;
}

@media (max-width: 1200px) {
  .metric-grid,
  .chart-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .metric-grid,
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
