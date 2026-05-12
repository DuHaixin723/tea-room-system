<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import dayjs from 'dayjs'
import { orderApi, reservationApi, teaRoomApi } from '@/api/modules/management'
import { useAuthStore } from '@/stores/auth'
import { suppressHttpErrorsFor } from '@/api/http'
import type { OrderRecord, ReservationRecord, TeaRoomRecord } from '@/types/business'
import { usePagedTable } from '@/composables/usePagedTable'
import PageIntro from '@/components/shared/PageIntro.vue'
import { formatStatus } from '@/utils/status'

interface ReservationForm {
  userId: number
  teaRoomId: number | null
  reservedRange: [Date, Date] | []
  partySize: number
  remark: string
}

interface NoShowForm {
  orderNo: string
  reason: string
}

const authStore = useAuthStore()
const keyword = ref('')
const statusFilter = ref('')
const teaRooms = ref<TeaRoomRecord[]>([])
const orders = ref<OrderRecord[]>([])
const createVisible = ref(false)
const createFormRef = ref<FormInstance>()
const actingId = ref<number | null>(null)
const noShowVisible = ref(false)
const noShowSubmitting = ref(false)
const noShowTarget = ref<ReservationRecord | null>(null)
const noShowFormRef = ref<FormInstance>()
const noShowOrderLoading = ref(false)

// 表格数据会跟随当前角色切换查询条件，让同一页面兼容用户、茶室员和管理员。
const table = usePagedTable<ReservationRecord>((params) => {
  const requestParams = { ...params }
  if (authStore.role === 'USER' && authStore.user?.id) requestParams.userId = authStore.user.id
  if (authStore.role === 'STAFF' && authStore.user?.id) requestParams.staffUserId = authStore.user.id
  return reservationApi.page(requestParams)
}, { size: 3 })

const createForm = reactive<ReservationForm>({
  userId: authStore.user?.id ?? 0,
  teaRoomId: null,
  reservedRange: [],
  partySize: 1,
  remark: '',
})

const noShowForm = reactive<NoShowForm>({
  orderNo: '',
  reason: '',
})

const canCreate = computed(() => authStore.role === 'USER')
const isStaff = computed(() => authStore.role === 'STAFF')
const isUser = computed(() => authStore.role === 'USER')

// 这里只保留符合关键词和状态筛选条件的预约记录。
const visibleRows = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return table.rows.filter((row) => {
    const room = roomRecord(row.teaRoomId)
    const matchQuery =
      !query ||
      [
        String(row.id),
        String(row.userId),
        String(row.teaRoomId),
        room?.name ?? '',
        room?.location ?? '',
        String(row.partySize),
        row.remark ?? '',
        row.status,
        row.reservedStartTime,
        row.reservedEndTime,
      ].some((item) => item.toLowerCase().includes(query))
    const matchStatus = !statusFilter.value || row.status === statusFilter.value
    return matchQuery && matchStatus
  })
})

// 顶部统计卡片的数据来自筛选后的当前列表。
const reservationStats = computed(() => {
  const rows = visibleRows.value
  return {
    total: rows.length,
    active: rows.filter((row) => ['PENDING', 'CONFIRMED', 'USER_CHECKED_IN', 'STAFF_CONFIRMED_CHECK_IN', 'CHARGING'].includes(row.status)).length,
    finished: rows.filter((row) => row.status === 'COMPLETED').length,
    cancelled: rows.filter((row) => row.status === 'CANCELLED').length,
  }
})

const createRules: FormRules<ReservationForm> = {
  teaRoomId: [{ required: true, message: '请选择茶室', trigger: 'change' }],
  partySize: [{
    required: true,
    validator: (_rule, value, callback) => {
      const room = teaRooms.value.find((item) => item.id === createForm.teaRoomId)
      if (value == null || Number.isNaN(Number(value))) return callback(new Error('请填写到店人数'))
      const normalized = Number(value)
      if (!Number.isInteger(normalized) || normalized < 1) return callback(new Error('到店人数必须为大于 0 的整数'))
      if (room && normalized > room.capacity) return callback(new Error('到店人数不能超过茶室容量'))
      callback()
    },
    trigger: 'change',
  }],
  reservedRange: [{
    required: true,
    validator: (_rule, value, callback) => {
      if (!Array.isArray(value) || value.length !== 2 || !value[0] || !value[1]) return callback(new Error('请选择预约时间'))
      if (!dayjs(value[0]).isValid() || !dayjs(value[1]).isValid()) return callback(new Error('预约时间不合法，请重新选择'))
      if (!dayjs(value[1]).isAfter(dayjs(value[0]))) return callback(new Error('结束时间必须晚于开始时间'))
      if (!dayjs(value[0]).isAfter(dayjs())) return callback(new Error('预约开始时间必须晚于当前时间'))
      callback()
    },
    trigger: 'change',
  }],
}

