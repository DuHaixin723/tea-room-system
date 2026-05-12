<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import { couponApi } from '@/api/modules/management'
import { usePagedTable } from '@/composables/usePagedTable'
import { useAuthStore } from '@/stores/auth'
import type { UserCouponRecord } from '@/types/business'
import PageIntro from '@/components/shared/PageIntro.vue'

const LEVEL_OPTIONS = [
  { label: '普通会员', value: 'NORMAL' },
  { label: '白银会员', value: 'SILVER' },
  { label: '黄金会员', value: 'GOLD' },
  { label: '铂金会员', value: 'PLATINUM' },
  { label: '钻石会员', value: 'DIAMOND' },
]

const STATUS_OPTIONS = [
  { label: '可使用', value: 'UNUSED' },
  { label: '已使用', value: 'USED' },
  { label: '已过期', value: 'EXPIRED' },
]

const USER_CARD_PAGE_SIZE = 2
const ADMIN_TABLE_PAGE_SIZE = 12

const authStore = useAuthStore()
const isAdmin = computed(() => authStore.role === 'ADMIN')

const table = usePagedTable<UserCouponRecord>((params) => couponApi.page(params), {
  size: authStore.role === 'ADMIN' ? ADMIN_TABLE_PAGE_SIZE : USER_CARD_PAGE_SIZE,
})

const refreshing = ref(false)
const dispatchingWeekly = ref(false)
const dispatchingLevel = ref(false)
const selectedLevel = ref('GOLD')
const keyword = ref('')
const levelFilter = ref('')
const statusFilter = ref('')

const availableCount = computed(() => table.rows.filter((item) => item.status === 'UNUSED').length)
const expiringCount = computed(() =>
  table.rows.filter((item) => item.status === 'UNUSED' && dayjs(item.validUntil).diff(dayjs(), 'hour') <= 24).length,
)
const totalDiscount = computed(() =>
  table.rows.filter((item) => item.status === 'UNUSED').reduce((sum, item) => sum + Number(item.discountAmount), 0),
)

const visibleRows = computed(() => {
  if (!isAdmin.value) return table.rows

  const query = keyword.value.trim().toLowerCase()
  return table.rows.filter((row) => {
    const matchKeyword = !query || [row.title, row.couponCode, levelLabel(row.sourceLevel), couponHeadline(row)]
      .some((item) => String(item).toLowerCase().includes(query))

    const matchLevel = !levelFilter.value || row.sourceLevel === levelFilter.value
    const matchStatus = !statusFilter.value || row.status === statusFilter.value

    return matchKeyword && matchLevel && matchStatus
  })
})

function levelLabel(level?: string) {
  return LEVEL_OPTIONS.find((item) => item.value === level)?.label ?? level ?? '未知等级'
}

function couponStatusLabel(status: UserCouponRecord['status']) {
  if (status === 'UNUSED') return '可使用'
  if (status === 'USED') return '已使用'
  return '已过期'
}

function couponStatusType(status: UserCouponRecord['status']) {
  if (status === 'UNUSED') return 'success'
  if (status === 'USED') return 'info'
  return 'danger'
}

function formatValidity(row: UserCouponRecord) {
  return `${dayjs(row.validFrom).format('YYYY-MM-DD HH:mm')} - ${dayjs(row.validUntil).format('YYYY-MM-DD HH:mm')}`
}

function expiryText(row: UserCouponRecord) {
  if (row.status === 'EXPIRED') return '已过期'
  const diffHours = dayjs(row.validUntil).diff(dayjs(), 'hour')
  if (diffHours <= 24) return `截止 ${dayjs(row.validUntil).format('MM-DD HH:mm')}，即将到期`
  return `截止 ${dayjs(row.validUntil).format('MM-DD HH:mm')}`
}

function couponHeadline(row: UserCouponRecord) {
  return `满${Number(row.thresholdAmount)}-${Number(row.discountAmount)}`
}

function resetFilters() {
  keyword.value = ''
  levelFilter.value = ''
  statusFilter.value = ''
}

async function refreshCoupons() {
  refreshing.value = true
  try {
    await table.load({
      page: 0,
      size: isAdmin.value ? ADMIN_TABLE_PAGE_SIZE : USER_CARD_PAGE_SIZE,
    })
  } finally {
    refreshing.value = false
  }
}

