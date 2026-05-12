
<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'
import dayjs from 'dayjs'
import { useRoute } from 'vue-router'
import { favoriteApi, fileApi, reservationApi, teaRoomApi, usersApi } from '@/api/modules/management'
import { useAuthStore } from '@/stores/auth'
import { suppressHttpErrorsFor } from '@/api/http'
import { resolveAssetUrl } from '@/utils/asset'
import type {
  FavoriteRecord,
  ReservationAvailabilityRecord,
  ReservationTimeSlotRecord,
  TeaRoomRecord,
  TeaRoomTypeRecord,
  UserRecord,
} from '@/types/business'
import { usePagedTable } from '@/composables/usePagedTable'
import PageIntro from '@/components/shared/PageIntro.vue'

interface TeaRoomForm {
  typeId: number
  staffUserId: number | undefined
  name: string
  capacity: number
  enabled: boolean
  location: string
  imageUrl: string
  businessStartTime: string
  businessEndTime: string
  description: string
}

interface RoomTypeForm {
  name: string
  description: string
  basePrice: number
  pricingMode: 'PER_ROOM' | 'PER_PERSON'
  minCapacity: number
  maxCapacity: number
}

interface BookingForm {
  teaRoomId: number | null
  reservedRange: [Date, Date] | []
  partySize: number
  remark: string
}

const authStore = useAuthStore()
const route = useRoute()
const roomKeyword = ref('')
const typeKeyword = ref('')
const statusFilter = ref<string | ''>('')
const activeTab = ref<'rooms' | 'types'>('rooms')
const roomPage = ref(1)
const roomPageSize = ref(4)
const typePage = ref(1)
const typePageSize = ref(8)
const availability = ref<ReservationAvailabilityRecord | null>(null)
const availabilityLoading = ref(false)
const favorites = ref<FavoriteRecord[]>([])
const favoriteSubmittingId = ref<number | null>(null)

const canManageRooms = computed(() => authStore.role === 'ADMIN')
const canBook = computed(() => authStore.role === 'USER')
const showTypeTab = computed(() => authStore.role !== 'USER')
const showStaffColumn = computed(() => authStore.role === 'ADMIN')

const roomTable = usePagedTable<TeaRoomRecord>((params) => {
  const requestParams = { ...params }
  if (authStore.role === 'STAFF' && authStore.user?.id) requestParams.staffUserId = authStore.user.id
  return teaRoomApi.rooms(requestParams)
}, { size: 200 })
const typeTable = usePagedTable<TeaRoomTypeRecord>((params) => teaRoomApi.roomTypes(params), { size: 200 })

const roomDialog = ref(false)
const typeDialog = ref(false)
const bookingDialog = ref(false)
const roomImageUploading = ref(false)
const roomFormRef = ref<FormInstance>()
const typeFormRef = ref<FormInstance>()
const bookingFormRef = ref<FormInstance>()
const editingRoomId = ref<number | null>(null)
const editingTypeId = ref<number | null>(null)
const staffOptions = ref<UserRecord[]>([])

const roomForm = reactive<TeaRoomForm>({
  typeId: 1,
  staffUserId: undefined,
  name: '',
  capacity: 6,
  enabled: true,
  location: '',
  imageUrl: '',
  businessStartTime: '09:00:00',
  businessEndTime: '22:00:00',
  description: '',
})
const typeForm = reactive<RoomTypeForm>({
  name: '',
  description: '',
  basePrice: 168,
  pricingMode: 'PER_ROOM',
  minCapacity: 2,
  maxCapacity: 6,
})
const bookingForm = reactive<BookingForm>({
  teaRoomId: null,
  reservedRange: [],
  partySize: 1,
  remark: '',
})

const pricingModeLabel = (mode?: 'PER_ROOM' | 'PER_PERSON') => mode === 'PER_PERSON' ? '按人数收费' : '按包厢收费'
const pricingModeHint = (mode?: 'PER_ROOM' | 'PER_PERSON') => mode === 'PER_PERSON' ? '公共茶室按人数和预约时长计费，预估金额会随人数与时长变化。' : '包厢按房间和预约时长计费，预估金额会随时长变化。'
const formatAmount = (value?: number | null) => typeof value === 'number' ? `${value.toFixed(2)}` : '--'
const roomTypeName = (typeId: number) => typeTable.rows.find((item) => item.id === typeId)?.name ?? `类型 ${typeId}`
const roomTypeById = (typeId: number) => typeTable.rows.find((item) => item.id === typeId) ?? null
const roomImage = (room: TeaRoomRecord) => resolveAssetUrl(room.imageUrl) || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1200&q=80'
const formatBusinessTime = (value?: string) => value?.slice(0, 5) || '--:--'
const roomBusinessHours = (room: TeaRoomRecord) => `${formatBusinessTime(room.businessStartTime)} - ${formatBusinessTime(room.businessEndTime)}`
const staffUsername = (staffUserId?: number | null) => {
  if (!staffUserId) return '未分配'
  return staffOptions.value.find((item) => item.id === staffUserId)?.username ?? String(staffUserId)
}