const noShowRules: FormRules<NoShowForm> = {
  orderNo: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入爽约登记原因', trigger: 'blur' }],
}

function statusType(status: string) {
  if (status === 'NO_SHOW') return 'danger'
  if (status === 'CANCELLED') return 'info'
  if (status === 'COMPLETED') return 'success'
  if (status === 'CHARGING') return 'warning'
  if (status === 'USER_CHECKED_IN' || status === 'STAFF_CONFIRMED_CHECK_IN') return 'primary'
  return ''
}

function roomRecord(id: number) {
  return teaRooms.value.find((item) => item.id === id) ?? null
}

function roomName(id: number) {
  return roomRecord(id)?.name ?? `茶室 #${id}`
}

function roomLocation(id: number) {
  return roomRecord(id)?.location?.trim() || '位置待补充'
}

function reservationWindow(row: ReservationRecord) {
  return `${dayjs(row.reservedStartTime).format('YYYY-MM-DD HH:mm')} - ${dayjs(row.reservedEndTime).format('HH:mm')}`
}

function reservationDuration(row: ReservationRecord) {
  const start = dayjs(row.reservedStartTime)
  const end = dayjs(row.reservedEndTime)
  return `${end.diff(start, 'minute')} 分钟`
}

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
}

function resetCreateForm() {
  Object.assign(createForm, {
    userId: authStore.user?.id ?? 0,
    teaRoomId: null,
    reservedRange: [],
    partySize: 1,
    remark: '',
  })
  createFormRef.value?.clearValidate()
}

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid || !Array.isArray(createForm.reservedRange) || createForm.reservedRange.length !== 2) return
  try {
    suppressHttpErrorsFor(1500)
    await reservationApi.create({
      userId: createForm.userId,
      teaRoomId: createForm.teaRoomId,
      reservedStartTime: dayjs(createForm.reservedRange[0]).format('YYYY-MM-DDTHH:mm:ss'),
      reservedEndTime: dayjs(createForm.reservedRange[1]).format('YYYY-MM-DDTHH:mm:ss'),
      partySize: createForm.partySize,
      remark: createForm.remark,
    })
    ElMessage.success('预约已提交')
    createVisible.value = false
    await table.load({ page: 0 })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '预约提交失败，请检查填写信息后重试')
  }
}
function userActions(row: ReservationRecord) {
  const actions: Array<{ label: string; status: string; type?: 'primary' | 'danger' }> = []
  if (row.status === 'PENDING' || row.status === 'CONFIRMED') actions.push({ label: '取消预约', status: 'CANCELLED', type: 'danger' })
  if (row.status === 'CONFIRMED') actions.push({ label: '到店打卡', status: 'USER_CHECKED_IN', type: 'primary' })
  return actions
}

function staffActions(row: ReservationRecord) {
  const actions: Array<{ label: string; status: string; type?: 'primary' | 'danger' }> = []
  if (row.status === 'PENDING') actions.push({ label: '确认预约', status: 'CONFIRMED', type: 'primary' })
  if (row.status === 'USER_CHECKED_IN') actions.push({ label: '确认到店', status: 'STAFF_CONFIRMED_CHECK_IN', type: 'primary' })
  if (canRegisterNoShow(row)) actions.push({ label: '登记爽约', status: 'NO_SHOW', type: 'danger' })
  return actions
}

function canRegisterNoShow(row: ReservationRecord) {
  return isStaff.value
    && row.status === 'CONFIRMED'
    && dayjs().isAfter(dayjs(row.reservedStartTime).add(30, 'minute'))
}

function adminActions(row: ReservationRecord) {
  const actions: Array<{ label: string; status: string; type?: 'primary' | 'danger' }> = []
  if (row.status === 'STAFF_CONFIRMED_CHECK_IN') actions.push({ label: '进入收费', status: 'CHARGING', type: 'primary' })
  if (!['COMPLETED', 'CANCELLED'].includes(row.status)) actions.push({ label: '取消预约', status: 'CANCELLED', type: 'danger' })
  return actions
}