async function dispatchWeeklyCoupons() {
  dispatchingWeekly.value = true
  try {
    const count = await couponApi.dispatchWeekly()
    ElMessage.success(count > 0 ? `已补发 ${count} 张本周会员优惠券` : '本周优惠券已发放，无需重复补发')
    await refreshCoupons()
  } finally {
    dispatchingWeekly.value = false
  }
}

async function dispatchLevelCoupons() {
  dispatchingLevel.value = true
  try {
    const count = await couponApi.dispatchByLevel(selectedLevel.value)
    ElMessage.success(count > 0 ? `已向${levelLabel(selectedLevel.value)}发放 ${count} 张优惠券` : `${levelLabel(selectedLevel.value)}当前没有可发券会员`)
    await refreshCoupons()
  } finally {
    dispatchingLevel.value = false
  }
}

onMounted(() =>
  table.load({
    page: 0,
    size: isAdmin.value ? ADMIN_TABLE_PAGE_SIZE : USER_CARD_PAGE_SIZE,
  }),
)
</script>

<template>
  <div class="page-shell">
    <PageIntro
      :title="isAdmin ? '会员优惠券管理' : '我的会员优惠券'"
      :description="isAdmin ? '管理员可以按会员等级批量发券，也可以补发本周固定会员券。普通会员可以查看优惠券状态和有效时间。' : '仅展示优惠券卡片，每页 2 张。'"
    >
      <el-button :loading="refreshing" @click="refreshCoupons">刷新</el-button>
    </PageIntro>

    <div v-if="isAdmin" class="section-card admin-actions">
      <div class="section-card__body admin-actions__body">
        <div class="action-block">
          <div>
            <h3>按等级发券</h3>
            <p>普通 99-10 / 7天，白银 120-15 / 10天，黄金 200-30 / 15天，铂金 300-50 / 20天，钻石 500-88 / 30天。</p>
          </div>
          <div class="action-row">
            <el-select v-model="selectedLevel" style="width: 180px">
              <el-option v-for="item in LEVEL_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button type="primary" :loading="dispatchingLevel" @click="dispatchLevelCoupons">按等级发券</el-button>
          </div>
        </div>

        <div class="action-block">
          <div>
            <h3>补发本周优惠券</h3>
            <p>用于补发本周固定会员券，系统会自动避免同一用户在同一周重复领取。</p>
          </div>
          <el-button :loading="dispatchingWeekly" @click="dispatchWeeklyCoupons">补发本周优惠券</el-button>
        </div>
      </div>
    </div>

    <div class="coupon-metrics">
      <div class="metric-card">
        <span>当前页可用</span>
        <strong>{{ availableCount }}</strong>
      </div>
      <div class="metric-card">
        <span>当前页即将到期</span>
        <strong>{{ expiringCount }}</strong>
      </div>
      <div class="metric-card">
        <span>当前页可节省</span>
        <strong>¥{{ totalDiscount }}</strong>
      </div>
    </div>

    <section v-if="!isAdmin" class="section-card coupon-card-section">
      <div class="section-card__body">
        <div v-if="table.rows.length" class="coupon-grid">
          <article v-for="row in table.rows" :key="row.id" class="coupon-card" :class="`is-${row.status.toLowerCase()}`">
            <div class="coupon-card__top">
              <el-tag :type="couponStatusType(row.status)">{{ couponStatusLabel(row.status) }}</el-tag>
              <span class="coupon-card__level">{{ levelLabel(row.sourceLevel) }}</span>
            </div>
            <h3>{{ row.title }}</h3>
            <div class="coupon-card__headline">{{ couponHeadline(row) }}</div>
            <div class="coupon-card__money">
              <strong>减 ¥{{ row.discountAmount }}</strong>
              <span>满 ¥{{ row.thresholdAmount }} 可用</span>
            </div>
            <div class="coupon-card__code">{{ row.couponCode }}</div>
            <div class="coupon-card__meta">
              <div>
                <span>发放批次</span>
                <strong>{{ row.issuedWeek }}</strong>
              </div>
              <div>
                <span>有效期</span>
                <strong>{{ expiryText(row) }}</strong>
              </div>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前暂无优惠券" />
        <el-pagination
          v-if="table.pager.total > 0"
          class="coupon-pagination"
          background
          layout="prev, pager, next"
          :current-page="table.pager.page + 1"
          :page-size="USER_CARD_PAGE_SIZE"
          :total="table.pager.total"
          @current-change="table.handlePageChange"
        />
      </div>
    </section>

    <div v-if="isAdmin" class="section-card">
      <div class="section-card__body">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="keyword" clearable placeholder="搜索优惠券名称或券码" style="width: 280px" />
            <el-select v-model="levelFilter" clearable placeholder="会员等级" style="width: 160px">
              <el-option v-for="item in LEVEL_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="statusFilter" clearable placeholder="可用状态" style="width: 160px">
              <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>

        <el-table v-loading="table.loading" :data="visibleRows" class="data-table">
          <el-table-column prop="title" label="优惠券名称" min-width="180" />
          <el-table-column prop="couponCode" label="券码" min-width="170" />
          <el-table-column label="优惠内容" min-width="160">
            <template #default="{ row }">满 {{ row.thresholdAmount }} - {{ row.discountAmount }}</template>
          </el-table-column>
          <el-table-column label="会员等级" width="120">
            <template #default="{ row }">{{ levelLabel(row.sourceLevel) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="couponStatusType(row.status)">{{ couponStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="有效时间" min-width="260">
            <template #default="{ row }">{{ formatValidity(row) }}</template>
          </el-table-column>
        </el-table>
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="table.pager.page + 1"
          :page-size="table.pager.size"
          :total="table.pager.total"
          @current-change="table.handlePageChange"
          @size-change="table.handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar,
