<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { activityApi, fileApi } from '@/api/modules/management'
import { useAuthStore } from '@/stores/auth'
import type { ActivityRecord, ActivityRegistrationRecord } from '@/types/business'
import { usePagedTable } from '@/composables/usePagedTable'
import PageIntro from '@/components/shared/PageIntro.vue'
import { formatStatus } from '@/utils/status'
import { resolveAssetUrl } from '@/utils/asset'

interface ActivityForm {
  title: string
  content: string
  schedule: [Date, Date] | []
  capacity: number
  imageUrl: string
  status: string
}

interface RegistrationCard extends ActivityRegistrationRecord {
  activity?: ActivityRecord
}

const authStore = useAuthStore()
const router = useRouter()

const canManage = computed(() => authStore.role === 'STAFF' || authStore.role === 'ADMIN')
const canReview = computed(() => authStore.role === 'ADMIN')
const canRegister = computed(() => authStore.role === 'USER')
const activityKeyword = ref('')
const activityStatusFilter = ref('')
const activityCreatorFilter = ref('')
const registrationKeyword = ref('')
const registrationStatusFilter = ref('')
const activityPage = ref(1)
const activityPageSize = ref(5)
const registrationPage = ref(1)
const registrationPageSize = ref(4)

const activityTable = usePagedTable<ActivityRecord>((params) => activityApi.page(params), {
  size: 200,
})

const registrationTable = usePagedTable<ActivityRegistrationRecord>(
  (params) => {
    const requestParams = { ...params }
    if (authStore.role === 'USER' && authStore.user?.id) {
      requestParams.userId = authStore.user.id
    }
    return activityApi.registrations(requestParams)
  },
  { size: 200 },
)

const activeTab = ref<'activity' | 'registration'>('activity')
const dialogVisible = ref(false)
const detailVisible = ref(false)
const successVisible = ref(false)
const activityImageUploading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const actingId = ref<number | null>(null)
const highlightedRegistrationId = ref<number | null>(null)
const selectedActivity = ref<ActivityRecord | null>(null)
const selectedRegistration = ref<RegistrationCard | null>(null)

const form = reactive<ActivityForm>({
  title: '',
  content: '',
  schedule: [],
  capacity: 30,
  imageUrl: '',
  status: 'PUBLISHED',
})

const adminStatusOptions = [
  { label: '已发布', value: 'PUBLISHED' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已结束', value: 'CLOSED' },
  { label: '已取消', value: 'CANCELLED' },
]

const pageDescription = computed(() => {
  if (authStore.role === 'ADMIN') {
    return '管理员可发布、修改、审核活动，并可在活动结束后补充或修正活动总结。'
  }
  if (authStore.role === 'STAFF') {
    return '茶室员可发起活动，提交后需管理员审核；活动结束后需要填写活动总结。'
  }
  return '用户只会看到未结束且已发布的活动，可直接报名参加。'
})

const createButtonLabel = computed(() => (authStore.role === 'ADMIN' ? '发布活动' : '发起活动'))
const dialogTitle = computed(() => {
  if (editingId.value) return '修改活动'
  return authStore.role === 'ADMIN' ? '发布活动' : '发起活动'
})
const submitButtonLabel = computed(() => {
  if (editingId.value) return '保存修改'
  return authStore.role === 'ADMIN' ? '立即发布' : '提交审核'
})

const activityMap = computed(() => new Map(activityTable.rows.map((item) => [item.id, item])))
const publishedCount = computed(() => activityTable.rows.filter((item) => item.status === 'PUBLISHED').length)
const pendingCount = computed(() => activityTable.rows.filter((item) => item.status === 'PENDING_REVIEW').length)
const myRegistrationCount = computed(() => registrationTable.rows.filter((item) => !item.cancelled).length)

const myRegistrations = computed<RegistrationCard[]>(() =>
  registrationTable.rows.map((item) => ({
    ...item,
    activity: activityMap.value.get(item.activityId),
  })),
)

const filteredActivityRows = computed(() => {
  const query = activityKeyword.value.trim().toLowerCase()
  return activityTable.rows.filter((row) => {
    const creatorLabel = row.creatorRole === 'ADMIN' ? '管理员' : row.creatorRole === 'STAFF' ? '茶室员' : '用户'
    const matchQuery = !query || [row.title, row.content ?? '', creatorLabel].some((item) => item.toLowerCase().includes(query))
    const matchStatus = !activityStatusFilter.value || row.status === activityStatusFilter.value
    const matchCreator = !activityCreatorFilter.value || row.creatorRole === activityCreatorFilter.value
    return matchQuery && matchStatus && matchCreator
  })
})

const filteredRegistrations = computed(() => {
  const query = registrationKeyword.value.trim().toLowerCase()
  return myRegistrations.value.filter((row) => {
    const status = row.cancelled ? 'CANCELLED' : 'ACTIVE'
    const matchQuery = !query || [row.activity?.title ?? '', row.activity?.content ?? ''].some((item) => item.toLowerCase().includes(query))
    const matchStatus = !registrationStatusFilter.value || status === registrationStatusFilter.value
    return matchQuery && matchStatus
  })
})

const pagedActivityRows = computed(() => {
  const start = (activityPage.value - 1) * activityPageSize.value
  return filteredActivityRows.value.slice(start, start + activityPageSize.value)
})

const pagedRegistrations = computed(() => {
  const start = (registrationPage.value - 1) * registrationPageSize.value
  return filteredRegistrations.value.slice(start, start + registrationPageSize.value)
})

const summaryReminderRows = computed(() =>
  activityTable.rows.filter(
    (item) =>
      item.creatorUserId === authStore.user?.id &&
      ['CLOSED', 'CANCELLED'].includes(item.status) &&
      !item.summaryContent?.trim(),
  ),
)

const rules: FormRules<ActivityForm> = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  schedule: [{ required: true, message: '请选择活动时间', trigger: 'change' }],
  capacity: [{ required: true, message: '请输入活动人数', trigger: 'change' }],
  status: [{ required: true, message: '请选择活动状态', trigger: 'change' }],
}