const roomVisibleRows = computed(() => {
  const query = roomKeyword.value.trim().toLowerCase()
  return roomTable.rows.filter((row) => {
    const matchQuery = !query || [row.name, row.location ?? '', row.description ?? '', roomTypeName(row.typeId)].some((item) => item.toLowerCase().includes(query))
    const matchStatus = !statusFilter.value || String(row.enabled) === statusFilter.value
    return matchQuery && matchStatus
  })
})

const typeVisibleRows = computed(() => {
  const query = typeKeyword.value.trim().toLowerCase()
  if (!query) return typeTable.rows
  return typeTable.rows.filter((row) => [row.name, row.description ?? '', pricingModeLabel(row.pricingMode)].some((item) => item.toLowerCase().includes(query)))
})

const pagedRoomRows = computed(() => {
  const start = (roomPage.value - 1) * roomPageSize.value
  return roomVisibleRows.value.slice(start, start + roomPageSize.value)
})

const pagedTypeRows = computed(() => {
  const start = (typePage.value - 1) * typePageSize.value
  return typeVisibleRows.value.slice(start, start + typePageSize.value)
})

const typeOptions = computed(() => typeTable.rows.map((item) => ({
  label: `${item.name} / ¥${item.basePrice} / ${pricingModeLabel(item.pricingMode)} / ${item.minCapacity}-${item.maxCapacity}人`,
  value: item.id,
})))
const selectedRoomType = computed(() => typeTable.rows.find((item) => item.id === roomForm.typeId) ?? null)
const selectedBookingRoom = computed(() => roomTable.rows.find((item) => item.id === bookingForm.teaRoomId) ?? null)
const selectedBookingType = computed(() => selectedBookingRoom.value ? roomTypeById(selectedBookingRoom.value.typeId) : null)
const unavailableSlots = computed(() => availability.value?.unavailableSlots ?? [])
const suggestedSlots = computed(() => availability.value?.suggestedSlots ?? [])
const favoriteMap = computed(() => new Map(favorites.value.map((item) => [`${item.targetType}:${item.targetId}`, item])))

const roomRules: FormRules<TeaRoomForm> = {
  typeId: [{ required: true, message: '请选择茶室类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入茶室名称', trigger: 'blur' }],
  capacity: [{ required: true, message: '请输入茶室容量', trigger: 'change' }],
  businessStartTime: [{ required: true, message: '请选择营业开始时间', trigger: 'change' }],
  businessEndTime: [{ required: true, message: '请选择营业结束时间', trigger: 'change' }],
}
const typeRules: FormRules<RoomTypeForm> = {
  name: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
  basePrice: [{ required: true, message: '请输入基础价格', trigger: 'change' }],
  minCapacity: [{ required: true, message: '请输入最少人数', trigger: 'change' }],
  maxCapacity: [{ required: true, message: '请输入最多人数', trigger: 'change' }],
}
const bookingRules: FormRules<BookingForm> = {
  teaRoomId: [{ required: true, message: '请选择茶室', trigger: 'change' }],
  partySize: [{ required: true, validator: bookingPartySizeValidator, trigger: 'change' }],
  reservedRange: [{ required: true, validator: bookingRangeValidator, trigger: 'change' }],
}

