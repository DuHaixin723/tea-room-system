<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import dayjs from 'dayjs'
import { useRoute, useRouter } from 'vue-router'
import { consultationApi, orderApi, reservationApi, teaApi, teaRoomApi } from '@/api/modules/management'
import { useAuthStore } from '@/stores/auth'
import { useConsultationStore } from '@/stores/consultation'
import type {
  ConsultationDetailRecord,
  ConsultationMessageRecord,
  ConsultationSessionRecord,
  OrderDetailRecord,
  OrderRecord,
  ReservationRecord,
  TeaRecord,
  TeaRoomRecord,
} from '@/types/business'
import { usePagedTable } from '@/composables/usePagedTable'
import PageIntro from '@/components/shared/PageIntro.vue'
import { getToken } from '@/utils/storage'
import { formatStatus } from '@/utils/status'

interface SessionForm {
  userId: number
  orderId: number | null
}

interface MessageForm {
  content: string
}

interface MentionCandidate {
  key: 'user' | 'admin' | 'staff'
  token: '@顾客' | '@管理员' | '@茶室员'
  label: string
}

const authStore = useAuthStore()
const consultationStore = useConsultationStore()
const route = useRoute()
const router = useRouter()
const table = usePagedTable<ConsultationSessionRecord>((params) => consultationApi.sessions(params))
const activeDetail = ref<ConsultationDetailRecord | null>(null)
const client = ref<Client | null>(null)
const connected = ref(false)
const sessionVisible = ref(false)
const sessionFormRef = ref<FormInstance>()
const messageFormRef = ref<FormInstance>()
const teaOptions = ref<TeaRecord[]>([])
const teaRoomOptions = ref<TeaRoomRecord[]>([])
const reservationOptions = ref<ReservationRecord[]>([])
const orderOptions = ref<OrderRecord[]>([])
const showClosedSessions = ref(false)
const sessionPage = ref(1)
const sessionPageSize = ref(5)
const handledRouteOrderKey = ref('')
let unsubscribe: (() => void) | null = null

const sessionForm = reactive<SessionForm>({
  userId: authStore.user?.id ?? 0,
  orderId: null,
})

const messageForm = reactive<MessageForm>({
  content: '',
})

const mentionAdminOnSend = ref(false)
const mentionStaffOnSend = ref(false)
const mentionActiveIndex = ref(0)

const canCreateSession = computed(() => authStore.role === 'USER' || authStore.role === 'STAFF')
const currentUserId = computed(() => authStore.user?.id ?? 0)
const currentRole = computed(() => authStore.role ?? '')
const isStaff = computed(() => currentRole.value === 'STAFF')
const isAdmin = computed(() => currentRole.value === 'ADMIN')
const displayedRows = computed(() => showClosedSessions.value ? table.rows : table.rows.filter((row) => row.status !== 'CLOSED'))
const pagedDisplayedRows = computed(() => {
  const start = (sessionPage.value - 1) * sessionPageSize.value
  return displayedRows.value.slice(start, start + sessionPageSize.value)
})
const pageDescription = computed(() => {
  if (isStaff.value) return '顾客、茶室员、管理员三端可互相 @ 提醒；关闭会话时可选择保留或删除聊天记录。'
  if (isAdmin.value) return '管理员可参与订单会话，接收 @管理员 提醒，并与顾客、茶室员双向沟通。'
  return '聊天会话绑定到订单，顾客、茶室员、管理员三端可互相 @。关闭时可保留或删除聊天记录。'
})

const sessionRules: FormRules<SessionForm> = {
  userId: [{ required: true, message: '缺少当前用户信息', trigger: 'change' }],
  orderId: [{ required: true, message: '请选择订单', trigger: 'change' }],
}

const messageRules: FormRules<MessageForm> = {
  content: [{ required: true, message: '请输入消息内容', trigger: 'blur' }],
}

