
<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'
import dayjs from 'dayjs'
import { favoriteApi, fileApi, orderApi, reservationApi, teaApi, teaRoomApi } from '@/api/modules/management'
import { fetchRecommendations } from '@/api/modules/dashboard'
import { usePagedTable } from '@/composables/usePagedTable'
import PageIntro from '@/components/shared/PageIntro.vue'
import { useAuthStore } from '@/stores/auth'
import type { FavoriteRecord, RecommendationRecord, ReservationRecord, TeaRecord, TeaRoomRecord } from '@/types/business'
import { resolveAssetUrl } from '@/utils/asset'

interface TeaForm {
  name: string
  category: string
  price: number
  stock: number
  imageUrl: string
  description: string
  enabled: boolean
}

interface CartEntry {
  tea: TeaRecord
  quantity: number
}

const authStore = useAuthStore()
const canManage = computed(() => authStore.role === 'ADMIN')
const keyword = ref('')
const statusFilter = ref<string | ''>('')
const marketKeyword = ref('')
const categoryFilter = ref<string | ''>('')
const selectedReservationId = ref<number | null>(null)
const recommendation = ref<RecommendationRecord | null>(null)
const reservationList = ref<ReservationRecord[]>([])
const teaRooms = ref<TeaRoomRecord[]>([])
const favorites = ref<FavoriteRecord[]>([])
const quantityMap = reactive<Record<number, number>>({})
const cartState = reactive<Record<number, CartEntry>>({})
const submitting = ref(false)
const favoriteSubmittingId = ref<number | null>(null)

const table = usePagedTable<TeaRecord>((params) => teaApi.page(params), { size: authStore.role === 'USER' ? 5 : 5 })
const dialogVisible = ref(false)
const teaImageUploading = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive<TeaForm>({ name: '', category: '', price: 98, stock: 100, imageUrl: '', description: '', enabled: true })

const teaImage = (tea: TeaRecord) => resolveAssetUrl(tea.imageUrl) || 'https://images.unsplash.com/photo-1563822249548-9a72b6353cd1?auto=format&fit=crop&w=900&q=80'
const categories = computed(() => [...new Set(table.rows.map((item) => item.category).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'zh-CN')))
const selectedReservation = computed(() => reservationList.value.find((item) => item.id === selectedReservationId.value) ?? null)
const selectedRoom = computed(() => teaRooms.value.find((item) => item.id === selectedReservation.value?.teaRoomId) ?? null)
const recommendationIds = computed(() => new Set((recommendation.value?.recommendedTeas ?? []).map((item) => item.id)))
const favoriteMap = computed(() => new Map(favorites.value.map((item) => [`${item.targetType}:${item.targetId}`, item])))

const marketTeas = computed(() => {
  const query = marketKeyword.value.trim().toLowerCase()
  return table.rows
    .filter((row) => row.enabled && row.stock > 0)
    .filter((row) => {
      const matchQuery = !query || [row.name, row.category, row.description ?? '', row.imageUrl ?? ''].some((item) => item.toLowerCase().includes(query))
      const matchCategory = !categoryFilter.value || row.category === categoryFilter.value
      return matchQuery && matchCategory
    })
    .sort((left, right) => scoreTea(right) - scoreTea(left) || left.price - right.price || left.id - right.id)
})
const cartItems = computed(() => Object.values(cartState).filter((item) => item.quantity > 0).map(({ tea, quantity }) => ({ ...tea, quantity, subtotal: Number((tea.price * quantity).toFixed(2)) })))
const cartTotal = computed(() => Number(cartItems.value.reduce((sum, item) => sum + item.subtotal, 0).toFixed(2)))
const visibleRows = computed(() => {
  if (canManage.value) return table.rows
  const query = keyword.value.trim().toLowerCase()
  return table.rows.filter((row) => {
    const matchQuery = !query || [row.name, row.category, row.description ?? '', row.imageUrl ?? ''].some((item) => item.toLowerCase().includes(query))
    const matchStatus = !statusFilter.value || String(row.enabled) === statusFilter.value
    return matchQuery && matchStatus
  })
})
const reservationOptions = computed(() => reservationList.value.filter((item) => ['PENDING', 'CONFIRMED', 'USER_CHECKED_IN', 'STAFF_CONFIRMED_CHECK_IN', 'CHARGING'].includes(item.status)).sort((left, right) => dayjs(left.reservedStartTime).valueOf() - dayjs(right.reservedStartTime).valueOf()))