.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar {
  justify-content: space-between;
  margin-bottom: 16px;
}

.admin-actions {
  margin-bottom: 18px;
}

.admin-actions__body {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.action-block {
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid rgba(47, 93, 71, 0.12);
  border-radius: 20px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(247, 243, 235, 0.96));
}

.action-block h3 {
  margin: 0 0 10px;
  color: #2f2418;
}

.action-block p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.coupon-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.metric-card {
  padding: 18px 20px;
  border: 1px solid var(--line);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(248, 244, 237, 0.96));
}

.metric-card span {
  color: var(--muted);
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin-top: 10px;
  font-size: 28px;
}

.coupon-card-section {
  margin-bottom: 20px;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.coupon-pagination {
  display: flex;
  justify-content: center;
}

.coupon-card {
  padding: 22px;
  border-radius: 24px;
  border: 1px solid rgba(123, 88, 50, 0.16);
  background:
    radial-gradient(circle at top right, rgba(255, 221, 166, 0.28), transparent 30%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(249, 244, 236, 0.96));
}

.coupon-card.is-expired {
  opacity: 0.66;
}

.coupon-card__top,
.coupon-card__meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.coupon-card__level {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(47, 93, 71, 0.08);
  color: #2f5d47;
  font-size: 12px;
  font-weight: 700;
}

.coupon-card h3 {
  margin: 18px 0 10px;
  font-size: 22px;
  color: #2f2418;
}

.coupon-card__headline {
  display: inline-flex;
  align-items: center;
  margin-bottom: 14px;
  padding: 10px 14px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(138, 90, 43, 0.12), rgba(255, 221, 166, 0.42));
  color: #7a4f22;
  font-size: 30px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.coupon-card__money strong {
  display: block;
  font-size: 24px;
  line-height: 1;
  color: #8a5a2b;
}

.coupon-card__money span,
.coupon-card__meta span {
  color: var(--muted);
  font-size: 13px;
}

.coupon-card__money span {
  display: block;
  margin-top: 8px;
}

.coupon-card__code {
  margin-top: 18px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(47, 93, 71, 0.06);
  color: #2f5d47;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.coupon-card__meta {
  margin-top: 18px;
}

.coupon-card__meta strong {
  display: block;
  margin-top: 8px;
  font-size: 15px;
}

@media (max-width: 960px) {
  .admin-actions__body,
  .coupon-metrics,
  .coupon-grid {
    grid-template-columns: 1fr;
  }

  .coupon-card__headline {
    font-size: 24px;
  }
}
</style>