const mentionCandidates = computed<MentionCandidate[]>(() => {
  const session = activeDetail.value?.session
  if (!session) return []

  const candidates: MentionCandidate[] = []
  if (session.userId && session.userId !== currentUserId.value) {
    candidates.push({
      key: 'user',
      token: '@顾客',
      label: session.userNickname || session.userUsername || '顾客',
    })
  }
  if (session.supportAdminUserId && session.supportAdminUserId !== currentUserId.value) {
    candidates.push({
      key: 'admin',
      token: '@管理员',
      label: session.supportAdminNickname || session.supportAdminUsername || '管理员',
    })
  }
  if (session.staffUserId && session.staffUserId !== currentUserId.value) {
    candidates.push({
      key: 'staff',
      token: '@茶室员',
      label: session.staffNickname || session.staffUsername || '茶室员',
    })
  }
  return candidates.filter((candidate, index, list) => list.findIndex((item) => item.key === candidate.key) === index)
})

const mentionMenuVisible = computed(() => mentionCandidates.value.length > 0 && /(^|\s)@[^\s@]*$/.test(messageForm.content))

function websocketUrl() {
  if (typeof window === 'undefined') return '/ws-consultation'
  const token = getToken()
  const qs = token ? `?token=${encodeURIComponent(token)}` : ''
  return `${window.location.origin}/ws-consultation${qs}`
}

function selectedTeaRoomName(teaRoomId?: number) {
  return teaRoomOptions.value.find((item) => item.id === teaRoomId)?.name ?? '未关联茶室'
}

function teaName(teaId?: number | null) {
  if (!teaId) return '未知茶品'
  return teaOptions.value.find((item) => item.id === teaId)?.name ?? `茶品 #${teaId}`
}

function reservationById(reservationId?: number | null) {
  if (!reservationId) return null
  return reservationOptions.value.find((item) => item.id === reservationId) ?? null
}

function orderById(orderId?: number | null) {
  return orderOptions.value.find((item) => item.id === orderId) ?? null
}

function orderLabel(orderId?: number | null, orderNo?: string) {
  const order = orderById(orderId)
  if (order?.orderNo) return order.orderNo
  return orderNo || (orderId ? `订单 #${orderId}` : '未关联订单')
}

function orderStatusLabel(order: OrderRecord) {
  return formatStatus('order', order.status)
}

function orderReservationSummary(order: OrderRecord) {
  const reservation = reservationById(order.reservationId)
  if (!reservation) return '商品订单 · 无预约信息'
  return `${selectedTeaRoomName(reservation.teaRoomId)} · ${dayjs(reservation.reservedStartTime).format('MM-DD HH:mm')} - ${dayjs(reservation.reservedEndTime).format('HH:mm')}`
}

function orderAmountLabel(order: OrderRecord) {
  return `金额 ${Number(order.amount ?? 0).toFixed(2)} 元`
}

function activeOrder() {
  return activeDetail.value?.order ?? null
}

function activeOrderItemCount() {
  return activeOrder()?.items.reduce((sum, item) => sum + Number(item.quantity ?? 0), 0) ?? 0
}

function activeOrderAmount() {
  const order = activeOrder()
  return order ? `¥${Number(order.order.amount ?? 0).toFixed(2)}` : '--'
}

function activeOrderSummary() {
  const order = activeOrder()
  if (!order) return '未加载订单信息'
  if (!order.items.length) return '当前会话对应茶室预约订单，无商品明细'
  return order.items.slice(0, 4).map((item) => `${teaName(item.teaId)} x${item.quantity}`).join(' · ')
}

function sessionTeaRoomMeta(teaRoomId?: number | null) {
  return teaRoomId ? `茶室：${selectedTeaRoomName(teaRoomId)}` : '商品订单 · 管理员直接处理'
}

function sessionBriefLabel(session: ConsultationSessionRecord) {
  return session.teaRoomId ? '茶室订单三方协作沟通' : '商品订单由管理员直接跟进处理'
}

function sessionItemMeta(session: ConsultationSessionRecord) {
  return session.teaRoomId ? '茶室预约订单' : `${activeOrderItemCount()} 件商品`
}