function activityImage(activity?: ActivityRecord | null) {
  return (
    resolveAssetUrl(activity?.imageUrl) ||
    'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=1200&q=80'
  )
}

function activitySummary(row?: ActivityRecord | null) {
  return row?.content || '适合品茶交流、体验课程或主题活动。'
}

function activityTagType(status: string) {
  if (status === 'PUBLISHED') return 'success'
  if (status === 'PENDING_REVIEW') return 'warning'
  if (status === 'REJECTED' || status === 'CANCELLED') return 'danger'
  return 'info'
}

function registrationBadgeType(row: RegistrationCard) {
  if (row.cancelled) return 'danger'
  if (row.activity && dayjs(row.activity.endTime).isBefore(dayjs())) return 'info'
  return 'success'
}

function registrationState(row: ActivityRegistrationRecord) {
  return row.cancelled ? '已取消' : '报名成功'
}

function isSummaryEditable(row: ActivityRecord) {
  if (!['CLOSED', 'CANCELLED'].includes(row.status)) return false
  return canReview.value || row.creatorUserId === authStore.user?.id
}

function resetForm() {
  editingId.value = null
  Object.assign(form, {
    title: '',
    content: '',
    schedule: [],
    capacity: 30,
    imageUrl: '',
    status: 'PUBLISHED',
  })
  formRef.value?.clearValidate()
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: ActivityRecord) {
  editingId.value = row.id
  Object.assign(form, {
    title: row.title,
    content: row.content ?? '',
    schedule: [new Date(row.startTime), new Date(row.endTime)],
    capacity: row.capacity,
    imageUrl: row.imageUrl ?? '',
    status: row.status,
  })
  dialogVisible.value = true
}

function openActivityDetail(row: ActivityRecord) {
  selectedActivity.value = row
  selectedRegistration.value = canRegister.value
    ? myRegistrations.value.find((item) => item.activityId === row.id && !item.cancelled) ?? null
    : null
  detailVisible.value = true
}

function openRegistrationDetail(row: RegistrationCard) {
  selectedRegistration.value = row
  selectedActivity.value = row.activity ?? null
  detailVisible.value = true
}

function hasRegistered(activityId: number) {
  return myRegistrations.value.some((item) => item.activityId === activityId && !item.cancelled)
}

function canRegisterActivity(row: ActivityRecord) {
  return canRegister.value && row.status === 'PUBLISHED' && !hasRegistered(row.id)
}

function canCloseActivity(row: ActivityRecord) {
  return canReview.value && row.status === 'PUBLISHED' && dayjs().isAfter(dayjs(row.endTime))
}

function disablePastDates(date: Date) {
  return dayjs(date).endOf('day').isBefore(dayjs())
}