const rules: FormRules<TeaForm> = {
  name: [{ required: true, message: '请输入茶叶名称', trigger: 'blur' }],
  category: [{ required: true, message: '请输入分类', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'change' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'change' }],
}

const roomName = (id?: number | null) => !id ? '未关联茶室' : teaRooms.value.find((item) => item.id === id)?.name ?? `茶室 ${id}`
const favoriteEntry = (targetType: string, targetId: number) => favoriteMap.value.get(`${targetType}:${targetId}`) ?? null
const isFavoriteTea = (teaId: number) => Boolean(favoriteEntry('TEA', teaId))
function ensureQuantities() { for (const tea of table.rows) if (!(tea.id in quantityMap)) quantityMap[tea.id] = 0 }
function syncCartEntry(tea: TeaRecord, quantity: number) {
  quantityMap[tea.id] = quantity
  if (quantity > 0) {
    cartState[tea.id] = { tea, quantity }
    return
  }
  delete cartState[tea.id]
}
function syncCurrentPageCart() {
  for (const tea of table.rows) {
    if (cartState[tea.id]) cartState[tea.id] = { tea, quantity: cartState[tea.id].quantity }
  }
}
function scoreTea(tea: TeaRecord) { let score = 1; if (recommendationIds.value.has(tea.id)) score += 18; if (selectedReservationId.value) score += 3; if (selectedRoom.value?.description && tea.description?.includes(selectedRoom.value.description.slice(0, 2))) score += 1; score += Math.min(tea.stock, 200) / 100; score += Math.max(0, 6 - tea.price / 80); return score }
async function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  table.filters.keyword = undefined
  table.filters.enabled = undefined
  await table.load({ page: 0 })
}
function resetMarketFilters() { marketKeyword.value = ''; categoryFilter.value = '' }
function resetForm() { editingId.value = null; Object.assign(form, { name: '', category: '', price: 98, stock: 100, imageUrl: '', description: '', enabled: true }); formRef.value?.clearValidate() }
function openCreate() { resetForm(); dialogVisible.value = true }
function openEdit(row: TeaRecord) { editingId.value = row.id; Object.assign(form, { name: row.name, category: row.category, price: row.price, stock: row.stock, imageUrl: row.imageUrl ?? '', description: row.description ?? '', enabled: row.enabled }); dialogVisible.value = true }
async function submit() { const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; if (editingId.value) await teaApi.update(editingId.value, form); else await teaApi.create(form); ElMessage.success(editingId.value ? '茶叶已更新' : '茶叶已创建'); dialogVisible.value = false; await table.load(); ensureQuantities() }
async function handleTeaImageSelect(file: UploadRawFile) { if (!file.type?.startsWith('image/')) { ElMessage.error('只能上传图片文件'); return false } if (file.size / 1024 / 1024 > 8) { ElMessage.error('图片不能超过 8MB'); return false } teaImageUploading.value = true; try { form.imageUrl = await fileApi.uploadImage(file, 'teas'); ElMessage.success('茶叶图片已上传') } finally { teaImageUploading.value = false } return false }
function onTeaImageChange(file: UploadFile) { if (!file.raw) return; void handleTeaImageSelect(file.raw) }
async function removeTea(row: TeaRecord) { await ElMessageBox.confirm(`确认删除茶叶“${row.name}”吗？`, '删除茶叶', { type: 'warning' }); await teaApi.remove(row.id); ElMessage.success('茶叶已删除'); await table.load() }
async function refreshRecommendations() { if (authStore.role !== 'USER' || !authStore.user?.id) return; recommendation.value = await fetchRecommendations(authStore.user.id, selectedReservationId.value ?? undefined) }
async function loadStoreContext() {
  if (authStore.role !== 'USER' || !authStore.user?.id) return
  const [reservationPage, roomPage, favoritePage] = await Promise.all([
    reservationApi.page({ page: 0, size: 100, userId: authStore.user.id }),
    teaRoomApi.rooms({ page: 0, size: 100 }),
    favoriteApi.page({ page: 0, size: 200, userId: authStore.user.id }),
  ])
  reservationList.value = reservationPage.content ?? []
  teaRooms.value = roomPage.content ?? []
  favorites.value = favoritePage.content ?? []
  await refreshRecommendations()
}
async function toggleFavoriteTea(tea: TeaRecord) {
  if (!authStore.user?.id) return
  favoriteSubmittingId.value = tea.id
  try {
    const existing = favoriteEntry('TEA', tea.id)
    if (existing) {
      await favoriteApi.remove(existing.id)
      favorites.value = favorites.value.filter((item) => item.id !== existing.id)
      ElMessage.success('已取消收藏茶叶')
      return
    }
    const created = await favoriteApi.create({ userId: authStore.user.id, targetId: tea.id, targetType: 'TEA' })
    favorites.value = [...favorites.value, created]
    ElMessage.success('已收藏茶叶')
  } finally { favoriteSubmittingId.value = null }
}
async function submitOrder(items: Array<{ teaId: number; quantity: number }>) {
  if (!authStore.user?.id || !items.length) return
  submitting.value = true
  try {
    await orderApi.create({ userId: authStore.user.id, reservationId: selectedReservationId.value, items })
    ElMessage.success('订单已提交')
    for (const item of items) {
      quantityMap[item.teaId] = 0
      delete cartState[item.teaId]
    }
    await table.load({ page: 0 }); ensureQuantities(); await loadStoreContext()
  } finally { submitting.value = false }
}
const buyNow = async (tea: TeaRecord) => submitOrder([{ teaId: tea.id, quantity: Math.max(1, quantityMap[tea.id] || 1) }])
const submitCart = async () => submitOrder(cartItems.value.map((item) => ({ teaId: item.id, quantity: item.quantity })))
watch(selectedReservationId, async () => { if (authStore.role === 'USER') await refreshRecommendations() })
watch(() => table.rows, () => { ensureQuantities(); syncCurrentPageCart() }, { deep: true })
watch([keyword, statusFilter], async () => {
  if (!canManage.value) return
  table.filters.keyword = keyword.value.trim() || undefined
  table.filters.enabled = statusFilter.value === '' ? undefined : statusFilter.value === 'true'
  await table.load({ page: 0 })
})
onMounted(async () => { await table.load({ page: 0 }); ensureQuantities(); syncCurrentPageCart(); if (authStore.role === 'USER') await loadStoreContext() })
</script>

