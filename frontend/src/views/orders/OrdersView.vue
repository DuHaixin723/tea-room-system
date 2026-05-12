<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { orderApi, reservationApi, reviewApi, teaApi, teaRoomApi, usersApi } from '@/api/modules/management'
import { usePagedTable } from '@/composables/usePagedTable'
import PageIntro from '@/components/shared/PageIntro.vue'
import { useAuthStore } from '@/stores/auth'
import { useMemberStore } from '@/stores/member'
import type {
  MemberAccountRecord,
  OrderDetailRecord,
  OrderRecord,
  ReservationRecord,
  ReviewRecord,
  TeaRecord,
  TeaRoomRecord,
} from '@/types/business'
import { formatStatus } from '@/utils/status'

interface ReviewForm {
  reservationId: number | null
  teaRoomId: number | null
  rating: number
  content: string
}

interface PayForm {
  paymentMethod: 'BALANCE' | 'WECHAT' | 'ALIPAY'
}

const authStore = useAuthStore()
const memberStore = useMemberStore()
const router = useRouter()

const keyword = ref('')
const categoryFilter = ref<'ROOM' | 'TEA' | ''>('')
const statusFilter = ref<OrderRecord['status'] | ''>('')
const teas = ref<TeaRecord[]>([])
const reservations = ref<ReservationRecord[]>([])
const teaRooms = ref<TeaRoomRecord[]>([])
const reviews = ref<ReviewRecord[]>([])
const account = ref<MemberAccountRecord | null>(null)
const detail = ref<OrderDetailRecord | null>(null)
const orderDetailsMap = ref<Record<number, OrderDetailResponseLite>>({})
const drawerVisible = ref(false)
const detailLoading = ref(false)
const payingId = ref<number | null>(null)
const reviewDialogVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewFormRef = ref<FormInstance>()
const payDialogVisible = ref(false)
const paySubmitting = ref(false)
const payingOrder = ref<OrderRecord | null>(null)

type OrderDetailResponseLite = Pick<OrderDetailRecord, 'order' | 'items'>

const reviewForm = reactive<ReviewForm>({
  reservationId: null,
  teaRoomId: null,
  rating: 5,
  content: '',
})

const payForm = reactive<PayForm>({
  paymentMethod: 'BALANCE',
})

const reviewRules: FormRules<ReviewForm> = {
  reservationId: [{ required: true, message: '缺少关联预约', trigger: 'change' }],
  teaRoomId: [{ required: true, message: '缺少关联茶室', trigger: 'change' }],
  rating: [{ required: true, message: '请选择评分', trigger: 'change' }],
}

const table = usePagedTable<OrderRecord>((params) => {
  const requestParams = { ...params }
  if (authStore.role === 'USER' && authStore.user?.id) requestParams.userId = authStore.user.id
  return orderApi.page(requestParams)
})

const visibleRows = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return table.rows.filter((row) => {
    const reservation = reservationRecord(row.reservationId)
    const room = teaRoomRecord(reservation?.teaRoomId)
    const roomOrder = isReservationFeeOrder(row)
    const categoryLabel = roomOrder ? '茶室预约' : '茶叶购买'
    const itemNames = orderItemsOf(row).map((item) => teaName(item.teaId))

    const matchKeyword = !query || [
      row.orderNo,
      room?.name ?? '',
      room?.location ?? '',
      categoryLabel,
      ...itemNames,
    ].some((item) => String(item).toLowerCase().includes(query))

    const matchCategory = !categoryFilter.value
      || (categoryFilter.value === 'ROOM' && roomOrder)
      || (categoryFilter.value === 'TEA' && !roomOrder)

    const matchStatus = !statusFilter.value || row.status === statusFilter.value

    return matchKeyword && matchCategory && matchStatus
  })
})

const detailReviewable = computed(() => {
  if (!detail.value?.order) return false
  return canReviewOrder(detail.value.order)
})

const detailContactable = computed(() => {
  if (!detail.value?.order) return false
  return canContactAdmin(detail.value.order)
})

const payMethodOptions = computed(() => {
  const order = payingOrder.value
  const shortfall = order ? requiredRechargeAmount(order) : 0
  return [
    {
      label: '余额支付',
      value: 'BALANCE' as const,
      description: shortfall > 0
        ? `当前余额不足，还差 ¥${shortfall.toFixed(2)}`
        : `当前余额 ¥${Number(account.value?.balance ?? 0).toFixed(2)}`,
      disabled: false,
    },
    {
      label: '微信支付',
      value: 'WECHAT' as const,
      description: '在线支付，适合快速完成下单',
      disabled: false,
    },
    {
      label: '支付宝',
      value: 'ALIPAY' as const,
      description: '在线支付，支持直接完成付款',
      disabled: false,
    },
  ]
})