function openComplaintEntry() {
  void router.push({ path: '/complaints', query: { action: 'create' } })
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !Array.isArray(form.schedule) || form.schedule.length !== 2) return

  const [startTime, endTime] = form.schedule
  if (!dayjs(startTime).isAfter(dayjs())) {
    ElMessage.error('活动开始时间必须晚于当前时间')
    return
  }
  if (!dayjs(endTime).isAfter(dayjs(startTime))) {
    ElMessage.error('活动结束时间必须晚于开始时间')
    return
  }

  const payload = {
    title: form.title,
    content: form.content,
    startTime: dayjs(startTime).format('YYYY-MM-DDTHH:mm:ss'),
    endTime: dayjs(endTime).format('YYYY-MM-DDTHH:mm:ss'),
    capacity: form.capacity,
    imageUrl: form.imageUrl,
    status: canReview.value ? form.status : 'PENDING_REVIEW',
  }

  if (editingId.value) {
    await activityApi.update(editingId.value, payload)
  } else {
    await activityApi.create(payload)
  }

  ElMessage.success(editingId.value ? '活动已更新' : canReview.value ? '活动已发布' : '活动已提交审核')
  dialogVisible.value = false
  await activityTable.load()
}

async function handleActivityImageSelect(file: UploadRawFile) {
  if (!file.type?.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (file.size / 1024 / 1024 > 8) {
    ElMessage.error('图片大小不能超过 8MB')
    return false
  }

  activityImageUploading.value = true
  try {
    form.imageUrl = await fileApi.uploadImage(file, 'activities')
    ElMessage.success('活动图片上传成功')
  } finally {
    activityImageUploading.value = false
  }
  return false
}

function onActivityImageChange(file: UploadFile) {
  if (!file.raw) return
  void handleActivityImageSelect(file.raw)
}

async function reviewActivity(row: ActivityRecord, status: 'PUBLISHED' | 'REJECTED') {
  actingId.value = row.id
  try {
    await activityApi.status(row.id, { status })
    ElMessage.success(status === 'PUBLISHED' ? '活动审核通过' : '活动已驳回')
    await activityTable.load()
  } finally {
    actingId.value = null
  }
}

async function closeActivity(row: ActivityRecord) {
  actingId.value = row.id
  try {
    await activityApi.status(row.id, { status: 'CLOSED' })
    ElMessage.success('活动已确认结束')
    await activityTable.load()
  } finally {
    actingId.value = null
  }
}

async function register(row: ActivityRecord) {
  if (!authStore.user?.id || !canRegisterActivity(row)) return
  actingId.value = row.id
  try {
    const registration = await activityApi.register({ activityId: row.id, userId: authStore.user.id })
    ElMessage.success('报名成功')
    await Promise.all([activityTable.load(), registrationTable.load({ page: 0 })])
    activeTab.value = 'registration'
    highlightedRegistrationId.value = registration.id
    selectedActivity.value = row
    selectedRegistration.value = { ...registration, activity: row }
    successVisible.value = true
  } finally {
    actingId.value = null
  }
}

async function cancelRegistration(row: ActivityRegistrationRecord) {
  actingId.value = row.id
  try {
    await activityApi.cancelRegistration(row.id)
    ElMessage.success('报名已取消')
    await registrationTable.load()
  } finally {
    actingId.value = null
  }
}

async function openSummaryEditor(row: ActivityRecord) {
  try {
    const { value } = await ElMessageBox.prompt('请填写活动总结，便于管理员追踪活动结果。', '活动总结', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValue: row.summaryContent ?? '',
      inputPlaceholder: '填写活动过程、到场情况、问题记录和结果总结',
      inputValidator: (value) => {
        if (!value || !value.trim()) return '请输入活动总结'
        if (value.trim().length < 10) return '活动总结至少 10 个字'
        return true
      },
    })
    await activityApi.summary(row.id, { summaryContent: value.trim() })
    ElMessage.success('活动总结已保存')
    await activityTable.load()
  } catch (error) {
    if (error !== 'cancel') {
      throw error
    }
  }
}

function handleActivityPageChange(page: number) {
  activityPage.value = page
}

function handleActivityPageSizeChange(size: number) {
  activityPageSize.value = size
  activityPage.value = 1
}

function handleRegistrationPageChange(page: number) {
  registrationPage.value = page
}

function handleRegistrationPageSizeChange(size: number) {
  registrationPageSize.value = size
  registrationPage.value = 1
}