function displayUserName(nickname?: string, username?: string, fallbackId?: number) {
  return nickname || username || (fallbackId != null ? `用户 #${fallbackId}` : '未知用户')
}

function customerName(session: ConsultationSessionRecord) {
  return displayUserName(session.userNickname, session.userUsername, session.userId)
}

function staffName(session: ConsultationSessionRecord) {
  return displayUserName(session.staffNickname, session.staffUsername, session.staffUserId)
}

function adminName(session: ConsultationSessionRecord) {
  return displayUserName(
    session.supportAdminNickname || session.adminNickname,
    session.supportAdminUsername || session.adminUsername,
    session.supportAdminUserId || session.adminUserId,
  )
}

function participantSummaryLines(session: ConsultationSessionRecord) {
  const lines = [`顾客：${customerName(session)}`]
  if (session.staffUserId) lines.push(`茶室员：${staffName(session)}`)
  if (session.supportAdminUserId || session.adminUserId) lines.push(`管理员：${adminName(session)}`)
  return lines
}

function initialOf(name?: string) {
  return (name || 'U').slice(0, 1).toUpperCase()
}

function sessionAvatarUrl(session: ConsultationSessionRecord) {
  if (currentRole.value === 'USER') return session.supportAdminAvatarUrl || session.adminAvatarUrl || session.staffAvatarUrl
  return session.userAvatarUrl || session.staffAvatarUrl || session.supportAdminAvatarUrl || session.adminAvatarUrl
}

function sessionDisplayName(session: ConsultationSessionRecord) {
  if (currentRole.value === 'USER') {
    if (session.supportAdminUserId || session.adminUserId) return adminName(session)
    if (session.staffUserId) return staffName(session)
    return customerName(session)
  }
  return customerName(session)
}

function sessionRoleText(session: ConsultationSessionRecord) {
  if (currentRole.value === 'USER') {
    if (session.supportAdminUserId || session.adminUserId) return `管理员：${adminName(session)}`
    if (session.staffUserId) return `茶室员：${staffName(session)}`
    return '订单协作'
  }
  if (currentRole.value === 'STAFF') return `顾客：${customerName(session)}`
  return `顾客：${customerName(session)}`
}

function sessionMeta(session: ConsultationSessionRecord) {
  return `订单：${orderLabel(session.orderId, session.orderNo)}`
}

function extractMentionTokens(content: string) {
  const tokens: string[] = []
  if (content.includes('@顾客')) tokens.push('@顾客')
  if (content.includes('@管理员')) tokens.push('@管理员')
  if (content.includes('@茶室员')) tokens.push('@茶室员')
  return tokens
}

function messageSenderLabel(message: ConsultationMessageRecord) {
  return displayUserName(message.senderNickname, message.senderUsername, message.senderId)
}

function messageSenderAvatar(message: ConsultationMessageRecord) {
  return message.senderAvatarUrl || ''
}

function messageMentionLabel(message: ConsultationMessageRecord) {
  return extractMentionTokens(message.content).join(' ')
}

function applyMentionFlagsFromContent(content: string) {
  mentionAdminOnSend.value = content.includes('@管理员')
  mentionStaffOnSend.value = content.includes('@茶室员')
}

function selectMention(candidate: MentionCandidate) {
  messageForm.content = messageForm.content.replace(/(^|\s)@[^\s@]*$/, `$1${candidate.token} `)
  applyMentionFlagsFromContent(messageForm.content)
  mentionActiveIndex.value = 0
}

function moveMentionSelection(offset: number) {
  if (!mentionMenuVisible.value || mentionCandidates.value.length === 0) return
  const total = mentionCandidates.value.length
  mentionActiveIndex.value = (mentionActiveIndex.value + offset + total) % total
}

function confirmMentionSelection() {
  if (!mentionMenuVisible.value) return false
  const candidate = mentionCandidates.value[mentionActiveIndex.value]
  if (!candidate) return false
  selectMention(candidate)
  return true
}