<template>
  <div class="page-shell">
    <template v-if="canManage">
      <PageIntro title="茶叶管理" description="维护茶叶图片、分类、价格和库存。"><el-button type="primary" @click="openCreate">新增茶叶</el-button></PageIntro>
      <div class="section-card"><div class="section-card__body">
        <div class="toolbar"><div class="toolbar-left"><el-input v-model="keyword" clearable placeholder="搜索名称、分类或描述" style="width:260px" /><el-select v-model="statusFilter" clearable placeholder="状态" style="width:140px"><el-option label="上架" value="true" /><el-option label="下架" value="false" /></el-select></div><div class="toolbar-right"><el-button @click="resetFilters">重置</el-button></div></div>
        <el-table v-loading="table.loading" :data="visibleRows" class="data-table">
          <el-table-column label="图片" width="110"><template #default="{ row }"><el-image :src="teaImage(row)" fit="cover" style="width:64px;height:64px;border-radius:12px" /></template></el-table-column>
          <el-table-column prop="name" label="茶叶名称" min-width="160" />
          <el-table-column prop="category" label="分类" width="120" />
          <el-table-column prop="price" label="价格" width="120" />
          <el-table-column prop="stock" label="库存" width="100" />
          <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '上架' : '下架' }}</el-tag></template></el-table-column>
          <el-table-column prop="description" label="描述" min-width="220" />
          <el-table-column label="操作" width="140"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button link type="danger" @click="removeTea(row)">删除</el-button></template></el-table-column>
        </el-table>
        <el-pagination
          class="list-pagination"
          background
          layout="total, sizes, prev, pager, next"
          :current-page="table.pager.page + 1"
          :page-size="table.pager.size"
          :total="table.pager.total"
          @current-change="table.handlePageChange"
          @size-change="table.handleSizeChange"
        />
      </div></div>
      <el-dialog v-model="dialogVisible" :title="editingId ? '编辑茶叶' : '新增茶叶'" width="640px" @closed="resetForm"><el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="form-grid"><el-form-item label="名称" prop="name"><el-input v-model="form.name" /></el-form-item><el-form-item label="分类" prop="category"><el-input v-model="form.category" /></el-form-item><el-form-item label="价格" prop="price"><el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" /></el-form-item><el-form-item label="库存" prop="stock"><el-input-number v-model="form.stock" :min="0" style="width:100%" /></el-form-item><el-form-item label="茶叶图片"><el-upload :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="onTeaImageChange"><el-button :loading="teaImageUploading">选择图片</el-button></el-upload></el-form-item><el-form-item label="上架"><el-switch v-model="form.enabled" /></el-form-item><el-form-item label="描述" class="span-2"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item><el-form-item v-if="form.imageUrl" label="图片预览" class="span-2"><el-image :src="form.imageUrl" fit="cover" style="width:100%;height:220px;border-radius:16px" /></el-form-item></el-form><template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></template></el-dialog>
    </template>
    <template v-else>
      <PageIntro title="茶叶商城" description="结合预约场景推荐茶叶，也可以直接下单购买。" />
      <section class="market-hero"><div><p class="eyebrow">Tea Selection</p><h2>看图选茶，更直观</h2><p class="market-hero__text">茶叶、分类、价格和推荐结果都集中在这里，支持直接收藏和下单。</p></div><div class="panel-card"><div class="panel-card__head"><h3>关联预约</h3><span>可选</span></div><el-select v-model="selectedReservationId" clearable placeholder="不关联预约，直接购买" style="width:100%"><el-option v-for="item in reservationOptions" :key="item.id" :label="`${roomName(item.teaRoomId)} / ${dayjs(item.reservedStartTime).format('MM-DD HH:mm')}`" :value="item.id" /></el-select><div v-if="selectedReservation && selectedRoom" class="reservation-brief"><strong>{{ selectedRoom.name }}</strong><span>{{ selectedRoom.location || '位置待补充' }}</span></div></div></section>
      <section class="market-layout">
        <div class="section-card"><div class="section-card__body"><div class="toolbar"><div class="toolbar-left"><el-input v-model="marketKeyword" clearable placeholder="搜索名称、分类或描述" style="width:280px" /><el-select v-model="categoryFilter" clearable placeholder="分类" style="width:160px"><el-option v-for="item in categories" :key="item" :label="item" :value="item" /></el-select></div><div class="toolbar-right"><el-button @click="resetMarketFilters">重置</el-button></div></div><div class="tea-grid"><article v-for="tea in marketTeas" :key="tea.id" class="tea-card"><div class="tea-card__image" :style="{ backgroundImage: `url(${teaImage(tea)})` }" /><div class="tea-card__top"><div><span v-if="recommendationIds.has(tea.id)" class="tea-card__badge">场景优先</span><h3>{{ tea.name }}</h3><p>{{ tea.description || '入口柔和，适合日常慢饮和会客分享。' }}</p></div><div class="tea-card__price">¥{{ tea.price }}</div></div><div class="tea-card__meta"><span>{{ tea.category }}</span><span>库存 {{ tea.stock }}</span></div><div class="tea-card__actions"><el-input-number :model-value="quantityMap[tea.id]" :min="0" :max="tea.stock" @update:model-value="syncCartEntry(tea, Number($event ?? 0))" /><el-button plain :loading="favoriteSubmittingId === tea.id" @click="toggleFavoriteTea(tea)">{{ isFavoriteTea(tea.id) ? '取消收藏' : '收藏茶叶' }}</el-button><el-button type="primary" :disabled="tea.stock < 1" @click="buyNow(tea)">立即购买</el-button></div></article></div><el-pagination v-if="table.pager.total > table.pager.size" class="list-pagination" background layout="total, sizes, prev, pager, next" :current-page="table.pager.page + 1" :page-size="table.pager.size" :total="table.pager.total" @current-change="table.handlePageChange" @size-change="table.handleSizeChange" /></div></div>
        <aside class="section-card"><div class="section-card__body"><div class="section-head"><div><h3>本次选购</h3><p>{{ selectedReservation ? `已关联 ${roomName(selectedReservation.teaRoomId)}` : '当前未关联预约' }}</p></div></div><div v-if="cartItems.length" class="cart-list"><div v-for="item in cartItems" :key="item.id" class="cart-item"><div><strong>{{ item.name }}</strong><p>{{ item.category }} / {{ item.quantity }} 份</p></div><div class="cart-item__right"><span>¥{{ item.subtotal }}</span><el-button link type="danger" @click="syncCartEntry(item, 0)">移除</el-button></div></div></div><el-empty v-else description="还没有选择茶叶" /><div class="cart-footer"><div><span>订单金额</span><strong>¥{{ cartTotal }}</strong></div><el-button type="primary" :loading="submitting" :disabled="!cartItems.length" @click="submitCart">提交订单</el-button></div></div></aside>
      </section>
    </template>
  </div>