function resetActivityFilters() {
  activityKeyword.value = ''
  activityStatusFilter.value = ''
  activityCreatorFilter.value = ''
  activityPage.value = 1
}

function resetRegistrationFilters() {
  registrationKeyword.value = ''
  registrationStatusFilter.value = ''
  registrationPage.value = 1
}

watch(() => [activityKeyword.value, activityStatusFilter.value, activityCreatorFilter.value], () => {
  activityPage.value = 1
})

watch(() => [registrationKeyword.value, registrationStatusFilter.value], () => {
  registrationPage.value = 1
})

watch(() => filteredActivityRows.value.length, (length) => {
  const maxPage = Math.max(1, Math.ceil(length / activityPageSize.value))
  if (activityPage.value > maxPage) activityPage.value = maxPage
})

watch(() => filteredRegistrations.value.length, (length) => {
  const maxPage = Math.max(1, Math.ceil(length / registrationPageSize.value))
  if (registrationPage.value > maxPage) registrationPage.value = maxPage
})

onMounted(async () => {
  await Promise.all([activityTable.load(), canRegister.value ? registrationTable.load() : Promise.resolve()])
})
</script>

<template>
  <div class="page-shell">
    <PageIntro title="近期活动" :description="pageDescription" />

    <div class="activity-metrics">
      <div class="metric-card">
        <span>已发布活动</span>
        <strong>{{ publishedCount }}</strong>
      </div>
      <div class="metric-card">
        <span>{{ canManage ? '待审核活动' : '可报名活动' }}</span>
        <strong>{{ canManage ? pendingCount : publishedCount }}</strong>
      </div>
      <div v-if="canRegister" class="metric-card">
        <span>我的报名</span>
        <strong>{{ myRegistrationCount }}</strong>
      </div>
    </div>

    <div v-if="canRegister" class="section-card complaint-shortcut">
      <div class="section-card__body complaint-shortcut__body">
        <div>
          <div class="complaint-shortcut__eyebrow">用户投诉入口</div>
          <h3>服务过程中遇到问题？</h3>
          <p>如果你在预约到店服务中遇到接待、服务态度或执行问题，可以直接进入投诉页提交反馈。</p>
        </div>
        <el-button type="danger" plain @click="openComplaintEntry">立即投诉</el-button>
      </div>
    </div>

    <div v-if="summaryReminderRows.length" class="section-card reminder-banner">
      <div class="section-card__body reminder-banner__body">
        <div>
          <div class="reminder-banner__eyebrow">待补充总结</div>
          <h3>你有 {{ summaryReminderRows.length }} 个已结束活动还没有填写总结</h3>
          <p>活动结束后请尽快补充总结，方便管理员跟进活动情况。</p>
        </div>
        <div class="reminder-banner__actions">
          <el-button
            v-for="row in summaryReminderRows.slice(0, 2)"
            :key="row.id"
            type="warning"
            plain
            @click="openSummaryEditor(row)"
          >
            {{ row.title }}
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="canRegister && selectedRegistration && successVisible" class="section-card success-banner">
      <div class="section-card__body success-banner__body">
        <div>
          <div class="success-banner__eyebrow">报名成功</div>
          <h3>{{ selectedActivity?.title }}</h3>
          <p>{{ activitySummary(selectedActivity) }}</p>
        </div>
        <div class="success-banner__actions">
          <el-button plain @click="successVisible = false">稍后查看</el-button>
          <el-button type="primary" @click="detailVisible = true">查看详情</el-button>
        </div>
      </div>
    </div>

    <div class="section-card">
      <div class="section-card__body">
        <div class="activity-toolbar">
          <el-tabs v-model="activeTab" class="activity-tabs">
            <el-tab-pane label="活动广场" name="activity" />
            <el-tab-pane v-if="canRegister" label="我的报名" name="registration" />
          </el-tabs>
          <el-button v-if="canManage" type="primary" class="activity-toolbar__button" @click="openCreate">
            {{ createButtonLabel }}
          </el-button>
        </div>

        <template v-if="activeTab === 'activity'">
          <div class="toolbar activity-filterbar">
            <div class="toolbar-left">
              <el-input v-model="activityKeyword" clearable placeholder="搜索名称、描述或发起人" style="width: 280px" />
              <el-select v-model="activityStatusFilter" clearable placeholder="状态" style="width: 160px">
                <el-option label="已发布" value="PUBLISHED" />
                <el-option label="待审核" value="PENDING_REVIEW" />
                <el-option label="已驳回" value="REJECTED" />
                <el-option label="已结束" value="CLOSED" />
                <el-option label="已取消" value="CANCELLED" />
              </el-select>
              <el-select v-model="activityCreatorFilter" clearable placeholder="发起人" style="width: 160px">
                <el-option label="管理员" value="ADMIN" />
                <el-option label="茶室员" value="STAFF" />
              </el-select>
            </div>
            <div class="toolbar-right">
              <el-button @click="resetActivityFilters">重置</el-button>
            </div>
          </div>

          <div v-if="filteredActivityRows.length" class="activity-grid">
            <article v-for="row in pagedActivityRows" :key="row.id" class="activity-card">
              <div class="activity-card__image" :style="{ backgroundImage: `url(${activityImage(row)})` }" />
              <div class="activity-card__top">
                <el-tag :type="activityTagType(row.status)">
                  {{ formatStatus('activity', row.status) }}
                </el-tag>
                <span class="activity-card__capacity">名额 {{ row.capacity }}</span>
              </div>

              <h3>{{ row.title }}</h3>
              <p>{{ activitySummary(row) }}</p>

              <div class="activity-card__time">
                <div>
                  <span>开始时间</span>
                  <strong>{{ dayjs(row.startTime).format('YYYY-MM-DD HH:mm') }}</strong>
                </div>
                <div>
                  <span>结束时间</span>
                  <strong>{{ dayjs(row.endTime).format('YYYY-MM-DD HH:mm') }}</strong>
                </div>
              </div>

              <div class="activity-card__time activity-card__time--single">
                <div>
                  <span>发起人</span>
                  <strong>{{ row.creatorRole === 'ADMIN' ? '管理员' : '茶室员' }}</strong>
                </div>
              </div>

              <div class="activity-card__actions">
                <div class="activity-card__links">
                  <el-button link type="primary" @click="openActivityDetail(row)">查看详情</el-button>
                  <el-button v-if="canReview" link type="primary" @click="openEdit(row)">修改</el-button>
                  <el-button
                    v-if="isSummaryEditable(row)"
                    link
                    type="warning"
                    @click="openSummaryEditor(row)"
                  >
                    {{ row.summaryContent ? '修改总结' : '提交总结' }}
                  </el-button>
                  <el-button
                    v-if="canReview && row.status === 'PENDING_REVIEW'"
                    link
                    type="success"
                    :loading="actingId === row.id"
                    @click="reviewActivity(row, 'PUBLISHED')"
                  >
                    通过
                  </el-button>
                  <el-button
                    v-if="canReview && row.status === 'PENDING_REVIEW'"
                    link
                    type="danger"
                    :loading="actingId === row.id"
                    @click="reviewActivity(row, 'REJECTED')"
                  >
                    驳回
                  </el-button>
                  <el-button
                    v-if="canCloseActivity(row)"
                    link
                    type="warning"
                    :loading="actingId === row.id"
                    @click="closeActivity(row)"
                  >
                    确认结束
                  </el-button>
                </div>

                <el-button
                  v-if="canRegister"
                  :type="canRegisterActivity(row) ? 'success' : 'info'"
                  plain
                  :disabled="!canRegisterActivity(row)"
                  :loading="actingId === row.id"
                  @click="register(row)"
                >
                  {{ hasRegistered(row.id) ? '已报名' : '立即报名' }}
                </el-button>
              </div>
            </article>
          </div>
          <el-empty v-else description="暂无符合条件的活动" />

          <el-pagination
            v-if="filteredActivityRows.length > activityPageSize"
            class="list-pagination"
            background
            layout="total, sizes, prev, pager, next"
            :current-page="activityPage"
            :page-size="activityPageSize"
            :total="filteredActivityRows.length"
            @current-change="handleActivityPageChange"
            @size-change="handleActivityPageSizeChange"
          />
        </template>

        <template v-else>
          <div class="toolbar activity-filterbar">
            <div class="toolbar-left">
              <el-input v-model="registrationKeyword" clearable placeholder="搜索活动名称或描述" style="width: 300px" />
              <el-select v-model="registrationStatusFilter" clearable placeholder="报名状态" style="width: 160px">
                <el-option label="已报名" value="ACTIVE" />
                <el-option label="已取消" value="CANCELLED" />
              </el-select>
            </div>
            <div class="toolbar-right">
              <el-button @click="resetRegistrationFilters">重置</el-button>
            </div>
          </div>

          <div v-if="filteredRegistrations.length" class="registration-grid">
            <article
              v-for="row in pagedRegistrations"
              :key="row.id"
              class="registration-card"
              :class="{ 'is-highlighted': highlightedRegistrationId === row.id }"
            >
              <div class="registration-card__top">
                <div>
                  <h3>{{ row.activity?.title ?? `活动 #${row.activityId}` }}</h3>
                  <p>{{ row.activity?.content || '可查看活动时间和参加提醒。' }}</p>
                </div>
                <el-tag :type="registrationBadgeType(row)">{{ registrationState(row) }}</el-tag>
              </div>

              <div class="registration-card__meta">
                <div>
                  <span>活动时间</span>
                  <strong>
                    {{
                      row.activity
                        ? `${dayjs(row.activity.startTime).format('MM-DD HH:mm')} - ${dayjs(row.activity.endTime).format('MM-DD HH:mm')}`
                        : '待加载'
                    }}
                  </strong>
                </div>
                <div>
                  <span>到场建议</span>
                  <strong>建议提前 15 分钟到场</strong>
                </div>
              </div>

              <div class="registration-card__actions">
                <el-button link type="primary" @click="openRegistrationDetail(row)">查看详情</el-button>
                <el-button
                  v-if="!row.cancelled"
                  link
                  type="danger"
                  :loading="actingId === row.id"
                  @click="cancelRegistration(row)"
                >
                  取消报名
                </el-button>
              </div>
            </article>
          </div>

          <el-pagination
            v-if="filteredRegistrations.length > registrationPageSize"
            class="list-pagination"
            background
            layout="total, sizes, prev, pager, next"
            :current-page="registrationPage"
            :page-size="registrationPageSize"
            :total="filteredRegistrations.length"
            @current-change="handleRegistrationPageChange"
            @size-change="handleRegistrationPageSizeChange"
          />

          <el-empty v-else description="暂时还没有报名记录" />
        </template>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="form.title" />
        </el-form-item>

        <el-form-item label="活动时间" prop="schedule">
          <el-date-picker
            v-model="form.schedule"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            :disabled-date="disablePastDates"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="活动图片">
          <el-upload :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="onActivityImageChange">
            <el-button :loading="activityImageUploading">选择图片</el-button>
          </el-upload>
        </el-form-item>

        <el-form-item v-if="canReview" label="活动状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option
              v-for="option in adminStatusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="活动人数" prop="capacity">
          <el-input-number v-model="form.capacity" :min="1" style="width: 100%" />
        </el-form-item>

        <el-form-item label="活动内容">
          <el-input v-model="form.content" type="textarea" :rows="5" />
        </el-form-item>

        <el-form-item v-if="form.imageUrl" label="图片预览">
          <el-image :src="activityImage({ imageUrl: form.imageUrl } as ActivityRecord)" fit="cover" style="width: 100%; height: 220px; border-radius: 16px" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">{{ submitButtonLabel }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="活动详情" size="520px">
      <template v-if="selectedActivity">
        <div class="detail-panel">
          <div class="detail-cover" :style="{ backgroundImage: `url(${activityImage(selectedActivity)})` }" />

          <div class="detail-hero">
            <el-tag :type="activityTagType(selectedActivity.status)" effect="dark">
              {{
                selectedRegistration && !selectedRegistration.cancelled
                  ? '报名已确认'
                  : formatStatus('activity', selectedActivity.status)
              }}
            </el-tag>
            <h2>{{ selectedActivity.title }}</h2>
            <p>{{ activitySummary(selectedActivity) }}</p>
          </div>

          <div class="detail-block">
            <h4>活动时间</h4>
            <p>
              {{ dayjs(selectedActivity.startTime).format('YYYY-MM-DD HH:mm') }}
              -
              {{ dayjs(selectedActivity.endTime).format('YYYY-MM-DD HH:mm') }}
            </p>
          </div>

          <div class="detail-block">
            <h4>活动发起人</h4>
            <p>{{ selectedActivity.creatorRole === 'ADMIN' ? '管理员' : '茶室员' }}</p>
          </div>

          <div v-if="selectedActivity.summaryContent" class="detail-block">
            <h4>活动总结</h4>
            <p>{{ selectedActivity.summaryContent }}</p>
            <small>提交时间：{{ dayjs(selectedActivity.summarySubmittedAt).format('YYYY-MM-DD HH:mm') }}</small>
          </div>

          <div class="detail-block">
            <h4>参加提醒</h4>
            <ul class="detail-list">
              <li>建议提前 15 分钟到场。</li>
              <li>如无法参加，请提前取消报名。</li>
              <li>活动信息如有调整，会同步更新到页面。</li>
            </ul>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.activity-metrics {
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

.complaint-shortcut {
  margin-bottom: 18px;
}

.complaint-shortcut__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: linear-gradient(135deg, rgba(255, 244, 240, 0.96), rgba(255, 251, 247, 0.94));
}