function handleMessageKeydown(event: KeyboardEvent) {
  if (!mentionMenuVisible.value) return

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    moveMentionSelection(1)
    return
  }
  if (event.key === 'ArrowUp') {
    event.preventDefault()
    moveMentionSelection(-1)
    return
  }
  if (event.key === 'Enter') {
    event.preventDefault()
    confirmMentionSelection()
    return
  }
  if (event.key === 'Escape') {
    event.preventDefault()
    mentionActiveIndex.value = 0
    messageForm.content = messageForm.content.replace(/(^|\s)@[^\s@]*$/, '$1')
  }
}

async function loadTeaRooms() {
  const page = await teaRoomApi.rooms({ page: 0, size: 200 })
  teaRoomOptions.value = page.content ?? []
}

async function loadTeas() {
  const page = await teaApi.page({ page: 0, size: 200 })
  teaOptions.value = page.content ?? []
}

async function loadReservations() {
  const params: Record<string, number> = { page: 0, size: 200 }
  if (currentRole.value === 'USER' && currentUserId.value) params.userId = currentUserId.value
  if (currentRole.value === 'STAFF' && currentUserId.value) params.staffUserId = currentUserId.value
  const page = await reservationApi.page(params)
  reservationOptions.value = page.content ?? []
}

async function loadOrders() {
  if (!canCreateSession.value) return
  const params: Record<string, number> = { page: 0, size: 200 }
  if (currentRole.value === 'USER' && currentUserId.value) params.userId = currentUserId.value
  if (currentRole.value === 'STAFF' && currentUserId.value) params.staffUserId = currentUserId.value
  const page = await orderApi.page(params)
  orderOptions.value = page.content ?? []
}

function connectSocket() {
  const stomp = new Client({
    webSocketFactory: () => new SockJS(websocketUrl()),
    reconnectDelay: 3000,
    onConnect: () => {
      connected.value = true
      if (activeDetail.value) subscribeSession(activeDetail.value.session.id)
    },
    onStompError: () => {
      connected.value = false
      ElMessage.error('咨询通道连接失败')
    },
    onWebSocketClose: () => {
      connected.value = false
    },
  })

  stomp.activate()
  client.value = stomp
}

function subscribeSession(sessionId: number) {
  if (!client.value?.connected) return
  unsubscribe?.()
  const subscription = client.value.subscribe(`/topic/consultation/${sessionId}`, (frame) => {
    const message = JSON.parse(frame.body) as ConsultationMessageRecord
    if (!activeDetail.value || activeDetail.value.session.id !== sessionId) return
    activeDetail.value.messages = [...activeDetail.value.messages, message]
    consultationStore.markRead(sessionId)
  })
  unsubscribe = () => subscription.unsubscribe()
}

async function openSession(id: number) {
  const detail = await consultationApi.detail(id)
  let order: OrderDetailRecord | undefined
  if (detail.session.orderId) {
    order = await orderApi.detail(detail.session.orderId)
  }
  activeDetail.value = { ...detail, order }
  messageForm.content = ''
  mentionAdminOnSend.value = false
  mentionStaffOnSend.value = false
  consultationStore.setActiveSession(id)
  subscribeSession(id)
}

async function createSessionForOrder(orderId: number) {
  const order = orderById(orderId)
  if (!order) {
    ElMessage.error('未找到对应订单，无法发起会话')
    return
  }
  const result = await consultationApi.createSession({
    userId: order.userId,
    orderId,
    adminUserId: null,
  })

  ElMessage.success(isStaff.value ? '管理员协作会话已创建' : '订单会话已创建')
  sessionVisible.value = false
  sessionForm.orderId = null
  await Promise.all([table.load({ page: 0 }), consultationStore.refreshSessions()])
  await openSession(result.id)
}

async function createSession() {
  const valid = await sessionFormRef.value?.validate().catch(() => false)
  if (!valid || !sessionForm.orderId) return
  await createSessionForOrder(sessionForm.orderId)
}