function teaName(id: number) {
  return teas.value.find((item) => item.id === id)?.name ?? `茶品 #${id}`
}

function reservationRecord(reservationId?: number | null) {
  if (!reservationId) return null
  return reservations.value.find((item) => item.id === reservationId) ?? null
}

function teaRoomRecord(teaRoomId?: number | null) {
  if (!teaRoomId) return null
  return teaRooms.value.find((item) => item.id === teaRoomId) ?? null
}

function roomName(teaRoomId?: number | null) {
  return teaRoomRecord(teaRoomId)?.name ?? (teaRoomId ? `茶室 #${teaRoomId}` : '未关联茶室')
}

function formatBusinessTime(value?: string) {
  return value?.slice(0, 5) || '--:--'
}

function roomBusinessHours(teaRoomId?: number | null) {
  const room = teaRoomRecord(teaRoomId)
  if (!room) return '未配置'
  return `${formatBusinessTime(room.businessStartTime)} - ${formatBusinessTime(room.businessEndTime)}`
}

function reservationInfo(reservationId?: number | null) {
  const reservation = reservationRecord(reservationId)
  if (!reservation) return reservationId ? `预约 #${reservationId}` : '未关联预约'
  return `${roomName(reservation.teaRoomId)} · ${dayjs(reservation.reservedStartTime).format('MM-DD HH:mm')}`
}

function reservationWindow(reservationId?: number | null) {
  const reservation = reservationRecord(reservationId)
  if (!reservation) return '无预约时间'
  return `${dayjs(reservation.reservedStartTime).format('YYYY-MM-DD HH:mm')} - ${dayjs(reservation.reservedEndTime).format('HH:mm')}`
}

function reservationStatus(reservationId?: number | null) {
  const reservation = reservationRecord(reservationId)
  return reservation ? formatStatus('reservation', reservation.status) : '未关联预约'
}

function paymentMethodLabel(value?: OrderRecord['paymentMethod']) {
  if (value === 'BALANCE') return '余额支付'
  if (value === 'WECHAT') return '微信支付'
  if (value === 'ALIPAY') return '支付宝'
  return '未支付'
}

function orderItemsOf(row: OrderRecord) {
  const current = detail.value?.order.id === row.id ? detail.value : orderDetailsMap.value[row.id]
  return current?.items ?? []
}

function isReservationFeeOrder(row: OrderRecord) {
  return Boolean(row.reservationId) && orderItemsOf(row).length === 0
}

function itemSummary(row: OrderRecord) {
  const items = orderItemsOf(row)
  if (isReservationFeeOrder(row)) return '茶室预约费用'
  if (!items.length) return '点击详情查看商品'
  return items.slice(0, 3).map((item) => `${teaName(item.teaId)} x${item.quantity}`).join('、')
}

function lineAmount(quantity: number, unitPrice: number) {
  return Number(quantity) * Number(unitPrice)
}

function detailItemCount() {
  return detail.value?.items.reduce((sum, item) => sum + Number(item.quantity ?? 0), 0) ?? 0
}

function hasReviewed(reservationId?: number | null) {
  if (!reservationId) return false
  return reviews.value.some((item) => item.reservationId === reservationId)
}

function canReviewOrder(row: OrderRecord) {
  const reservation = reservationRecord(row.reservationId)
  return authStore.role === 'USER'
    && row.status === 'COMPLETED'
    && Boolean(row.reservationId)
    && reservation?.status === 'COMPLETED'
    && !hasReviewed(row.reservationId)
}

function canLeaveEarly(row: OrderRecord) {
  const reservation = reservationRecord(row.reservationId)
  return authStore.role === 'USER'
    && row.status === 'PAID'
    && ['USER_CHECKED_IN', 'STAFF_CONFIRMED_CHECK_IN', 'CHARGING'].includes(reservation?.status ?? '')
}

function canContactAdmin(row: OrderRecord) {
  return authStore.role === 'USER' && row.status !== 'NO_SHOW'
}

function canRefundOrder(row: OrderRecord) {
  return authStore.role === 'ADMIN' && ['PAID', 'COMPLETED'].includes(row.status)
}

function requiredRechargeAmount(row: OrderRecord) {
  return Math.max(Number(row.amount) - Number(account.value?.balance ?? 0), 0)
}

