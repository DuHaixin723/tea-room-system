<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { reviewApi, teaRoomApi } from '@/api/modules/management'
import { useAuthStore } from '@/stores/auth'
import type { ReviewRecord, TeaRoomRecord } from '@/types/business'
import { usePagedTable } from '@/composables/usePagedTable'
import PageIntro from '@/components/shared/PageIntro.vue'

interface ReviewForm {
  userId: number
  teaRoomId: number | null
  reservationId: number | null
  rating: number
  content: string
}

const authStore = useAuthStore()
const keyword = ref('')
const ratingFilter = ref<number | null>(null)
const teaRooms = ref<TeaRoomRecord[]>([])
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const table = usePagedTable<ReviewRecord>((params) => {
  const requestParams = { ...params }
  if (authStore.role === 'STAFF' && authStore.user?.id) requestParams.staffUserId = authStore.user.id
  return reviewApi.page(requestParams)
})

const form = reactive<ReviewForm>({
  userId: authStore.user?.id ?? 0,
  teaRoomId: null,
  reservationId: null,
  rating: 5,
  content: '',
})

const canCreate = computed(() => false)
const visibleRows = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return table.rows.filter((row) => {
    const matchQuery =
      !query || [String(row.userId), String(row.teaRoomId), row.content ?? ''].some((item) => item.toLowerCase().includes(query))
    const matchRating = !ratingFilter.value || row.rating === ratingFilter.value
    return matchQuery && matchRating
  })
})

const reviewStats = computed(() => {
  const rows = visibleRows.value
  const total = rows.length
  const avg = total ? (rows.reduce((sum, row) => sum + row.rating, 0) / total).toFixed(1) : '0.0'
  const high = rows.filter((row) => row.rating >= 4).length
  return { total, avg, high }
})

const rules: FormRules<ReviewForm> = {
  teaRoomId: [{ required: true, message: '请选择茶室', trigger: 'change' }],
  rating: [{ required: true, message: '请选择评分', trigger: 'change' }],
}

function resetFilters() {
  keyword.value = ''
  ratingFilter.value = null
}

function resetForm() {
  Object.assign(form, {
    userId: authStore.user?.id ?? 0,
    teaRoomId: null,
    reservationId: null,
    rating: 5,
    content: '',
  })
  formRef.value?.clearValidate()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  await reviewApi.create(form)
  ElMessage.success('评价已提交')
  dialogVisible.value = false
  await table.load({ page: 0 })
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
    <PageIntro title="评价反馈" description="支持按评分和关键词筛选评价，用户可提交新的服务反馈。">
      <el-button v-if="canCreate" type="primary" @click="dialogVisible = true">提交评价</el-button>
    </PageIntro>

    <div class="review-overview">
      <div class="overview-card">
        <span>当前结果</span>
        <strong>{{ reviewStats.total }}</strong>
      </div>
      <div class="overview-card">
        <span>平均评分</span>
        <strong>{{ reviewStats.avg }}</strong>
      </div>
      <div class="overview-card">
        <span>4 星以上</span>
        <strong>{{ reviewStats.high }}</strong>
      </div>
    </div>

    <div class="section-card">
      <div class="section-card__body">
        <div class="toolbar-panel">
          <div class="toolbar-panel__head">
            <div>
              <strong>评价筛选</strong>
              <p>用关键词和评分快速定位重点评价</p>
            </div>
            <el-button @click="resetFilters">重置</el-button>
          </div>

          <div class="toolbar">
            <div class="toolbar-left">
              <el-input v-model="keyword" clearable placeholder="搜索用户 / 茶室 / 内容" style="width: 320px" />
              <el-select v-model="ratingFilter" clearable placeholder="评分筛选" style="width: 160px">
                <el-option v-for="rating in [5, 4, 3, 2, 1]" :key="rating" :label="`${rating} 星`" :value="rating" />
              </el-select>
            </div>
          </div>
        </div>

        <el-table v-loading="table.loading" :data="visibleRows" class="data-table">
          <el-table-column prop="userId" label="用户ID" width="110" />
          <el-table-column prop="teaRoomId" label="茶室ID" width="110" />
          <el-table-column prop="rating" label="评分" width="110" />
          <el-table-column prop="content" label="评价内容" min-width="320" />
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

    <el-dialog v-model="dialogVisible" title="提交评价" width="560px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="茶室" prop="teaRoomId">
          <el-select v-model="form.teaRoomId" placeholder="请选择茶室" style="width: 100%">
            <el-option v-for="item in teaRooms" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分" prop="rating">
          <el-rate v-model="form.rating" />
        </el-form-item>
        <el-form-item label="反馈内容">
          <el-input v-model="form.content" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.review-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
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
  font-size: 15px;
}

.overview-card strong {
  display: block;
  margin-top: 8px;
  color: #21493b;
  font-size: 30px;
  line-height: 1;
}

.toolbar-panel {
  margin-bottom: 16px;
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

:deep(.toolbar-panel .el-input__wrapper),
:deep(.toolbar-panel .el-select__wrapper),
:deep(.toolbar-panel .el-button) {
  min-height: 46px;
  font-size: 16px;
}

:deep(.data-table) {
  font-size: 18px;
}

:deep(.data-table .el-table__header th) {
  font-size: 18px;
}

:deep(.data-table .cell) {
  line-height: 30px;
  padding-top: 5px;
  padding-bottom: 5px;
}

:deep(.el-pagination) {
  margin-top: 18px;
  font-size: 16px;
}

@media (max-width: 900px) {
  .review-overview {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .toolbar-panel__head,
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    width: 100%;
  }
}
</style>
