<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { reportApi, teaRoomApi } from '@/api/modules/management'
import { useAuthStore } from '@/stores/auth'
import type { ReportRecord, TeaRoomRecord } from '@/types/business'
import { usePagedTable } from '@/composables/usePagedTable'
import PageIntro from '@/components/shared/PageIntro.vue'
import { formatStatus } from '@/utils/status'

interface ReportForm {
  teaRoomId: number | null
  reportedBy: number
  title: string
  content: string
}

interface StatusForm {
  status: string
}

const authStore = useAuthStore()
const keyword = ref('')
const statusFilter = ref('')
const teaRooms = ref<TeaRoomRecord[]>([])
const createVisible = ref(false)
const statusVisible = ref(false)
const createFormRef = ref<FormInstance>()
const statusFormRef = ref<FormInstance>()
const currentRow = ref<ReportRecord | null>(null)

const table = usePagedTable<ReportRecord>((params) => {
  const requestParams = { ...params }
  if (authStore.role === 'STAFF' && authStore.user?.id) requestParams.staffUserId = authStore.user.id
  return reportApi.page(requestParams)
})

const visibleRows = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return table.rows.filter((row) => {
    const matchQuery =
      !query ||
      [row.title, row.content, row.status, String(row.teaRoomId), String(row.reportedBy)].some((item) =>
        item.toLowerCase().includes(query),
      )
    const matchStatus = !statusFilter.value || row.status === statusFilter.value
    return matchQuery && matchStatus
  })
})

const createForm = reactive<ReportForm>({
  teaRoomId: null,
  reportedBy: authStore.user?.id ?? 0,
  title: '',
  content: '',
})

const statusForm = reactive<StatusForm>({
  status: 'PENDING',
})

const canCreate = computed(() => authStore.role === 'STAFF' || authStore.role === 'ADMIN')
const canUpdateStatus = computed(() => authStore.role === 'ADMIN')

const rules: FormRules<ReportForm> = {
  teaRoomId: [{ required: true, message: '请选择茶室', trigger: 'change' }],
  title: [
    { required: true, message: '请输入报障标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度需要在 2 到 100 个字符之间', trigger: 'blur' },
  ],
  content: [{ required: true, message: '请输入报障内容', trigger: 'blur' }],
}

const statusRules: FormRules<StatusForm> = {
  status: [{ required: true, message: '请选择处理状态', trigger: 'change' }],
}

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
}

function resetCreateForm() {
  Object.assign(createForm, {
    teaRoomId: null,
    reportedBy: authStore.user?.id ?? 0,
    title: '',
    content: '',
  })
  createFormRef.value?.clearValidate()
}

function openStatusDialog(row: ReportRecord) {
  currentRow.value = row
  statusForm.status = row.status
  statusVisible.value = true
}

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  await reportApi.create(createForm)
  ElMessage.success('报障已提交')
  createVisible.value = false
  await table.load({ page: 0 })
}

async function submitStatus() {
  const valid = await statusFormRef.value?.validate().catch(() => false)
  if (!valid || !currentRow.value) return
  await reportApi.status(currentRow.value.id, statusForm)
  ElMessage.success('报障状态已更新')
  statusVisible.value = false
  await table.load()
}

async function loadTeaRooms() {
  const data = await teaRoomApi.rooms({ page: 0, size: 100 })
  teaRooms.value = data.content
}

onMounted(async () => {
  await Promise.all([table.load(), loadTeaRooms()])
})
</script>

<template>
  <div class="page-shell">
    <PageIntro title="设备报障" description="茶室员可以提交自己负责茶室的报修，管理员统一处理报障状态。">
      <el-button v-if="canCreate" type="primary" @click="createVisible = true">提交报障</el-button>
    </PageIntro>

    <div class="section-card">
      <div class="section-card__body">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="keyword" clearable placeholder="搜索标题 / 内容 / 茶室 / 报障人" style="width: 300px" />
            <el-select v-model="statusFilter" clearable placeholder="状态筛选" style="width: 160px">
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已解决" value="RESOLVED" />
              <el-option label="已关闭" value="CLOSED" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>

        <el-table v-loading="table.loading" :data="visibleRows" class="data-table">
          <el-table-column prop="title" label="报障标题" min-width="180" />
          <el-table-column prop="teaRoomId" label="茶室 ID" width="90" />
          <el-table-column prop="reportedBy" label="报障人" width="90" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              {{ authStore.role === 'ADMIN' ? row.status : formatStatus('report', row.status) }}
            </template>
          </el-table-column>
          <el-table-column prop="content" label="内容" min-width="260" />
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

    <el-dialog v-model="createVisible" title="提交报障" width="620px" @closed="resetCreateForm">
      <el-form ref="createFormRef" :model="createForm" :rules="rules" label-position="top">
        <el-form-item label="茶室" prop="teaRoomId">
          <el-select v-model="createForm.teaRoomId" placeholder="请选择茶室" style="width: 100%">
            <el-option v-for="item in teaRooms" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="createForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="createForm.content" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="更新报障状态" width="420px">
      <el-form ref="statusFormRef" :model="statusForm" :rules="statusRules" label-position="top">
        <el-form-item label="处理状态" prop="status">
          <el-select v-model="statusForm.status" style="width: 100%">
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已关闭" value="CLOSED" />
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
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    width: 100%;
  }
}
</style>