function resetFilters() {
  keyword.value = ''
  categoryFilter.value = ''
  statusFilter.value = ''
}

function redirectToRecharge(row: OrderRecord, amount: number) {
  const minimumAmount = Number(amount.toFixed(2))
  ElMessage.warning(`当前余额不足，还差 ¥${minimumAmount.toFixed(2)}，将跳转到充值页`)
  router.push({
    path: '/recharge',
    query: {
      amount: String(minimumAmount),
      redirect: '/orders',
      orderNo: row.orderNo,
    },
  })
}

function resetReviewForm() {
  Object.assign(reviewForm, {
    reservationId: null,
    teaRoomId: null,
    rating: 5,
    content: '',
  })
  reviewFormRef.value?.clearValidate()
}

function openReview(row: OrderRecord) {
  const reservation = reservationRecord(row.reservationId)
  if (!reservation) return
  reviewForm.reservationId = reservation.id
  reviewForm.teaRoomId = reservation.teaRoomId
  reviewForm.rating = 5
  reviewForm.content = ''
  reviewDialogVisible.value = true
}

async function openDetail(id: number) {
  detailLoading.value = true
  try {
    detail.value = await orderApi.detail(id)
    orderDetailsMap.value = {
      ...orderDetailsMap.value,
      [id]: detail.value,
    }
    drawerVisible.value = true
  } finally {
    detailLoading.value = false
  }
}

async function leaveEarly(row: OrderRecord) {
  await ElMessageBox.confirm(
    '确认提前离店后，系统会立刻完成订单、释放茶室预约状态，并引导你进入服务评价流程。',
    '提前离店确认',
    { type: 'warning', confirmButtonText: '确认离店', cancelButtonText: '暂不离店' },
  )
  await orderApi.status(row.id, { status: 'COMPLETED' })
  await Promise.all([table.load(), loadContext()])
  if (detail.value?.order.id === row.id) {
    detail.value = await orderApi.detail(row.id)
    orderDetailsMap.value = {
      ...orderDetailsMap.value,
      [row.id]: detail.value,
    }
  }
  ElMessage.success('已为你完成提前离店，茶室已释放，可继续提交评价')
  if (canReviewOrder({ ...row, status: 'COMPLETED' })) {
    openReview({ ...row, status: 'COMPLETED' })
  }
}

async function refundOrder(row: OrderRecord) {
  await ElMessageBox.confirm(
    '退款后将保留订单记录，并把金额退回用户余额；如订单包含茶叶会恢复库存，预约费用订单会释放对应预约资源。',
    '确认退款',
    { type: 'warning', confirmButtonText: '确认退款', cancelButtonText: '取消' },
  )
  await orderApi.status(row.id, { status: 'REFUNDED' })
  await Promise.all([table.load(), loadContext()])
  if (detail.value?.order.id === row.id) {
    detail.value = await orderApi.detail(row.id)
    orderDetailsMap.value = {
      ...orderDetailsMap.value,
      [row.id]: detail.value,
    }
  }
  ElMessage.success('订单已退款')
}

function openPayDialog(row: OrderRecord) {
  payingOrder.value = row
  payForm.paymentMethod = 'BALANCE'
  payDialogVisible.value = true
}

async function confirmPayOrder() {
  if (!payingOrder.value) return
  const row = payingOrder.value

  if (payForm.paymentMethod === 'BALANCE') {
    const shortfall = requiredRechargeAmount(row)
    if (shortfall > 0) {
      payDialogVisible.value = false
      redirectToRecharge(row, shortfall)
      return
    }
  }

  paySubmitting.value = true
  payingId.value = row.id
  try {
    await orderApi.pay(row.id, { paymentMethod: payForm.paymentMethod })
    ElMessage.success(`订单已使用${paymentMethodLabel(payForm.paymentMethod)}完成支付`)
    payDialogVisible.value = false
    payingOrder.value = null
    await Promise.all([table.load(), loadContext(), memberStore.refreshAccount()])
  } catch (error) {
    const message = error instanceof Error ? error.message : ''
    if (payForm.paymentMethod === 'BALANCE' && message.includes('余额不足')) {
      payDialogVisible.value = false
      redirectToRecharge(row, Math.max(requiredRechargeAmount(row), 0.01))
      return
    }
    throw error
  } finally {
    paySubmitting.value = false
    payingId.value = null
  }
}

