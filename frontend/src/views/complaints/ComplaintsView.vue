<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { complaintApi, reservationApi, teaRoomApi } from '@/api/modules/management'
import { useAuthStore } from '@/stores/auth'
import type { ReservationRecord, StaffComplaintRecord, TeaRoomRecord } from '@/types/business'
import { usePagedTable } from '@/composables/usePagedTable'
import PageIntro from '@/components/shared/PageIntro.vue'
import { formatStatus } from '@/utils/status'

interface StatusForm {
  status: string
}

interface CreateForm {
  teaRoomId: number | null
  content: string
}

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const keyword = ref('')
const statusFilter = ref('')
const statusVisible = ref(false)
const createVisible = ref(false)
const createSubmitting = ref(false)
const statusFormRef = ref<FormInstance>()
const createFormRef = ref<FormInstance>()
const currentRow = ref<StaffComplaintRecord | null>(null)
const reservations = ref<ReservationRecord[]>([])
const teaRooms = ref<TeaRoomRecord[]>([])

const table = usePagedTable<StaffComplaintRecord>((params) => {
  const requestParams = { ...params }
  if (authStore.role === 'STAFF' && authStore.user?.id) requestParams.staffUserId = authStore.user.id
  return complaintApi.page(requestParams)
})

const canCreateComplaint = computed(() => authStore.role === 'USER')

const visibleRows = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return table.rows.filter((row) => {
    const matchQuery =
      !query ||
      [
        String(row.id),
        String(row.userId),
        String(row.teaRoomId),
        String(row.staffUserId),
        row.status,
        row.content,
        row.createdAt,
      ].some((item) => item.toLowerCase().includes(query))
    const matchStatus = !statusFilter.value || row.status === statusFilter.value
    return matchQuery && matchStatus
  })
})

const canUpdateStatus = computed(() => authStore.role === 'STAFF' || authStore.role === 'ADMIN')

const statusForm = reactive<StatusForm>({
  status: 'PENDING',
})

const createForm = reactive<CreateForm>({
  teaRoomId: null,
  content: '',
})

const statusRules: FormRules<StatusForm> = {
  status: [{ required: true, message: '请选择处理状态', trigger: 'change' }],
}

const createRules: FormRules<CreateForm> = {
  teaRoomId: [{ required: true, message: '请选择投诉关联茶室', trigger: 'change' }],
  content: [
    { required: true, message: '请输入投诉内容', trigger: 'blur' },
    { min: 10, message: '投诉内容至少 10 个字', trigger: 'blur' },
  ],
}

const complaintRoomOptions = computed(() => {
  const options = new Map<number, { id: number; label: string; reservationCount: number }>()
  for (const reservation of reservations.value) {
    if (reservation.status === 'CANCELLED') continue
    const current = options.get(reservation.teaRoomId)
    if (current) {
      current.reservationCount += 1
      continue
    }
    options.set(reservation.teaRoomId, {
      id: reservation.teaRoomId,
      label: roomName(reservation.teaRoomId),
      reservationCount: 1,
    })
  }
  return Array.from(options.values())
})

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
}

function roomRecord(id: number) {
  return teaRooms.value.find((item) => item.id === id) ?? null
}

function roomName(id: number) {
  return roomRecord(id)?.name ?? `茶室 #${id}`
}

function resetCreateForm() {
  createForm.teaRoomId = null
  createForm.content = ''
  createFormRef.value?.clearValidate()
}

function openCreateDialog() {
  resetCreateForm()
  createVisible.value = true
}

function openStatusDialog(row: StaffComplaintRecord) {
  currentRow.value = row
  statusForm.status = row.status
  statusVisible.value = true
}

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid || !createForm.teaRoomId || createSubmitting.value) return

  createSubmitting.value = true
  try {
    await complaintApi.create({
      teaRoomId: createForm.teaRoomId,
      content: createForm.content.trim(),
    })
    ElMessage.success('投诉已提交')
    createVisible.value = false
    await table.load({ page: 0 })
    await router.replace({ query: { ...route.query, action: undefined } })
  } finally {
    createSubmitting.value = false
  }
}

async function submitStatus() {
  const valid = await statusFormRef.value?.validate().catch(() => false)
  if (!valid || !currentRow.value) return
  await complaintApi.status(currentRow.value.id, statusForm)
  ElMessage.success('投诉状态已更新')
  statusVisible.value = false
  await table.load()
}