const favoriteEntry = (targetType: string, targetId: number) => favoriteMap.value.get(`${targetType}:${targetId}`) ?? null
const isFavoriteRoom = (roomId: number) => Boolean(favoriteEntry('TEA_ROOM', roomId))
const disabledBookingDate = (date: Date) => dayjs(date).endOf('day').isBefore(dayjs())
const formatSlot = (slot: ReservationTimeSlotRecord) => `${dayjs(slot.startTime).format('MM-DD HH:mm')} - ${dayjs(slot.endTime).format('HH:mm')}`
function bookingRangeValidator(_rule: unknown, value: BookingForm['reservedRange'], callback: (error?: Error) => void) {
  if (!Array.isArray(value) || value.length !== 2 || !value[0] || !value[1]) {
    callback(new Error('请选择预约时间'))
    return
  }

  const [start, end] = value
  const startTime = dayjs(start)
  const endTime = dayjs(end)

  if (!startTime.isValid() || !endTime.isValid()) {
    callback(new Error('预约时间不合法，请重新选择'))
    return
  }
  if (!endTime.isAfter(startTime)) {
    callback(new Error('结束时间必须晚于开始时间'))
    return
  }
  if (!startTime.isAfter(dayjs())) {
    callback(new Error('预约开始时间必须晚于当前时间'))
    return
  }

  callback()
}
function bookingPartySizeValidator(_rule: unknown, value: number, callback: (error?: Error) => void) {
  const room = selectedBookingRoom.value
  if (value == null || Number.isNaN(Number(value))) {
    callback(new Error('请填写到店人数'))
    return
  }

  const normalized = Number(value)
  if (!Number.isInteger(normalized) || normalized < 1) {
    callback(new Error('到店人数必须为大于 0 的整数'))
    return
  }
  if (room && normalized > room.capacity) {
    callback(new Error('到店人数不能超过茶室容量'))
    return
  }

  callback()
}
function bookingDurationMinutes() {
  if (!Array.isArray(bookingForm.reservedRange) || bookingForm.reservedRange.length !== 2) return 120
  const [start, end] = bookingForm.reservedRange
  const diff = dayjs(end).diff(dayjs(start), 'minute')
  return diff > 0 ? diff : 120
}
const bookingDurationLabel = computed(() => {
  const minutes = bookingDurationMinutes()
  const hours = Math.floor(minutes / 60)
  const remainMinutes = minutes % 60
  if (hours > 0 && remainMinutes > 0) return `${hours}小时${remainMinutes}分钟`
  if (hours > 0) return `${hours}小时`
  return `${minutes}分钟`
})

function syncRoomCapacityWithType() {
  const type = selectedRoomType.value
  if (!type) return
  if (roomForm.capacity < type.minCapacity) roomForm.capacity = type.minCapacity
  if (roomForm.capacity > type.maxCapacity) roomForm.capacity = type.maxCapacity
}

function syncBookingPartySizeWithRoom() {
  const room = selectedBookingRoom.value
  const type = selectedBookingType.value
  if (!room) return
  if (bookingForm.partySize < 1) bookingForm.partySize = 1
  if (type?.pricingMode === 'PER_PERSON' && bookingForm.partySize > room.capacity) bookingForm.partySize = room.capacity
}

async function loadAvailability() {
  if (!bookingForm.teaRoomId) {
    availability.value = null
    return
  }
  availabilityLoading.value = true
  try {
    availability.value = await reservationApi.availability({
      teaRoomId: bookingForm.teaRoomId,
      start: dayjs().startOf('hour').format('YYYY-MM-DDTHH:mm:ss'),
      end: dayjs().add(7, 'day').endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
      durationMinutes: bookingDurationMinutes(),
      partySize: bookingForm.partySize,
    })
  } finally {
    availabilityLoading.value = false
  }
}

async function loadFavorites() {
  if (!canBook.value || !authStore.user?.id) return
  const page = await favoriteApi.page({ page: 0, size: 200, userId: authStore.user.id })
  favorites.value = page.content ?? []
}