// 每一行可执行的按钮会跟随角色和当前预约状态变化。
function rowActions(row: ReservationRecord) {
  if (authStore.role === 'USER') return userActions(row)
  if (authStore.role === 'STAFF') return staffActions(row)
  return adminActions(row)
}

function roleTip(row: ReservationRecord) {
  if (isUser.value) {
    if (row.status === 'PENDING') return '已提交，等待茶室员确认。'
    if (row.status === 'CONFIRMED') return '已确认，可按预约时间到店打卡。'
    if (row.status === 'USER_CHECKED_IN') return '已打卡，等待茶室员确认到店。'
    return '可在这里持续跟踪预约处理进度。'
  }
  if (isStaff.value) {
    if (row.status === 'PENDING') return '这条预约正等待你确认。'
    if (row.status === 'USER_CHECKED_IN') return '顾客已到店，等待你确认。'
    return '这是分配给你名下茶室的预约记录。'
  }
  if (['STAFF_CONFIRMED_CHECK_IN', 'CHARGING'].includes(row.status)) {
    return '到达预约结束时间后，系统会自动将预约标记为已完成。'
  }
  return '管理员可继续推进预约状态流转。'
}

// 单独封装状态更新逻辑，让加载状态只落在当前操作的那一行。
async function updateStatus(row: ReservationRecord, status: string) {
  if (status === 'NO_SHOW') {
    openNoShow(row)
    return
  }
  actingId.value = row.id
  try {
    await reservationApi.status(row.id, { status })
    ElMessage.success('预约状态已更新')
    await table.load()
  } finally {
    actingId.value = null
  }
}

async function loadTeaRooms() {
  const data = await teaRoomApi.rooms({ page: 0, size: 200 })
  teaRooms.value = data.content
}

async function loadOrders() {
  if (authStore.role !== 'STAFF' && authStore.role !== 'ADMIN') {
    orders.value = []
    return
  }
  const data = await orderApi.page({ page: 0, size: 500 })
  orders.value = data.content ?? []
}

function noShowOrderOf(row: ReservationRecord) {
  const related = orders.value.filter((order) => order.reservationId === row.id)
  return related.find((order) => order.status === 'PAID')
    ?? related.find((order) => order.status === 'PENDING_PAYMENT')
    ?? related.find((order) => !['COMPLETED', 'CANCELLED', 'REFUNDED', 'NO_SHOW'].includes(order.status))
    ?? related[0]
    ?? null
}

function resetNoShowForm() {
  Object.assign(noShowForm, {
    orderNo: '',
    reason: '',
  })
  noShowTarget.value = null
  noShowFormRef.value?.clearValidate()
}

async function openNoShow(row: ReservationRecord) {
  noShowTarget.value = row
  if (!orders.value.length) {
    noShowOrderLoading.value = true
    try {
      await loadOrders()
    } finally {
      noShowOrderLoading.value = false
    }
  }
  const order = noShowOrderOf(row)
  noShowForm.orderNo = order?.orderNo ?? ''
  if (!order) {
    ElMessage.warning('未找到该预约关联订单，请先确认订单已生成')
  }
  noShowVisible.value = true
}

async function submitNoShow() {
  const valid = await noShowFormRef.value?.validate().catch(() => false)
  if (!valid || !noShowTarget.value) return
  if (!noShowForm.orderNo) {
    ElMessage.warning('未找到该预约关联订单，不能登记爽约')
    return
  }
  noShowSubmitting.value = true
  actingId.value = noShowTarget.value.id
  try {
    await reservationApi.noShow(noShowTarget.value.id, {
      orderNo: noShowForm.orderNo.trim(),
      reason: noShowForm.reason.trim(),
    })
    ElMessage.success('已登记爽约并释放预约资源')
    noShowVisible.value = false
    await table.load()
  } finally {
    noShowSubmitting.value = false
    actingId.value = null
  }
}

onMounted(async () => {
  await Promise.all([table.load(), loadTeaRooms(), loadOrders()])
})
</script>