async function closeSession(row: ConsultationSessionRecord) {
  try {
    await ElMessageBox.confirm(
      '关闭后会从当前列表隐藏。确定保留聊天记录，还是连同聊天记录一起删除？',
      '关闭会话',
      {
        confirmButtonText: '保留记录并关闭',
        cancelButtonText: '关闭并删除记录',
        distinguishCancelAndClose: true,
        type: 'warning',
      },
    )

    await consultationApi.closeSession(row.id)
    ElMessage.success('会话已关闭，聊天记录已保留')
} catch (error) {
    if (error === 'cancel') {
      await consultationApi.deleteSession(row.id)
      ElMessage.success('会话已关闭并删除聊天记录')
    } else if (error === 'close') {
      return
    } else {
      ElMessage.error('关闭会话失败，请稍后重试')
      return
    }
  }

  await Promise.all([table.load(), consultationStore.refreshSessions()])
  if (activeDetail.value?.session.id === row.id) {
    activeDetail.value = null
    consultationStore.setActiveSession(null)
  }
}

async function sendMessage() {
  const valid = await messageFormRef.value?.validate().catch(() => false)
  if (!valid || !activeDetail.value || !client.value?.connected) return

  client.value.publish({
    destination: '/app/consultation.send',
    body: JSON.stringify({
      sessionId: activeDetail.value.session.id,
      senderId: currentUserId.value,
      content: messageForm.content,
      mentionAdmin: mentionAdminOnSend.value,
      mentionStaff: mentionStaffOnSend.value,
    }),
  })

  messageForm.content = ''
  mentionAdminOnSend.value = false
  mentionStaffOnSend.value = false
  messageFormRef.value?.clearValidate()
}

function handleSessionPageChange(page: number) {
  sessionPage.value = page
}

function handleSessionPageSizeChange(size: number) {
  sessionPageSize.value = size
  sessionPage.value = 1
}

function routeOrderId() {
  const raw = Array.isArray(route.query.orderId) ? route.query.orderId[0] : route.query.orderId
  const parsed = Number(raw)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}

function routeAutoCreate() {
  const raw = Array.isArray(route.query.auto) ? route.query.auto[0] : route.query.auto
  return raw === '1' || raw === 'true'
}

async function clearRouteOrderQuery() {
  const nextQuery = { ...route.query }
  delete nextQuery.orderId
  delete nextQuery.auto
  await router.replace({ path: route.path, query: nextQuery })
}

async function handleRouteOrderEntry() {
  const orderId = routeOrderId()
  if (!orderId || !canCreateSession.value || orderOptions.value.length === 0) return

  const routeKey = `${orderId}:${routeAutoCreate()}`
  if (handledRouteOrderKey.value === routeKey) return

  const order = orderById(orderId)
  if (!order) return

  handledRouteOrderKey.value = routeKey
  sessionForm.userId = order.userId
  sessionForm.orderId = orderId

  if (routeAutoCreate()) {
    await createSessionForOrder(orderId)
  } else {
    sessionVisible.value = true
  }

  await clearRouteOrderQuery()
}

watch(
  () => sessionForm.orderId,
  (orderId) => {
    const order = orderById(orderId)
    sessionForm.userId = order?.userId ?? authStore.user?.id ?? 0
  },
)

watch(
  () => activeDetail.value?.session.id,
  (sessionId) => {
    consultationStore.setActiveSession(sessionId ?? null)
    if (sessionId) subscribeSession(sessionId)
  },
)

watch(
  () => messageForm.content,
  (content) => {
    applyMentionFlagsFromContent(content)
    mentionActiveIndex.value = 0
  },
)

watch(
  () => [route.query.orderId, route.query.auto, orderOptions.value.length],
  async () => {
    await handleRouteOrderEntry()
  },
)

watch(
  () => [showClosedSessions.value, table.rows.length],
  () => {
    sessionPage.value = 1
  },
)