.complaint-shortcut__eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: #b54708;
  background: rgba(245, 158, 11, 0.14);
}

.complaint-shortcut h3 {
  margin: 12px 0 8px;
  font-size: 24px;
}

.complaint-shortcut p {
  margin: 0;
  color: var(--muted);
  line-height: 1.8;
}

.reminder-banner,
.success-banner {
  margin-bottom: 18px;
}

.reminder-banner__body,
.success-banner__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.reminder-banner__body {
  background: linear-gradient(135deg, rgba(255, 247, 229, 0.96), rgba(255, 252, 246, 0.94));
}

.success-banner__body {
  background: linear-gradient(135deg, rgba(248, 252, 244, 0.96), rgba(255, 249, 241, 0.94));
}

.reminder-banner__eyebrow,
.success-banner__eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.reminder-banner__eyebrow {
  background: rgba(230, 162, 60, 0.16);
  color: #b88224;
}

.success-banner__eyebrow {
  background: rgba(103, 194, 58, 0.14);
  color: #5b8b2a;
}

.reminder-banner h3,
.success-banner h3 {
  margin: 12px 0 8px;
  font-size: 28px;
}

.reminder-banner p,
.success-banner p {
  margin: 0;
  color: var(--muted);
  line-height: 1.8;
}

.reminder-banner__actions,
.success-banner__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.activity-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.activity-tabs {
  flex: 1;
}