<template>
  <div class="page-shell">
    <PageIntro
      title="预约中心"
      description="用户可以查看自己的预约进度，茶室员可以处理分配到自己茶室的预约，管理员可以继续推进收费与完成状态。"
    >
      <el-button v-if="canCreate" type="primary" @click="createVisible = true">新增预约</el-button>
    </PageIntro>

    <div class="reservation-overview">
      <div class="overview-card">
        <span>当前页预约</span>
        <strong>{{ reservationStats.total }}</strong>
      </div>
      <div class="overview-card">
        <span>进行中</span>
        <strong>{{ reservationStats.active }}</strong>
      </div>
      <div class="overview-card">
        <span>已完成</span>
        <strong>{{ reservationStats.finished }}</strong>
      </div>
      <div class="overview-card">
        <span>已取消</span>
        <strong>{{ reservationStats.cancelled }}</strong>
      </div>
    </div>

    <div class="section-card">
      <div class="section-card__body">
        <div class="toolbar-panel">
          <div class="toolbar-panel__head">
            <div>
              <strong>预约筛选</strong>
              <p>可按茶室、地点、用户、人数、备注和状态快速筛选当前页预约。</p>
            </div>
            <el-button @click="resetFilters">重置</el-button>
          </div>

          <div class="toolbar">
            <div class="toolbar-left">
              <el-input v-model="keyword" clearable placeholder="搜索预约号、茶室、地点、人数、备注" style="width: 360px" />
              <el-select v-model="statusFilter" clearable placeholder="状态筛选" style="width: 180px">
                <el-option label="待确认" value="PENDING" />
                <el-option label="已确认" value="CONFIRMED" />
                <el-option label="用户已打卡" value="USER_CHECKED_IN" />
                <el-option label="茶室员已确认" value="STAFF_CONFIRMED_CHECK_IN" />
                <el-option label="进行中" value="CHARGING" />
                <el-option label="已取消" value="CANCELLED" />
                <el-option label="已完成" value="COMPLETED" />
              </el-select>
            </div>
          </div>
        </div>

        <div v-if="visibleRows.length" class="reservation-list">
          <div v-for="row in visibleRows" :key="row.id" class="reservation-card">
            <div class="reservation-card__head">
              <div>
                <div class="reservation-card__title">
                  <strong>{{ roomName(row.teaRoomId) }}</strong>
                  <el-tag :type="statusType(row.status)" size="large">{{ formatStatus('reservation', row.status) }}</el-tag>
                </div>
                <div class="reservation-card__meta">
                  <span>预约 #{{ row.id }}</span>
                  <span v-if="authStore.role !== 'USER'">用户 ID {{ row.userId }}</span>
                  <span>{{ roomLocation(row.teaRoomId) }}</span>
                </div>
              </div>
              <div class="reservation-card__hint">{{ roleTip(row) }}</div>
            </div>

            <div class="reservation-card__grid">
              <div class="info-block">
                <label>预约时间</label>
                <strong>{{ reservationWindow(row) }}</strong>
                <span>{{ reservationDuration(row) }}</span>
              </div>
              <div class="info-block">
                <label>预约人数</label>
                <strong>{{ row.partySize }} 人</strong>
                <span>茶室：{{ roomName(row.teaRoomId) }}</span>
              </div>
              <div class="info-block info-block--wide">
                <label>备注信息</label>
                <strong>{{ row.remark?.trim() || '暂无备注' }}</strong>
                <span>状态流转：{{ formatStatus('reservation', row.status) }}</span>
              </div>
            </div>

            <div class="reservation-card__actions">
              <template v-if="rowActions(row).length">
                <el-button
                  v-for="action in rowActions(row)"
                  :key="action.status"
                  :type="action.type ?? 'primary'"
                  plain
                  :loading="actingId === row.id"
                  @click="updateStatus(row, action.status)"
                >
                  {{ action.label }}
                </el-button>
              </template>
              <span v-else class="muted">当前状态下没有可执行操作</span>
            </div>
          </div>
        </div>

        <div v-else class="empty-panel">
          <el-empty :image-size="86" description="当前没有符合条件的预约记录" />
          <p v-if="isStaff" class="empty-tip">茶室员只能看到分配给自己茶室的预约。如果这里为空，请先检查茶室是否绑定了当前茶室员。</p>
          <p v-else-if="isUser" class="empty-tip">你还没有预约记录，可以直接从这里新建预约。</p>
        </div>

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

    <el-dialog v-model="createVisible" title="新增预约" width="620px" @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="茶室" prop="teaRoomId">
          <el-select v-model="createForm.teaRoomId" placeholder="请选择茶室" style="width: 100%">
            <el-option
              v-for="item in teaRooms.filter((room) => room.enabled)"
              :key="item.id"
              :label="`${item.name} / ${item.location || '位置待补充'} / 容量 ${item.capacity} 人`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="预约人数" prop="partySize">
          <el-input-number v-model="createForm.partySize" :min="1" :max="99" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预约时间" prop="reservedRange">
          <el-date-picker
            v-model="createForm.reservedRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="4" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="noShowVisible" title="登记用户爽约" width="560px" @closed="resetNoShowForm">
      <div v-if="noShowTarget" class="no-show-summary">
        <strong>预约 #{{ noShowTarget.id }} · {{ roomName(noShowTarget.teaRoomId) }}</strong>
        <span>{{ reservationWindow(noShowTarget) }}</span>
        <span>登记后将释放茶室资源；如有关联茶叶订单，将仅释放茶叶库存，不进行退款。</span>
      </div>
      <el-form ref="noShowFormRef" class="no-show-form" :model="noShowForm" :rules="noShowRules" label-position="top">
        <el-form-item label="订单号">
          <el-input :model-value="noShowOrderLoading ? '正在获取订单号...' : (noShowForm.orderNo || '未找到关联订单')" readonly />
        </el-form-item>
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="noShowForm.orderNo" placeholder="请输入需要登记爽约的订单号" maxlength="64" />
        </el-form-item>
        <el-form-item label="登记原因" prop="reason">
          <el-input v-model="noShowForm.reason" type="textarea" :rows="4" maxlength="255" show-word-limit placeholder="请填写用户未到店打卡的说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noShowVisible = false">取消</el-button>
        <el-button type="danger" :loading="noShowSubmitting" @click="submitNoShow">确认登记爽约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.reservation-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.overview-card {
  padding: 18px 20px;
  border: 1px solid rgba(32, 96, 75, 0.1);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95), rgba(244, 249, 246, 0.92));
}