onMounted(async () => {
  consultationStore.init()
  connectSocket()
  await Promise.all([loadTeas(), loadTeaRooms(), loadReservations(), loadOrders(), table.load(), consultationStore.refreshSessions()])
  await handleRouteOrderEntry()
})

onBeforeUnmount(() => {
  consultationStore.setActiveSession(null)
  unsubscribe?.()
  client.value?.deactivate()
})
</script>

<template>
  <div class="page-shell">
    <PageIntro title="订单会话" :description="pageDescription">
      <div class="toolbar-right">
        <el-tag :type="connected ? 'success' : 'danger'">{{ connected ? '实时通道已连接' : '实时通道未连接' }}</el-tag>
        <el-switch v-model="showClosedSessions" inline-prompt active-text="显示已关闭" inactive-text="隐藏已关闭" />
        <el-button v-if="canCreateSession" type="primary" @click="sessionVisible = true">
          {{ isStaff ? '发起管理员协作' : '发起订单会话' }}
        </el-button>
      </div>
    </PageIntro>

    <div class="consultation-grid">
      <div class="section-card">
        <div class="section-card__body">
          <div v-if="table.loading" class="empty-panel">会话加载中...</div>
          <div v-else-if="!displayedRows.length" class="empty-panel">当前没有会话</div>
          <div v-else class="session-list">
            <button
              v-for="row in pagedDisplayedRows"
              :key="row.id"
              type="button"
              class="session-card"
              :class="{ 'is-active': activeDetail?.session.id === row.id }"
              @click="openSession(row.id)"
            >
              <div class="session-card__avatar" :class="{ 'has-image': Boolean(sessionAvatarUrl(row)) }">
                <img v-if="sessionAvatarUrl(row)" :src="sessionAvatarUrl(row)" alt="avatar" />
                <span v-else>{{ initialOf(sessionDisplayName(row)) }}</span>
              </div>
              <div class="session-card__content">
                <div class="session-card__top">
                  <strong>{{ sessionDisplayName(row) }}</strong>
                  <el-tag size="small">{{ formatStatus('consultation', row.status) }}</el-tag>
                </div>
                <div class="session-card__role">{{ sessionRoleText(row) }}</div>
                <div class="session-card__meta">{{ sessionMeta(row) }}</div>
                <div class="session-card__meta">{{ sessionTeaRoomMeta(row.teaRoomId) }}</div>
              </div>
              <el-button v-if="row.status !== 'CLOSED'" link type="danger" @click.stop="closeSession(row)">关闭</el-button>
            </button>
          </div>
          <el-pagination
            v-if="displayedRows.length > sessionPageSize"
            class="list-pagination"
            background
            layout="total, sizes, prev, pager, next"
            :current-page="sessionPage"
            :page-size="sessionPageSize"
            :total="displayedRows.length"
            @current-change="handleSessionPageChange"
            @size-change="handleSessionPageSizeChange"
          />
        </div>
      </div>

      <div class="section-card">
        <div class="section-card__body">
          <div v-if="activeDetail" class="chat-panel">
            <div class="chat-panel__header">
              <div>
                <strong>{{ orderLabel(activeDetail.session.orderId, activeDetail.session.orderNo) }}</strong>
                <p>{{ sessionTeaRoomMeta(activeDetail.session.teaRoomId) }}</p>
              </div>
              <el-tag>{{ formatStatus('consultation', activeDetail.session.status) }}</el-tag>
            </div>

            <div class="chat-panel__summary">
              <span>会话 #{{ activeDetail.session.id }}</span>
              <span v-for="line in participantSummaryLines(activeDetail.session)" :key="line">{{ line }}</span>
            </div>

            <div v-if="activeDetail.order" class="order-brief">
              <div class="order-brief__main">
                <strong>{{ activeOrderSummary() }}</strong>
                <span>{{ sessionBriefLabel(activeDetail.session) }}</span>
              </div>
              <div class="order-brief__side">
                <strong>{{ activeOrderAmount() }}</strong>
                <span>{{ sessionItemMeta(activeDetail.session) }}</span>
              </div>
            </div>

            <div class="message-list">
              <div
                v-for="item in activeDetail.messages"
                :key="item.id"
                class="message-row"
                :class="{ 'is-self': item.senderId === currentUserId }"
              >
                <div class="message-avatar" :class="{ 'has-image': Boolean(messageSenderAvatar(item)) }">
                  <img v-if="messageSenderAvatar(item)" :src="messageSenderAvatar(item)" alt="avatar" />
                  <span v-else>{{ initialOf(messageSenderLabel(item)) }}</span>
                </div>
                <div class="message-bubble">
                  <div class="message-bubble__meta">
                    <strong>{{ messageSenderLabel(item) }}</strong>
                    <span>{{ item.createdAt }}</span>
                  </div>
                  <div v-if="messageMentionLabel(item)" class="mention-tag">{{ messageMentionLabel(item) }}</div>
                  <div class="message-bubble__content">{{ item.content }}</div>
                </div>
              </div>
            </div>

            <el-form ref="messageFormRef" :model="messageForm" :rules="messageRules">
              <el-form-item prop="content" class="message-editor">
                <el-input
                  v-model="messageForm.content"
                  type="textarea"
                  :rows="4"
                  :disabled="activeDetail.session.status === 'CLOSED' || !connected"
                  placeholder="输入消息内容，键入 @ 可选择提醒对象..."
                  @keydown="handleMessageKeydown"
                />
                <div v-if="mentionMenuVisible" class="mention-panel">
                  <button
                    v-for="(candidate, index) in mentionCandidates"
                    :key="candidate.key"
                    type="button"
                    class="mention-option"
                    :class="{ 'is-active': index === mentionActiveIndex }"
                    @mouseenter="mentionActiveIndex = index"
                    @click="selectMention(candidate)"
                  >
                    <span class="mention-option__token">{{ candidate.token }}</span>
                    <span class="mention-option__label">{{ candidate.label }}</span>
                  </button>
                </div>
              </el-form-item>
            </el-form>

            <div class="mention-toolbar">
              <span class="mention-hint">
                {{
                  extractMentionTokens(messageForm.content).length
                    ? `本条消息会提醒：${extractMentionTokens(messageForm.content).join('、')}`
                    : '三端都可互相 @，输入 @ 可选择顾客、茶室员或管理员。'
                }}
              </span>
              <el-button type="primary" :disabled="activeDetail.session.status === 'CLOSED' || !connected" @click="sendMessage">发送</el-button>
            </div>
          </div>

          <div v-else class="empty-panel">请选择左侧会话查看聊天内容</div>
        </div>
      </div>
    </div>

    <el-dialog v-model="sessionVisible" :title="isStaff ? '发起管理员协作' : '发起订单会话'" width="460px">
      <el-form ref="sessionFormRef" :model="sessionForm" :rules="sessionRules" label-position="top">
        <el-form-item :label="isStaff ? '顾客 ID' : '用户 ID'" prop="userId">
          <el-input :model-value="sessionForm.userId" disabled />
        </el-form-item>
        <el-form-item label="选择订单" prop="orderId">
          <el-select v-model="sessionForm.orderId" placeholder="请选择订单" style="width: 100%">
            <el-option
              v-for="item in orderOptions"
              :key="item.id"
              :label="`${item.orderNo} / ${orderStatusLabel(item)}`"
              :value="item.id"
            >
              <div class="order-option">
                <div class="order-option__top">
                  <strong>{{ item.orderNo }}</strong>
                  <span>{{ orderStatusLabel(item) }}</span>
                </div>
                <div class="order-option__meta">{{ orderReservationSummary(item) }}</div>
                <div class="order-option__meta">{{ orderAmountLabel(item) }}</div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="会话说明">
          <el-input :model-value="isStaff ? '会话将绑定到订单，并把管理员纳入协作提醒链路。' : '会话将绑定到订单，并自动关联该订单对应的服务茶室员。'" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sessionVisible = false">取消</el-button>
        <el-button type="primary" @click="createSession">创建会话</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.consultation-grid {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 20px;
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.session-card {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px;
  border: 1px solid rgba(32, 96, 75, 0.1);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(244, 249, 246, 0.92));
  text-align: left;
}