</template>

<style scoped>
.toolbar,.toolbar-left,.toolbar-right{display:flex;align-items:center;gap:10px;flex-wrap:wrap}.toolbar{justify-content:space-between;margin-bottom:18px}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.span-2{grid-column:1/-1}.list-pagination{margin-top:22px;justify-content:flex-end}.market-hero{display:grid;grid-template-columns:minmax(0,1.7fr) 320px;gap:20px;padding:24px;border-radius:28px;background:linear-gradient(135deg,#183028 0%,#244439 48%,#f0e1c1 150%);color:#fff7eb;margin-bottom:22px}.eyebrow{margin:0 0 10px;text-transform:uppercase;letter-spacing:.2em;font-size:12px;color:rgba(255,247,235,.72)}.market-hero h2{margin:0;font-size:34px}.market-hero__text{margin:14px 0 0;line-height:1.8;color:rgba(255,247,235,.84)}.panel-card{padding:20px;border-radius:24px;background:rgba(255,249,240,.96);color:#193228}.panel-card__head{display:flex;justify-content:space-between;margin-bottom:12px}.reservation-brief{display:flex;flex-direction:column;gap:4px;margin-top:14px;padding:14px;border-radius:16px;background:#eff4ea}.market-layout{display:grid;grid-template-columns:minmax(0,1.6fr) 320px;gap:20px}.tea-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:14px}.tea-card{padding:16px;border-radius:20px;background:#fff;border:1px solid rgba(15,23,42,.08);box-shadow:0 14px 30px rgba(28,52,40,.08)}.tea-card__image{height:148px;margin:-16px -16px 14px;background-size:cover;background-position:center;border-radius:20px 20px 16px 16px}.tea-card__top{display:flex;align-items:flex-start;justify-content:space-between;gap:10px}.tea-card__badge{display:inline-flex;margin-bottom:10px;padding:4px 9px;border-radius:999px;background:#1e5b42;color:#fff9f0;font-size:12px}.tea-card h3{margin:0;font-size:18px}.tea-card p{margin:8px 0 0;color:#66756d;line-height:1.6;font-size:13px}.tea-card__price{font-size:20px;font-weight:700;color:#194d37;white-space:nowrap}.tea-card__meta{display:flex;gap:8px;flex-wrap:wrap;margin-top:14px}.tea-card__meta span{padding:5px 9px;border-radius:999px;background:#f3f0e7;color:#53655d;font-size:12px}.tea-card__actions{display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin-top:14px}.section-head{display:flex;justify-content:space-between;margin-bottom:18px}.section-head h3{margin:0;font-size:20px}.section-head p{margin:6px 0 0;color:var(--muted)}.cart-list{display:flex;flex-direction:column;gap:14px}.cart-item{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:14px 16px;border-radius:16px;background:#f7f7f1}.cart-item p{margin:6px 0 0;color:var(--muted)}.cart-footer{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-top:20px;padding-top:18px;border-top:1px solid rgba(15,23,42,.08)}.cart-footer strong{font-size:28px;color:#184b36}@media (max-width:1400px){.tea-grid{grid-template-columns:repeat(4,minmax(0,1fr))}}@media (max-width:1200px){.tea-grid{grid-template-columns:repeat(3,minmax(0,1fr))}}@media (max-width:980px){.market-hero,.market-layout,.form-grid{grid-template-columns:1fr}.list-pagination{justify-content:center}.tea-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media (max-width:720px){.tea-grid{grid-template-columns:1fr}.tea-card__top,.cart-footer{flex-direction:column;align-items:flex-start}}
</style>