.overview-card span {
  display: block;
  color: var(--muted);
  font-size: 14px;
}

.overview-card strong {
  display: block;
  margin-top: 8px;
  color: #21493b;
  font-size: 30px;
  line-height: 1;
}

.toolbar-panel {
  margin-bottom: 18px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(244, 249, 246, 0.9);
  border: 1px solid rgba(32, 96, 75, 0.08);
}

.toolbar-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.toolbar-panel__head strong {
  font-size: 19px;
  color: #21493b;
}

.toolbar-panel__head p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 14px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.reservation-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.reservation-card {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(32, 96, 75, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 248, 0.94));
}

.reservation-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.reservation-card__title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.reservation-card__title strong {
  font-size: 20px;
  color: #21493b;
}

.reservation-card__meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
}

.reservation-card__hint {
  max-width: 320px;
  color: #5d7268;
  line-height: 1.7;
  text-align: right;
}

.reservation-card__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.info-block {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(244, 249, 246, 0.9);
}

.info-block--wide {
  grid-column: span 1;
}

.info-block label {
  display: block;
  color: var(--muted);
  font-size: 13px;
}

.info-block strong {
  display: block;
  margin-top: 8px;
  color: #21493b;
  font-size: 16px;
  line-height: 1.6;
}

.info-block span {
  display: block;
  margin-top: 6px;
  color: #60756c;
  line-height: 1.6;
}

.reservation-card__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.empty-panel {
  padding: 20px 0 10px;
}

.empty-tip {
  margin: 8px auto 0;
  max-width: 520px;
  color: var(--muted);
  text-align: center;
  line-height: 1.7;
}

.muted {
  color: var(--muted);
}

.no-show-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;
  padding: 16px 18px;
  border-radius: 16px;
  background: #fff7ed;
  color: #7c2d12;
  line-height: 1.6;
}

.no-show-summary strong {
  color: #7c2d12;
}

.no-show-form :deep(.el-form-item:nth-child(2)) {
  display: none;
}

:deep(.toolbar-panel .el-input__wrapper),
:deep(.toolbar-panel .el-select__wrapper),
:deep(.toolbar-panel .el-button) {
  min-height: 46px;
  font-size: 16px;
}

:deep(.el-pagination) {
  margin-top: 18px;
  font-size: 16px;
}

:deep(.el-form-item:has([prop='orderNo'])) {
  display: none;
}

@media (max-width: 1100px) {
  .reservation-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .reservation-card__grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .reservation-overview {
    grid-template-columns: 1fr;
  }

  .toolbar-panel__head,
  .toolbar,
  .reservation-card__head {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    width: 100%;
  }

  .reservation-card__hint {
    max-width: none;
    text-align: left;
  }
}
</style>