.session-card.is-active {
  border-color: rgba(32, 96, 75, 0.24);
  box-shadow: 0 16px 32px rgba(32, 96, 75, 0.1);
}

.session-card__avatar,
.message-avatar {
  display: grid;
  place-items: center;
  overflow: hidden;
  flex: 0 0 auto;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(32, 96, 75, 0.16), rgba(220, 176, 111, 0.24));
  color: #21493b;
  font-weight: 800;
}

.session-card__avatar {
  width: 54px;
  height: 54px;
  font-size: 22px;
}

.message-avatar {
  width: 44px;
  height: 44px;
  font-size: 18px;
}

.session-card__avatar img,
.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.session-card__content {
  flex: 1;
  min-width: 0;
}

.session-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.session-card__top strong {
  color: #21493b;
  font-size: 16px;
}

.session-card__role {
  margin-top: 4px;
  color: #2e6a56;
  font-size: 13px;
  font-weight: 700;
}

.session-card__meta {
  margin-top: 4px;
  color: var(--muted);
  font-size: 13px;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 640px;
}

.chat-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(32, 96, 75, 0.08);
}

.chat-panel__header strong {
  font-size: 18px;
  color: #21493b;
}

.chat-panel__header p,
.chat-panel__summary {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.chat-panel__summary {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.order-brief {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(145deg, rgba(244, 249, 246, 0.96), rgba(255, 249, 241, 0.94));
}

.order-brief__main,
.order-brief__side {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.order-brief__main strong,
.order-brief__side strong {
  color: #21493b;
}

.order-brief__main span,
.order-brief__side span {
  color: var(--muted);
  font-size: 13px;
}

.order-brief__side {
  align-items: flex-end;
  text-align: right;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 420px;
  overflow: auto;
  padding-right: 4px;
}

.message-row {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message-row.is-self {
  flex-direction: row-reverse;
}

.message-bubble {
  max-width: min(75%, 620px);
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(244, 249, 246, 0.92);
}

.message-row.is-self .message-bubble {
  background: rgba(32, 96, 75, 0.1);
}

.message-bubble__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--muted);
  font-size: 12px;
}

.message-bubble__content {
  margin-top: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  color: #21493b;
}

.mention-tag {
  display: inline-flex;
  margin-top: 8px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(218, 82, 82, 0.12);
  color: #b94848;
  font-size: 12px;
  font-weight: 700;
}

.message-editor {
  margin-bottom: 0;
}

.mention-panel {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mention-option {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid rgba(32, 96, 75, 0.12);
  border-radius: 14px;
  background: #fff;
}

.mention-option.is-active {
  border-color: rgba(32, 96, 75, 0.24);
  background: rgba(244, 249, 246, 0.92);
}

.mention-option__token {
  color: #2e6a56;
  font-weight: 700;
}

.mention-option__label,
.mention-hint {
  color: var(--muted);
  font-size: 13px;
}

.mention-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.empty-panel {
  display: grid;
  place-items: center;
  min-height: 240px;
  color: var(--muted);
}

.list-pagination {
  margin-top: 18px;
  justify-content: flex-end;
}

.order-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 0;
}

.order-option__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.order-option__top strong {
  color: #21493b;
  font-size: 14px;
}

.order-option__top span,
.order-option__meta {
  color: var(--muted);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 1100px) {
  .consultation-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .message-bubble {
    max-width: 100%;
  }

  .mention-toolbar,
  .chat-panel__summary,
  .order-brief {
    flex-direction: column;
    align-items: flex-start;
  }

  .order-brief__side {
    align-items: flex-start;
    text-align: left;
  }

  .list-pagination {
    justify-content: center;
  }
}
</style>