async function contactAdmin(row: OrderRecord) {
  if (!canContactAdmin(row)) return
  await router.push({ path: '/consultations', query: { orderId: String(row.id), auto: '1' } })
}

async function submitReview() {
  const valid = await reviewFormRef.value?.validate().catch(() => false)
  if (!valid || !authStore.user?.id || !reviewForm.reservationId || !reviewForm.teaRoomId) return
  reviewSubmitting.value = true
  try {
    await reviewApi.create({
      userId: authStore.user.id,
      reservationId: reviewForm.reservationId,
      teaRoomId: reviewForm.teaRoomId,
      rating: reviewForm.rating,
      content: reviewForm.content,
    })
    ElMessage.success('评价已提交')
    reviewDialogVisible.value = false
    await loadContext()
  } finally {
    reviewSubmitting.value = false
  }
}

async function warmOrderDetails(rows: OrderRecord[]) {
  if (authStore.role !== 'ADMIN') return
  const unresolved = rows.filter((row) => !orderDetailsMap.value[row.id])
  if (!unresolved.length) return

  const entries = await Promise.all(
    unresolved.map(async (row) => {
      const data = await orderApi.detail(row.id)
      return [row.id, data] as const
    }),
  )

  orderDetailsMap.value = {
    ...orderDetailsMap.value,
    ...Object.fromEntries(entries),
  }
}

async function loadContext() {
  const reviewRequest = authStore.role === 'USER' ? reviewApi.page({ page: 0, size: 200 }) : Promise.resolve({ content: [] as ReviewRecord[] })
  const [teaPage, reservationPage, roomPage, reviewPage] = await Promise.all([
    teaApi.page({ page: 0, size: 100 }),
    reservationApi.page(authStore.role === 'USER' && authStore.user?.id ? { page: 0, size: 200, userId: authStore.user.id } : { page: 0, size: 200 }),
    teaRoomApi.rooms({ page: 0, size: 200 }),
    reviewRequest,
  ])
  teas.value = teaPage.content ?? []
  reservations.value = reservationPage.content ?? []
  teaRooms.value = roomPage.content ?? []
  reviews.value = reviewPage.content ?? []
  if (authStore.role === 'USER' && authStore.user?.id) {
    account.value = await usersApi.account(authStore.user.id)
    memberStore.setAccount(account.value)
  }
}

onMounted(async () => {
  await Promise.all([table.load(), loadContext()])
  await warmOrderDetails(table.rows)
})

watch(
  () => table.rows,
  async (rows) => {
    await warmOrderDetails(rows)
  },
  { deep: true },
)
</script>