.activity-filterbar {
  margin-bottom: 18px;
}

.activity-grid,
.registration-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 18px;
  align-items: start;
}

.activity-card,
.registration-card {
  padding: 16px;
  border: 1px solid rgba(123, 88, 50, 0.14);
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 14px 30px rgba(77, 55, 31, 0.08);
}

.registration-card.is-highlighted {
  border-color: rgba(230, 162, 60, 0.45);
  box-shadow: 0 16px 34px rgba(230, 162, 60, 0.16);
}

.activity-card__image {
  height: 148px;
  margin: -16px -16px 14px;
  border-radius: 18px 18px 14px 14px;
  background-size: cover;
  background-position: center;
}

.activity-card__top,
.activity-card__actions,
.registration-card__top,
.registration-card__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.activity-card__links {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.activity-card h3,
.registration-card h3 {
  margin: 10px 0 6px;
  font-size: 18px;
}

.activity-card p,
.registration-card p {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
  font-size: 13px;
}

.activity-card__capacity {
  color: #7c6b57;
  font-size: 12px;
}

.activity-card__time,
.registration-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid rgba(15, 23, 42, 0.08);
}

.activity-card__time--single {
  grid-template-columns: 1fr;
}

.activity-card__time span,
.registration-card__meta span {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.activity-card__time strong,
.registration-card__meta strong {
  display: block;
  margin-top: 6px;
  font-size: 13px;
}

.detail-cover {
  height: 220px;
  border-radius: 18px;
  background-size: cover;
  background-position: center;
  margin-bottom: 16px;
}

.detail-hero h2 {
  margin: 12px 0 8px;
}

.detail-hero p,
.detail-block p,
.detail-block small {
  margin: 0;
  color: var(--muted);
  line-height: 1.8;
}

.detail-block {
  margin-top: 20px;
}

.detail-list {
  padding-left: 18px;
  color: var(--muted);
  line-height: 1.8;
}

@media (max-width: 1600px) {
  .activity-grid,
  .registration-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1280px) {
  .activity-grid,
  .registration-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .activity-metrics,
  .activity-grid,
  .registration-grid,
  .activity-card__time,
  .registration-card__meta {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .complaint-shortcut__body,
  .activity-filterbar,
  .activity-toolbar,
  .reminder-banner__body,
  .success-banner__body,
  .activity-card__actions,
  .registration-card__actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