async function loadComplaintCreateContext() {
  if (!canCreateComplaint.value || !authStore.user?.id) return
  const [reservationPage, teaRoomPage] = await Promise.all([
    reservationApi.page({ page: 0, size: 200, userId: authStore.user.id }),
    teaRoomApi.rooms({ page: 0, size: 200 }),
  ])
  reservations.value = reservationPage.content
  teaRooms.value = teaRoomPage.content
}

onMounted(async () => {
  await Promise.all([table.load(), loadComplaintCreateContext()])
  if (canCreateComplaint.value && route.query.action === 'create') {
    openCreateDialog()
  }
})
</script>

<template>
  <div class="page-shell">
    <PageIntro title="投诉处理" description="支持按状态与关键字筛选投诉记录，并进行状态更新。"> </PageIntro>

    <div v-if="canCreateComplaint" class="section-card complaint-entry">
      <div class="section-card__body complaint-entry__body">
        <div>
          <div class="complaint-entry__eyebrow">用户投诉入口</div>
          <h3>遇到服务问题可直接发起投诉</h3>
          <p>请选择你预约过的茶室并填写投诉内容，提交后会同步通知管理员和对应茶室员处理。</p>
        </div>
        <el-button type="danger" @click="openCreateDialog">发起投诉</el-button>
      </div>
    </div>

    <div class="section-card">
      <div class="section-card__body">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="keyword" clearable placeholder="搜索 ID / 用户 / 茶室 / 内容" style="width: 320px" />
            <el-select v-model="statusFilter" clearable placeholder="状态" style="width: 160px">
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已完成" value="RESOLVED" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>

        <el-table v-loading="table.loading" :data="visibleRows" class="data-table">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="userId" label="用户ID" width="100" />
          <el-table-column prop="teaRoomId" label="茶室ID" width="100" />
          <el-table-column prop="staffUserId" label="茶室员ID" width="110" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              {{ formatStatus('complaint', row.status) }}
            </template>
          </el-table-column>
          <el-table-column prop="content" label="内容" min-width="260" />
          <el-table-column prop="createdAt" label="提交时间" width="180" />
          <el-table-column v-if="canUpdateStatus" label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="primary" @click="openStatusDialog(row)">更新状态</el-button>
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

    <el-dialog v-model="createVisible" title="提交投诉" width="520px" @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-position="top">
        <el-form-item label="关联茶室" prop="teaRoomId">
          <el-select v-model="createForm.teaRoomId" placeholder="请选择已预约的茶室" style="width: 100%">
            <el-option
              v-for="option in complaintRoomOptions"
              :key="option.id"
              :label="`${option.label} / 可投诉`"
              :value="option.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!complaintRoomOptions.length">
          <el-alert title="暂无可投诉茶室" type="info" :closable="false" description="只有预约过且未取消的茶室才支持提交投诉。" />
        </el-form-item>
        <el-form-item label="投诉内容" prop="content">
          <el-input
            v-model="createForm.content"
            type="textarea"
            :rows="5"
            maxlength="300"
            show-word-limit
            placeholder="请描述服务过程中的问题、发生时间和具体情况。"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" :disabled="!complaintRoomOptions.length" @click="submitCreate">
          提交投诉
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="更新投诉状态" width="420px">
      <el-form ref="statusFormRef" :model="statusForm" :rules="statusRules" label-position="top">
        <el-form-item label="处理状态" prop="status">
          <el-select v-model="statusForm.status" style="width: 100%">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已完成" value="RESOLVED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStatus">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.complaint-entry {
  margin-bottom: 18px;
}

.complaint-entry__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: linear-gradient(135deg, rgba(255, 243, 243, 0.96), rgba(255, 250, 245, 0.94));
}

.complaint-entry__eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #b42318;
  background: rgba(217, 45, 32, 0.1);
}

.complaint-entry h3 {
  margin: 12px 0 8px;
  font-size: 24px;
}

.complaint-entry p {
  margin: 0;
  color: var(--muted);
  line-height: 1.8;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

:deep(.toolbar .el-input__wrapper),
:deep(.toolbar .el-select__wrapper),
:deep(.toolbar .el-button) {
  min-height: 42px;
  font-size: 15px;
}

:deep(.data-table) {
  font-size: 16px;
}

:deep(.data-table .el-table__header th) {
  font-size: 16px;
}

:deep(.data-table .cell) {
  line-height: 26px;
  padding-top: 2px;
  padding-bottom: 2px;
}

:deep(.el-pagination) {
  margin-top: 18px;
  font-size: 15px;
}

@media (max-width: 768px) {
  .complaint-entry__body,
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    width: 100%;
  }
}
</style>