function resetRoomForm() {
  const fallbackType = typeTable.rows[0]
  editingRoomId.value = null
  Object.assign(roomForm, {
    typeId: fallbackType?.id ?? 1,
    staffUserId: undefined,
    name: '',
    capacity: fallbackType?.maxCapacity ?? 6,
    enabled: true,
    location: '',
    imageUrl: '',
    businessStartTime: '09:00:00',
    businessEndTime: '22:00:00',
    description: '',
  })
  roomFormRef.value?.clearValidate()
}
function resetTypeForm() {
  editingTypeId.value = null
  Object.assign(typeForm, { name: '', description: '', basePrice: 168, pricingMode: 'PER_ROOM', minCapacity: 2, maxCapacity: 6 })
  typeFormRef.value?.clearValidate()
}
function resetBookingForm() {
  Object.assign(bookingForm, { teaRoomId: null, reservedRange: [], partySize: 1, remark: '' })
  availability.value = null
  bookingFormRef.value?.clearValidate()
}
function openEditRoom(row: TeaRoomRecord) {
  editingRoomId.value = row.id
  Object.assign(roomForm, {
    typeId: row.typeId,
    staffUserId: row.staffUserId,
    name: row.name,
    capacity: row.capacity,
    enabled: row.enabled,
    location: row.location ?? '',
    imageUrl: row.imageUrl ?? '',
    businessStartTime: row.businessStartTime ?? '09:00:00',
    businessEndTime: row.businessEndTime ?? '22:00:00',
    description: row.description ?? '',
  })
  roomDialog.value = true
}
function openEditType(row: TeaRoomTypeRecord) {
  editingTypeId.value = row.id
  Object.assign(typeForm, { name: row.name, description: row.description ?? '', basePrice: row.basePrice, pricingMode: row.pricingMode, minCapacity: row.minCapacity, maxCapacity: row.maxCapacity })
  typeDialog.value = true
}
async function openBooking(row?: TeaRoomRecord) {
  resetBookingForm()
  bookingForm.teaRoomId = row?.id ?? null
  syncBookingPartySizeWithRoom()
  bookingDialog.value = true
  await loadAvailability()
}
async function toggleFavoriteRoom(room: TeaRoomRecord) {
  if (!authStore.user?.id) return
  favoriteSubmittingId.value = room.id
  try {
    const existing = favoriteEntry('TEA_ROOM', room.id)
    if (existing) {
      await favoriteApi.remove(existing.id)
      favorites.value = favorites.value.filter((item) => item.id !== existing.id)
      ElMessage.success('已取消收藏茶室')
    } else {
      const created = await favoriteApi.create({ userId: authStore.user.id, targetId: room.id, targetType: 'TEA_ROOM' })
      favorites.value = [...favorites.value, created]
      ElMessage.success('已收藏茶室')
    }
  } finally {
    favoriteSubmittingId.value = null
  }
}
async function submitRoom() {
  const valid = await roomFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (editingRoomId.value) await teaRoomApi.updateRoom(editingRoomId.value, roomForm)
  else await teaRoomApi.createRoom(roomForm)
  ElMessage.success(editingRoomId.value ? '茶室已更新' : '茶室已创建')
  roomDialog.value = false
  await roomTable.load()
}
async function handleRoomImageSelect(file: UploadRawFile) {
  if (!file.type?.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (file.size / 1024 / 1024 > 8) {
    ElMessage.error('图片不能超过 8MB')
    return false
  }
  roomImageUploading.value = true
  try {
    roomForm.imageUrl = await fileApi.uploadImage(file, 'tea-rooms')
    ElMessage.success('茶室图片已上传')
  } finally {
    roomImageUploading.value = false
  }
  return false
}
function onRoomImageChange(file: UploadFile) {
  if (!file.raw) return
  void handleRoomImageSelect(file.raw)
}
async function submitType() {
  const valid = await typeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (editingTypeId.value) await teaRoomApi.updateRoomType(editingTypeId.value, typeForm)
  else await teaRoomApi.createRoomType(typeForm)
  ElMessage.success(editingTypeId.value ? '茶室类型已更新' : '茶室类型已创建')
  typeDialog.value = false
  await typeTable.load()
}
async function submitBooking() {
  const valid = await bookingFormRef.value?.validate().catch(() => false)
  if (!valid || !authStore.user?.id || !Array.isArray(bookingForm.reservedRange) || bookingForm.reservedRange.length !== 2) return
  try {
    suppressHttpErrorsFor(1500)
    await reservationApi.create({
      userId: authStore.user.id,
      teaRoomId: bookingForm.teaRoomId,
      reservedStartTime: dayjs(bookingForm.reservedRange[0]).format('YYYY-MM-DDTHH:mm:ss'),
      reservedEndTime: dayjs(bookingForm.reservedRange[1]).format('YYYY-MM-DDTHH:mm:ss'),
      partySize: bookingForm.partySize,
      remark: bookingForm.remark,
    })
    ElMessage.success('预约已提交')
    bookingDialog.value = false
  } catch (error: any) {
    const backendMessage = typeof error?.response?.data?.message === 'string' ? error.response.data.message.trim() : ''
    const fallbackMessage = error instanceof Error ? error.message.trim() : ''
    const isStatusOnlyMessage = /^Request failed with status code \d+$/i.test(fallbackMessage)
    const message = backendMessage || (!isStatusOnlyMessage ? fallbackMessage : '')
    if (message) ElMessage.error(message)
  }
}
async function removeRoom(row: TeaRoomRecord) {
  await ElMessageBox.confirm(`确认删除茶室“${row.name}”吗？`, '删除茶室', { type: 'warning' })
  await teaRoomApi.removeRoom(row.id)
  ElMessage.success('茶室已删除')
  await roomTable.load()
}
async function initLoad() {
  const [staffPage] = await Promise.all([
    canManageRooms.value ? usersApi.page({ page: 0, size: 100, role: 'STAFF' }) : Promise.resolve({ content: [] as UserRecord[] }),
    roomTable.load(),
    typeTable.load(),
    loadFavorites(),
  ])
  staffOptions.value = staffPage.content ?? []
  syncRoomCapacityWithType()
}

function handleRoomPageChange(page: number) {
  roomPage.value = page
}

function handleRoomPageSizeChange(size: number) {
  roomPageSize.value = size
  roomPage.value = 1
}

function handleTypePageChange(page: number) {
  typePage.value = page
}

function handleTypePageSizeChange(size: number) {
  typePageSize.value = size
  typePage.value = 1
}

watch(() => roomForm.typeId, syncRoomCapacityWithType)
watch(() => bookingForm.teaRoomId, async () => { syncBookingPartySizeWithRoom(); if (bookingDialog.value) await loadAvailability() })
watch(() => bookingForm.partySize, async () => { if (bookingDialog.value) await loadAvailability() })
watch(() => bookingForm.reservedRange, async (value) => { if (bookingDialog.value && Array.isArray(value) && value.length === 2) await loadAvailability() }, { deep: true })
watch(() => [route.query.bookRoomId, roomVisibleRows.value.length], () => {
  if (!canBook.value) return
  const rawId = route.query.bookRoomId
  const roomId = typeof rawId === 'string' ? Number(rawId) : Number.NaN
  if (!roomId || Number.isNaN(roomId)) return
  const target = roomVisibleRows.value.find((item) => item.id === roomId && item.enabled)
  if (target) void openBooking(target)
}, { immediate: true })

onMounted(initLoad)
</script>
<template>
  <div class="page-shell">
    <PageIntro :title="authStore.role === 'ADMIN' ? '茶室管理' : '茶室空间'" :description="authStore.role === 'USER' ? '查看茶室图片、预约规则和预估金额。' : '维护茶室与茶室类型。'" />

    <div class="section-card">
      <div class="section-card__body">
        <div class="toolbar">
          <el-tabs v-if="showTypeTab" v-model="activeTab"><el-tab-pane label="茶室列表" name="rooms" /><el-tab-pane label="茶室类型" name="types" /></el-tabs>
          <div class="toolbar-right">
            <el-button v-if="activeTab === 'rooms' && canManageRooms" type="primary" @click="resetRoomForm(); roomDialog = true">新增茶室</el-button>
            <el-button v-if="activeTab === 'types' && canManageRooms" type="primary" @click="resetTypeForm(); typeDialog = true">新增类型</el-button>
          </div>
        </div>

        <template v-if="activeTab === 'rooms'">
          <div class="room-panel" :class="{ 'room-panel--manage': !canBook }">
            <div class="toolbar room-toolbar">
              <div class="toolbar-left">
                <el-input v-model="roomKeyword" clearable placeholder="搜索名称、位置或类型" style="width: 280px" />
                <el-select v-model="statusFilter" clearable placeholder="状态" style="width: 140px"><el-option label="营业中" value="true" /><el-option label="已停用" value="false" /></el-select>
              </div>
            </div>

            <div class="room-grid" :class="{ 'room-grid--user': canBook, 'room-grid--manage': !canBook }">
              <article v-for="row in pagedRoomRows" :key="row.id" class="room-card" :class="{ 'room-card--manage': !canBook }">
                <button
                  v-if="canBook"
                  type="button"
                  class="room-image room-image-button"
                  :style="{ backgroundImage: `url(${roomImage(row)})` }"
                  :disabled="!row.enabled"
                  :title="row.enabled ? `预约 ${row.name}` : `${row.name} 当前不可预约`"
                  @click="openBooking(row)"
                />
                <div v-else class="room-image" :style="{ backgroundImage: `url(${roomImage(row)})` }" />
                <div class="room-body">
                  <div class="room-head"><h3>{{ row.name }}</h3><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '营业中' : '已停用' }}</el-tag></div>
                  <div class="room-meta">
                    <span>{{ roomTypeName(row.typeId) }}</span>
                    <span>容量 {{ row.capacity }} 人</span>
                    <span>{{ row.location || '位置待补充' }}</span>
                    <span>基础价 ¥{{ roomTypeById(row.typeId)?.basePrice ?? '--' }}</span>
                    <span>{{ pricingModeLabel(roomTypeById(row.typeId)?.pricingMode) }}</span>
                    <span>营业时间 {{ roomBusinessHours(row) }}</span>
                    <span v-if="showStaffColumn">负责人 {{ staffUsername(row.staffUserId) }}</span>
                  </div>
                  <p class="room-desc">{{ row.description || '适合品茶、会客和小型私享活动。' }}</p>
                  <div class="room-actions">
                    <template v-if="canBook">
                      <el-button plain :loading="favoriteSubmittingId === row.id" @click="toggleFavoriteRoom(row)">{{ isFavoriteRoom(row.id) ? '取消收藏' : '收藏茶室' }}</el-button>
                      <el-button type="primary" :disabled="!row.enabled" @click="openBooking(row)">立即预约</el-button>
                    </template>
                    <template v-if="canManageRooms">
                      <el-button link type="primary" @click="openEditRoom(row)">编辑</el-button>
                      <el-button link type="danger" @click="removeRoom(row)">删除</el-button>
                    </template>
                  </div>
                </div>
              </article>
            </div>
            <el-pagination
              v-if="roomVisibleRows.length > roomPageSize"
              class="list-pagination"
              background
              layout="total, sizes, prev, pager, next"
              :current-page="roomPage"
              :page-size="roomPageSize"
              :total="roomVisibleRows.length"
              @current-change="handleRoomPageChange"
              @size-change="handleRoomPageSizeChange"
            />
          </div>
        </template>

        <template v-else>
          <div class="room-panel room-panel--manage">
            <div class="toolbar room-toolbar"><el-input v-model="typeKeyword" clearable placeholder="搜索类型或计费方式" style="width: 320px" /></div>
            <el-table v-loading="typeTable.loading" :data="pagedTypeRows" class="data-table">
              <el-table-column prop="name" label="类型名称" min-width="160" />
              <el-table-column label="基础价格" width="120"><template #default="{ row }">¥{{ row.basePrice }}</template></el-table-column>
              <el-table-column label="计费模式" width="140"><template #default="{ row }">{{ pricingModeLabel(row.pricingMode) }}</template></el-table-column>
              <el-table-column label="人数范围" width="140"><template #default="{ row }">{{ row.minCapacity }} - {{ row.maxCapacity }} 人</template></el-table-column>
              <el-table-column prop="description" label="说明" min-width="220" />
              <el-table-column v-if="canManageRooms" label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click="openEditType(row)">编辑</el-button></template></el-table-column>
            </el-table>
            <el-pagination
              v-if="typeVisibleRows.length > typePageSize"
              class="list-pagination"
              background
              layout="total, sizes, prev, pager, next"
              :current-page="typePage"
              :page-size="typePageSize"
              :total="typeVisibleRows.length"
              @current-change="handleTypePageChange"
              @size-change="handleTypePageSizeChange"
            />
          </div>
        </template>
      </div>
    </div>

    <el-dialog v-model="roomDialog" :title="editingRoomId ? '编辑茶室' : '新增茶室'" width="720px" @closed="resetRoomForm">
      <el-form ref="roomFormRef" :model="roomForm" :rules="roomRules" label-position="top" class="form-grid">
        <el-form-item label="茶室名称" prop="name"><el-input v-model="roomForm.name" /></el-form-item>
        <el-form-item label="茶室类型" prop="typeId"><el-select v-model="roomForm.typeId" style="width: 100%"><el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="负责人"><el-select v-model="roomForm.staffUserId" clearable style="width: 100%"><el-option v-for="item in staffOptions" :key="item.id" :label="`${item.nickname} / ${item.username}`" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="容量" prop="capacity"><el-input-number v-model="roomForm.capacity" :min="selectedRoomType?.minCapacity ?? 1" :max="selectedRoomType?.maxCapacity ?? 99" style="width: 100%" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="roomForm.location" /></el-form-item>
        <el-form-item label="茶室图片">
          <el-upload :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="onRoomImageChange">
            <el-button :loading="roomImageUploading">选择图片</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="营业开始时间" prop="businessStartTime">
          <el-time-select
            v-model="roomForm.businessStartTime"
            start="06:00"
            step="00:30"
            end="23:30"
            format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="营业结束时间" prop="businessEndTime">
          <el-time-select
            v-model="roomForm.businessEndTime"
            start="06:30"
            step="00:30"
            end="23:59"
            format="HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="启用"><el-switch v-model="roomForm.enabled" /></el-form-item>
        <el-form-item label="描述" class="span-2"><el-input v-model="roomForm.description" type="textarea" :rows="4" /></el-form-item>
        <el-form-item v-if="roomForm.imageUrl" label="图片预览" class="span-2"><img :src="roomForm.imageUrl" alt="茶室图片" class="preview" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="roomDialog = false">取消</el-button><el-button type="primary" @click="submitRoom">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="typeDialog" :title="editingTypeId ? '编辑茶室类型' : '新增茶室类型'" width="620px" @closed="resetTypeForm">
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-position="top" class="form-grid">
        <el-form-item label="类型名称" prop="name"><el-input v-model="typeForm.name" /></el-form-item>
        <el-form-item label="基础价格" prop="basePrice"><el-input-number v-model="typeForm.basePrice" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="计费模式"><el-select v-model="typeForm.pricingMode" style="width: 100%"><el-option label="按包厢收费" value="PER_ROOM" /><el-option label="按人数收费" value="PER_PERSON" /></el-select></el-form-item>
        <el-form-item label="最少人数" prop="minCapacity"><el-input-number v-model="typeForm.minCapacity" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="最多人数" prop="maxCapacity"><el-input-number v-model="typeForm.maxCapacity" :min="typeForm.minCapacity || 1" style="width: 100%" /></el-form-item>
        <el-form-item label="说明" class="span-2"><el-input v-model="typeForm.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="typeDialog = false">取消</el-button><el-button type="primary" @click="submitType">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="bookingDialog" title="预约茶室" width="900px" @closed="resetBookingForm">
      <div class="booking-layout">
        <el-form ref="bookingFormRef" :model="bookingForm" :rules="bookingRules" label-position="top">
          <el-form-item label="茶室" prop="teaRoomId"><el-select v-model="bookingForm.teaRoomId" style="width: 100%"><el-option v-for="item in roomVisibleRows.filter((room) => room.enabled)" :key="item.id" :label="`${item.name} / ${item.location || '位置待补充'} / 容量 ${item.capacity} 人`" :value="item.id" /></el-select></el-form-item>
          <el-form-item :label="selectedBookingType?.pricingMode === 'PER_PERSON' ? '预约人数' : '到店人数'" prop="partySize"><el-input-number v-model="bookingForm.partySize" :min="1" :max="selectedBookingType?.pricingMode === 'PER_PERSON' ? (selectedBookingRoom?.capacity ?? 99) : 999" style="width: 100%" /></el-form-item>
          <el-form-item label="预约时间" prop="reservedRange"><el-date-picker v-model="bookingForm.reservedRange" type="datetimerange" range-separator="至" start-placeholder="开始时间" end-placeholder="结束时间" :disabled-date="disabledBookingDate" style="width: 100%" /></el-form-item>
          <el-form-item label="备注"><el-input v-model="bookingForm.remark" type="textarea" :rows="4" /></el-form-item>
        </el-form>
        <div class="availability-card">
          <h3>预约参考</h3>
          <p v-if="selectedBookingType">{{ pricingModeHint(availability?.pricingMode || selectedBookingType.pricingMode) }}</p>
          <div class="summary-grid">
            <div><span>营业时间</span><strong>{{ selectedBookingRoom ? roomBusinessHours(selectedBookingRoom) : '--' }}</strong></div>
            <div><span>计费方式</span><strong>{{ pricingModeLabel(availability?.pricingMode || selectedBookingType?.pricingMode) }}</strong></div>
            <div><span>本次时长</span><strong>{{ bookingDurationLabel }}</strong></div>
            <div><span>预估金额</span><strong class="price">{{ formatAmount(availability?.estimatedAmount) }}</strong></div>
            <div><span>茶室容量</span><strong>{{ availability?.teaRoomCapacity ?? selectedBookingRoom?.capacity ?? '--' }} 人</strong></div>
            <div><span>本次人数</span><strong>{{ availability?.requestedPartySize ?? bookingForm.partySize }} 人</strong></div>
          </div>
          <el-skeleton v-if="availabilityLoading" :rows="4" animated />
          <template v-else>
            <div class="slot-section"><div class="slot-title">推荐时段</div><div class="slot-list"><button v-for="slot in suggestedSlots" :key="`${slot.startTime}-${slot.endTime}`" type="button" class="slot-chip free" @click="bookingForm.reservedRange = [dayjs(slot.startTime).toDate(), dayjs(slot.endTime).toDate()]"><strong>{{ formatSlot(slot) }}</strong><span>{{ slot.label }}</span></button></div><el-empty v-if="!suggestedSlots.length" description="暂无推荐时段" /></div>
            <div class="slot-section"><div class="slot-title">已占用时段</div><div class="slot-list"><div v-for="slot in unavailableSlots" :key="`${slot.startTime}-${slot.endTime}`" class="slot-chip busy"><strong>{{ formatSlot(slot) }}</strong><span>{{ slot.label }}</span></div></div><el-empty v-if="!unavailableSlots.length" description="暂无占用记录" /></div>
          </template>
        </div>
      </div>
      <template #footer><el-button @click="bookingDialog = false">取消</el-button><el-button type="primary" @click="submitBooking">提交预约</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar,.toolbar-left,.toolbar-right{display:flex;align-items:center;gap:10px;flex-wrap:wrap}.toolbar{justify-content:space-between}.room-toolbar{margin:0 0 14px}.room-panel{width:100%}.room-panel--manage{max-width:none}.room-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(360px,1fr));gap:18px;align-items:start}.room-grid--user,.room-grid--manage{grid-template-columns:repeat(auto-fill,minmax(360px,1fr))}.room-card{overflow:hidden;border-radius:20px;background:#fff;border:1px solid rgba(15,23,42,.08);box-shadow:0 12px 28px rgba(15,23,42,.05);max-width:440px;width:100%;justify-self:start}.room-card--manage{display:block;min-height:unset}.room-image{height:210px;background-size:cover;background-position:center;background-color:#f4efe6}.room-image-button{display:block;width:100%;border:none;padding:0;cursor:pointer;transition:transform .18s ease,box-shadow .18s ease}.room-image-button:hover:not(:disabled){transform:scale(1.015)}.room-image-button:focus-visible{outline:2px solid #3b82f6;outline-offset:-2px}.room-image-button:disabled{cursor:not-allowed;opacity:.75}.room-card--manage .room-image{height:210px;min-height:210px}.room-body{padding:16px 16px 14px}.room-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.room-head h3{margin:0;font-size:18px;line-height:1.35}.room-meta{display:flex;gap:8px;flex-wrap:wrap;margin-top:10px}.room-meta span{padding:5px 10px;border-radius:999px;background:#f3efe7;color:#5b635b;font-size:12px;line-height:1.4}.room-desc{margin:10px 0 0;color:#5d655f;line-height:1.65;font-size:14px}.room-actions{display:flex;gap:12px;flex-wrap:wrap;margin-top:12px}.list-pagination{margin-top:22px;justify-content:flex-end}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.span-2{grid-column:1/-1}.preview{display:block;width:100%;max-height:260px;object-fit:cover;border-radius:18px}.booking-layout{display:grid;grid-template-columns:minmax(0,1fr) 320px;gap:18px}.availability-card{padding:18px;border-radius:22px;background:linear-gradient(180deg,#fffdf8,#f6f0e8);border:1px solid rgba(15,23,42,.08)}.availability-card h3{margin:0 0 8px}.availability-card p{margin:0 0 16px;color:var(--muted);line-height:1.7}.summary-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px;margin-bottom:16px}.summary-grid span{display:block;color:var(--muted);font-size:12px}.summary-grid strong{display:block;margin-top:6px}.summary-grid .price{font-size:24px;color:#16523b}.slot-section+.slot-section{margin-top:16px}.slot-title{margin-bottom:10px;font-weight:700}.slot-list{display:flex;flex-direction:column;gap:10px}.slot-chip{display:flex;flex-direction:column;gap:4px;padding:12px;border-radius:14px;text-align:left}.slot-chip.free{background:rgba(64,158,255,.08);border:1px solid rgba(64,158,255,.2)}.slot-chip.busy{background:rgba(245,108,108,.08);border:1px solid rgba(245,108,108,.2)}@media (max-width:960px){.room-grid,.room-grid--user,.room-grid--manage,.booking-layout,.form-grid,.summary-grid,.room-card--manage{grid-template-columns:1fr}.room-card{max-width:none}.room-card--manage .room-image{min-height:210px}.list-pagination{justify-content:center}}
</style>