<template>
  <div class="page-shell">
    <PageIntro
      title="订单中心"
      description="茶室预约确认后会生成待支付订单。支付时可以选择余额、微信或支付宝，服务结束后可以直接评价。"
    >
      <el-button v-if="authStore.role === 'USER'" plain @click="router.push('/recharge')">充值</el-button>
    </PageIntro>

    <div class="section-card">
      <div class="section-card__body">
        <div v-if="authStore.role === 'ADMIN' || authStore.role === 'USER' || authStore.role === 'STAFF'" class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="keyword" clearable placeholder="搜索订单号、茶室名称、茶叶名称或位置" style="width: 320px" />
            <el-select v-model="categoryFilter" clearable placeholder="购买内容分类" style="width: 180px">
              <el-option label="茶室预约" value="ROOM" />
              <el-option label="茶叶购买" value="TEA" />
            </el-select>
            <el-select v-model="statusFilter" clearable placeholder="订单状态" style="width: 160px">
              <el-option :label="formatStatus('order', 'PENDING_PAYMENT')" value="PENDING_PAYMENT" />
              <el-option :label="formatStatus('order', 'PAID')" value="PAID" />
              <el-option :label="formatStatus('order', 'NO_SHOW')" value="NO_SHOW" />
              <el-option :label="formatStatus('order', 'COMPLETED')" value="COMPLETED" />
              <el-option :label="formatStatus('order', 'CANCELLED')" value="CANCELLED" />
              <el-option :label="formatStatus('order', 'REFUNDED')" value="REFUNDED" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>

        <el-table v-loading="table.loading" :data="visibleRows" class="data-table">
          <el-table-column prop="orderNo" label="订单号" min-width="210" />
          <el-table-column label="购买内容" min-width="240">
            <template #default="{ row }">{{ itemSummary(row) }}</template>
          </el-table-column>
          <el-table-column v-if="authStore.role === 'ADMIN'" label="内容分类" width="120">
            <template #default="{ row }">{{ isReservationFeeOrder(row) ? '茶室预约' : '茶叶购买' }}</template>
          </el-table-column>
          <el-table-column label="预约信息" min-width="240">
            <template #default="{ row }">
              <div class="cell-stack">
                <strong>{{ reservationInfo(row.reservationId) }}</strong>
                <span>{{ reservationWindow(row.reservationId) }}</span>
                <span v-if="reservationRecord(row.reservationId)">营业时间 {{ roomBusinessHours(reservationRecord(row.reservationId)?.teaRoomId) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="120">
            <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="160">
            <template #default="{ row }">
              <div class="cell-stack">
                <el-tag>{{ formatStatus('order', row.status) }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="400" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
              <el-button v-if="canRefundOrder(row)" link type="danger" @click="refundOrder(row)">退款</el-button>
              <el-button v-if="authStore.role === 'USER' && row.status === 'PENDING_PAYMENT'" link type="warning" :loading="payingId === row.id" @click="openPayDialog(row)">支付</el-button>
              <el-button v-if="canLeaveEarly(row)" link type="warning" @click="leaveEarly(row)">提前离店</el-button>
              <el-button v-if="canContactAdmin(row)" link type="primary" @click="contactAdmin(row)">联系管理员</el-button>
              <el-button v-if="canReviewOrder(row)" link type="success" @click="openReview(row)">评价</el-button>
            </template>
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

    <el-dialog v-model="payDialogVisible" title="选择支付方式" width="520px">
      <template v-if="payingOrder">
        <div class="pay-summary">
          <div class="pay-summary__line">
            <span>订单号</span>
            <strong>{{ payingOrder.orderNo }}</strong>
          </div>
          <div class="pay-summary__line">
            <span>待支付金额</span>
            <strong>¥{{ Number(payingOrder.amount).toFixed(2) }}</strong>
          </div>
          <div class="pay-summary__line">
            <span>当前余额</span>
            <strong>¥{{ Number(account?.balance ?? 0).toFixed(2) }}</strong>
          </div>
        </div>

        <div class="pay-options">
          <button
            v-for="item in payMethodOptions"
            :key="item.value"
            type="button"
            class="pay-option"
            :class="{ 'is-active': payForm.paymentMethod === item.value, 'is-balance-short': item.value === 'BALANCE' && requiredRechargeAmount(payingOrder) > 0 }"
            :disabled="item.disabled"
            @click="payForm.paymentMethod = item.value"
          >
            <div class="pay-option__head">
              <div class="pay-option__title">{{ item.label }}</div>
              <div v-if="payForm.paymentMethod === item.value" class="pay-option__check">已选中</div>
            </div>
            <div class="pay-option__desc">{{ item.description }}</div>
          </button>
        </div>
      </template>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="paySubmitting" @click="confirmPayOrder">确认支付</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="drawerVisible" title="订单详情" size="48%">
      <template v-if="detail">
        <div class="order-detail">
          <div class="order-detail__summary">
            <div class="order-kpi">
              <span>订单金额</span>
              <strong>¥{{ Number(detail.order.amount).toFixed(2) }}</strong>
            </div>
            <div class="order-kpi">
              <span>商品件数</span>
              <strong>{{ detailItemCount() }}</strong>
            </div>
            <div class="order-kpi">
              <span>支付方式</span>
              <strong>{{ paymentMethodLabel(detail.order.paymentMethod) }}</strong>
            </div>
          </div>

          <el-descriptions :column="2" border class="order-detail__desc">
            <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">{{ formatStatus('order', detail.order.status) }}</el-descriptions-item>
            <el-descriptions-item label="用户 ID">{{ detail.order.userId }}</el-descriptions-item>
            <el-descriptions-item label="关联预约">{{ reservationInfo(detail.order.reservationId) }}</el-descriptions-item>
            <el-descriptions-item label="预约状态">{{ reservationStatus(detail.order.reservationId) }}</el-descriptions-item>
            <el-descriptions-item label="预约时段">{{ reservationWindow(detail.order.reservationId) }}</el-descriptions-item>
            <el-descriptions-item label="营业时间">{{ roomBusinessHours(reservationRecord(detail.order.reservationId)?.teaRoomId) }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ detail.order.createdAt ? dayjs(detail.order.createdAt).format('YYYY-MM-DD HH:mm:ss') : '-' }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ detail.order.updatedAt ? dayjs(detail.order.updatedAt).format('YYYY-MM-DD HH:mm:ss') : '-' }}</el-descriptions-item>
            <el-descriptions-item label="跟进提醒">
              {{ canLeaveEarly(detail.order) ? '用户已到店并处于服务进行阶段，可由用户发起提前离店完成订单。' : '订单完成以当前订单完成流程为准，完成后将同步释放关联预约状态。' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <el-table v-if="detail.items.length" v-loading="detailLoading" :data="detail.items" class="order-detail__table">
          <el-table-column label="茶品" min-width="160">
            <template #default="{ row }">{{ teaName(row.teaId) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="90" />
          <el-table-column prop="unitPrice" label="单价" width="110">
            <template #default="{ row }">¥{{ Number(row.unitPrice).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="小计" width="120">
            <template #default="{ row }">¥{{ lineAmount(row.quantity, row.unitPrice).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
        <div v-else class="order-detail__empty">这笔订单对应茶室预约费用，没有茶品明细。</div>
        <div class="order-detail__footer">
          <span>合计 {{ detailItemCount() }} 件商品</span>
          <strong>订单总额 ¥{{ Number(detail.order.amount).toFixed(2) }}</strong>
        </div>
        <div v-if="detailContactable" class="order-detail__contact-action">
          <el-button type="primary" plain @click="contactAdmin(detail.order)">联系管理员处理订单问题</el-button>
        </div>
        <div v-if="canLeaveEarly(detail.order)" class="order-detail__review-action">
          <el-button type="warning" plain @click="leaveEarly(detail.order)">提前离店并完成订单</el-button>
        </div>
        <div v-if="detailReviewable" class="order-detail__review-action">
          <el-button type="success" @click="openReview(detail.order)">去评价本次服务</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="reviewDialogVisible" title="服务评价" width="560px" @closed="resetReviewForm">
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-position="top">
        <el-form-item label="评分" prop="rating">
          <el-rate v-model="reviewForm.rating" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="reviewForm.content" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
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

.cell-stack {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cell-stack strong {
  color: #21493b;
}

.cell-stack span {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.pay-summary {
  display: grid;
  gap: 10px;
  margin-bottom: 18px;
  padding: 18px 20px;
  border-radius: 20px;
  background:
    radial-gradient(circle at top right, rgba(255, 221, 166, 0.3), transparent 35%),
    linear-gradient(145deg, rgba(255, 250, 242, 0.98), rgba(244, 250, 245, 0.96));
}

.pay-summary__line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pay-summary__line span {
  color: var(--muted);
}

.pay-summary__line strong {
  color: #21493b;
  font-size: 18px;
}

.pay-options {
  display: grid;
  gap: 12px;
}

.pay-option {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  text-align: left;
  border-radius: 18px;
  border: 1px solid rgba(33, 73, 59, 0.12);
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(249, 244, 236, 0.9));
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.pay-option.is-active {
  border-color: rgba(47, 93, 71, 0.4);
  box-shadow: 0 14px 30px rgba(47, 93, 71, 0.1);
  transform: translateY(-1px);
}

.pay-option.is-balance-short {
  border-style: dashed;
  border-color: rgba(138, 90, 43, 0.35);
}

.pay-option__title {
  font-size: 18px;
  font-weight: 700;
  color: #21493b;
}

.pay-option__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pay-option__check {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(47, 93, 71, 0.1);
  color: #2f5d47;
  font-size: 12px;
  font-weight: 700;
}

.pay-option__desc {
  color: var(--muted);
  line-height: 1.6;
}

.order-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.order-detail__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.order-kpi {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(32, 96, 75, 0.1);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(244, 249, 246, 0.92));
}

.order-kpi span {
  display: block;
  color: var(--muted);
  font-size: 13px;
}

.order-kpi strong {
  display: block;
  margin-top: 8px;
  color: #21493b;
  font-size: 24px;
  line-height: 1.2;
}

.order-detail__desc {
  margin-top: 2px;
}

.order-detail__table {
  margin-top: 18px;
}

.order-detail__empty {
  margin-top: 18px;
  padding: 18px;
  border-radius: 16px;
  background: #f7f7f1;
  color: var(--muted);
}

.order-detail__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(32, 96, 75, 0.1);
  color: var(--muted);
}

.order-detail__footer strong {
  color: #21493b;
  font-size: 20px;
}

.order-detail__contact-action,
.order-detail__review-action {
  margin-top: 16px;
}

@media (max-width: 900px) {
  .order-detail__summary {
    grid-template-columns: 1fr;
  }

  .order-detail__footer,
  .pay-summary__line {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
