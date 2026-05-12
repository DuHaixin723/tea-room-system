<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { usersApi, activityApi, favoriteApi, orderApi, reportApi, reservationApi, reviewApi, teaRoomApi } from '@/api/modules/management'
import { fetchOverview, fetchRecommendations } from '@/api/modules/dashboard'
import type {
  ActivityRegistrationRecord,
  FavoriteRecord,
  MemberAccountRecord,
  OrderRecord,
  RecommendationRecord,
  ReportRecord,
  ReservationRecord,
  ReviewRecord,
  StatisticOverview,
  TeaRoomRecord,
} from '@/types/business'
import PageIntro from '@/components/shared/PageIntro.vue'
import MetricCard from '@/components/shared/MetricCard.vue'
import TrendChart from '@/components/chart/TrendChart.vue'
import { resolveAssetUrl } from '@/utils/asset'

interface ProfileForm {
  nickname: string
  phone: string
  email: string
  avatarUrl: string
}

interface PasswordForm {
  oldPassword: string
  newPassword: string
}

const authStore = useAuthStore()
const router = useRouter()

const profileFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const avatarUploading = ref(false)
const reviewsLoading = ref(false)
const reviewKeyword = ref('')
const reviewPage = ref(1)
const reviewPageSize = 2
const reviewPanelTitle = '\u6211\u7684\u8BC4\u4EF7'
const reviewSearchPlaceholder = '\u641C\u7D22\u8336\u5BA4 / \u8BC4\u4EF7\u5185\u5BB9 / \u8BA2\u5355\u53F7 / \u8BC4\u5206'
const reviewOrderNoLabel = '\u8BA2\u5355\u53F7'
const reviewEmptyContentText = '\u8BE5\u8BC4\u4EF7\u6682\u672A\u586B\u5199\u8BE6\u7EC6\u5185\u5BB9\u3002'
const reviewNoMatchText = '\u672A\u627E\u5230\u5339\u914D\u7684\u8BC4\u4EF7\u4FE1\u606F'
const reviewNoReservationText = '\u5F53\u524D\u8FD8\u672A\u9884\u5B9A\u8FC7\u8336\u5BA4\uFF0C\u5FEB\u53BB\u9884\u7EA6\u5427~'

const account = ref<MemberAccountRecord | null>(null)
const overview = ref<StatisticOverview | null>(null)
const recommendations = ref<RecommendationRecord | null>(null)

const userOrders = ref<OrderRecord[]>([])
const userReservations = ref<ReservationRecord[]>([])
const userFavorites = ref<FavoriteRecord[]>([])
const userRegistrations = ref<ActivityRegistrationRecord[]>([])
const userReviews = ref<ReviewRecord[]>([])
const teaRooms = ref<TeaRoomRecord[]>([])
const staffReservations = ref<ReservationRecord[]>([])
const staffReports = ref<ReportRecord[]>([])

const canViewMemberAccount = computed(() => authStore.role === 'USER')
const isAdmin = computed(() => authStore.role === 'ADMIN')
const isStaff = computed(() => authStore.role === 'STAFF')
const isUser = computed(() => authStore.role === 'USER')

const profileForm = reactive<ProfileForm>({
  nickname: authStore.user?.nickname ?? '',
  phone: '',
  email: '',
  avatarUrl: authStore.user?.avatarUrl ?? '',
})

const passwordForm = reactive<PasswordForm>({
  oldPassword: '',
  newPassword: '',
})

const profileRules: FormRules<ProfileForm> = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度需在 2 到 20 个字符之间', trigger: 'blur' },
  ],
  phone: [{ pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const passwordRules: FormRules<PasswordForm> = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 个字符之间', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 个字符之间', trigger: 'blur' },
  ],
}

const avatarPreview = computed(() => resolveAssetUrl(profileForm.avatarUrl))
const avatarFallback = computed(() => (profileForm.nickname || authStore.user?.username || 'U').slice(0, 1).toUpperCase())
const roleText = computed(() => {
  if (authStore.role === 'ADMIN') return '管理员'
  if (authStore.role === 'STAFF') return '茶室员'
  return '顾客'
})

const userMetrics = computed(() => {
  const pendingOrders = userOrders.value.filter((o) => o.status === 'PENDING_PAYMENT').length
  const activeOrders = userOrders.value.filter((o) => ['PENDING_PAYMENT', 'PAID'].includes(o.status)).length
  const upcomingReservations = userReservations.value.filter((r) => ['PENDING', 'CONFIRMED', 'USER_CHECKED_IN'].includes(r.status)).length
  return {
    pendingOrders,
    activeOrders,
    upcomingReservations,
    favorites: userFavorites.value.length,
    registrations: userRegistrations.value.filter((r) => !r.cancelled).length,
  }
})

const staffMetrics = computed(() => {
  const pendingReservations = staffReservations.value.filter((r) => ['PENDING', 'USER_CHECKED_IN'].includes(r.status)).length
  const activeReports = staffReports.value.filter((r) => ['PENDING', 'PROCESSING'].includes(r.status)).length
  const today = new Date().toISOString().slice(0, 10)
  const todayReservations = staffReservations.value.filter((r) => r.reservedStartTime.startsWith(today)).length
  return { pendingReservations, activeReports, todayReservations }
})

const teaRoomNameMap = computed(() => new Map(teaRooms.value.map((room) => [room.id, room.name])))
const orderNoMap = computed(
  () => new Map(userOrders.value.filter((order) => order.reservationId != null).map((order) => [order.reservationId as number, order.orderNo])),
)
const hasAnyReview = computed(() => userReviews.value.length > 0)
const normalizedUserReviews = computed(() =>
  userReviews.value.map((review) => {
    const teaRoomName = teaRoomNameMap.value.get(review.teaRoomId) ?? `\u8336\u5BA4 #${review.teaRoomId}`
    const orderNo = review.reservationId ? (orderNoMap.value.get(review.reservationId) ?? '-') : '-'
    return {
      ...review,
      teaRoomName,
      orderNo,
    }
  }),
)
const filteredDisplayReviews = computed(() => {
  const keyword = reviewKeyword.value.trim().toLowerCase()
  if (!keyword) return normalizedUserReviews.value
  return normalizedUserReviews.value.filter((review) =>
    [review.teaRoomName, review.content ?? '', review.orderNo, `${review.rating}\u661F`].some((field) =>
      field.toLowerCase().includes(keyword),
    ),
  )
})
const pagedDisplayReviews = computed(() => {
  const start = (reviewPage.value - 1) * reviewPageSize
  return filteredDisplayReviews.value.slice(start, start + reviewPageSize)
})

async function loadAllUserOrders(userId: number) {
  const pageSize = 100
  const firstPage = await orderApi.page({ page: 0, size: pageSize, userId })
  const totalPages = Math.max(firstPage.totalPages ?? 1, 1)

  if (totalPages <= 1) {
    return firstPage.content
  }

  const restPages = await Promise.all(
    Array.from({ length: totalPages - 1 }, (_, index) => orderApi.page({ page: index + 1, size: pageSize, userId })),
  )

  return [firstPage, ...restPages].flatMap((page) => page.content)
}

async function loadProfile() {
  if (!authStore.user) return
  const current = await usersApi.getProfile(authStore.user.id)
  profileForm.nickname = current.nickname ?? ''
  profileForm.phone = current.phone ?? ''
  profileForm.email = current.email ?? ''
  profileForm.avatarUrl = current.avatarUrl ?? ''
  authStore.setUser({
    ...authStore.user,
    nickname: current.nickname,
    avatarUrl: current.avatarUrl,
  })
}

async function loadAccount() {
  if (!authStore.user || !canViewMemberAccount.value) return
  account.value = await usersApi.account(authStore.user.id)
}

async function loadOverview() {
  if (isAdmin.value) {
    overview.value = await fetchOverview()
  }
  if (authStore.user?.id && isUser.value) {
    recommendations.value = await fetchRecommendations(authStore.user.id)
    const [orders, reservations, favorites, registrations, reviewPageData, teaRoomPageData] = await Promise.all([
      loadAllUserOrders(authStore.user.id),
      reservationApi.page({ page: 0, size: 50, userId: authStore.user.id }),
      favoriteApi.page({ page: 0, size: 50, userId: authStore.user.id }),
      activityApi.registrations({ page: 0, size: 50, userId: authStore.user.id }),
      reviewApi.page({ page: 0, size: 100, sort: 'id,desc' }),
      teaRoomApi.rooms({ page: 0, size: 100 }),
    ])
    userOrders.value = orders
    userReservations.value = reservations.content
    userFavorites.value = favorites.content
    userRegistrations.value = registrations.content
    userReviews.value = reviewPageData.content
    teaRooms.value = teaRoomPageData.content
  }
  if (authStore.user?.id && isStaff.value) {
    const [reservations, reports] = await Promise.all([
      reservationApi.page({ page: 0, size: 50, staffUserId: authStore.user.id }),
      reportApi.page({ page: 0, size: 50, staffUserId: authStore.user.id }),
    ])
    staffReservations.value = reservations.content
    staffReports.value = reports.content
  }
}

async function saveProfile() {
  const valid = await profileFormRef.value?.validate().catch(() => false)
  if (!valid || !authStore.user) return
  const result = await usersApi.profile(authStore.user.id, profileForm)
  authStore.setUser({
    ...authStore.user,
    nickname: result.nickname,
    avatarUrl: result.avatarUrl,
  })
  ElMessage.success('资料已更新')
}

async function handleAvatarSelect(file: UploadRawFile) {
  if (!authStore.user) return false
  if (!file.type?.startsWith('image/')) {
    ElMessage.error('只能选择图片文件')
    return false
  }
  if (file.size / 1024 / 1024 > 5) {
    ElMessage.error('头像图片不能超过 5MB')
    return false
  }

  avatarUploading.value = true
  try {
    const result = await usersApi.uploadAvatar(authStore.user.id, file)
    profileForm.avatarUrl = result.avatarUrl ?? ''
    authStore.setUser({
      ...authStore.user,
      nickname: result.nickname,
      avatarUrl: result.avatarUrl,
    })
    ElMessage.success('头像已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '头像上传失败')
  } finally {
    avatarUploading.value = false
  }
  return false
}

function onAvatarChange(file: UploadFile) {
  if (!file.raw) return
  void handleAvatarSelect(file.raw)
}

async function changePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid || !authStore.user) return
  await usersApi.password(authStore.user.id, passwordForm)
  ElMessage.success('密码已更新')
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
}

function goRecharge() {
  router.push('/recharge')
}

function handleReviewPageChange(page: number) {
  reviewPage.value = page
}

watch(reviewKeyword, () => {
  reviewPage.value = 1
})

onMounted(async () => {
  reviewsLoading.value = true
  try {
    await Promise.all([loadProfile(), loadAccount(), loadOverview()])
  } finally {
    reviewsLoading.value = false
  }
})
</script>

<template>
  <div class="page-shell">
    <PageIntro title="个人中心" description="在这里维护头像、昵称、联系方式和账户安全信息。">
      <div class="profile-hero">
        <div class="profile-avatar" :class="{ 'has-image': Boolean(avatarPreview) }">
          <img v-if="avatarPreview" :src="avatarPreview" alt="avatar" />
          <span v-else>{{ avatarFallback }}</span>
        </div>
        <div class="profile-hero__meta">
          <strong>{{ profileForm.nickname || authStore.user?.nickname || authStore.user?.username }}</strong>
          <span>{{ roleText }}</span>
        </div>
      </div>
    </PageIntro>

    <div v-if="overview" class="metric-grid">
      <MetricCard label="用户数" :value="overview.userCount" sub="平台注册用户总量" />
      <MetricCard label="茶室数" :value="overview.teaRoomCount" sub="当前可运营的茶室资源" />
      <MetricCard label="订单数" :value="overview.orderCount" sub="累计订单处理量" />
      <MetricCard label="交易额" :value="`¥${overview.totalOrderAmount}`" sub="平台累计成交金额" />
    </div>

    <div v-if="isStaff" class="metric-grid">
      <MetricCard label="待处理预约" :value="staffMetrics.pendingReservations" sub="需要继续跟进的预约" />
      <MetricCard label="处理中报障" :value="staffMetrics.activeReports" sub="当前仍在处理中的设备问题" />
      <MetricCard label="今日预约" :value="staffMetrics.todayReservations" sub="今天开始的预约数量" />
    </div>

    <div v-if="isUser" class="metric-grid">
      <MetricCard label="待支付订单" :value="userMetrics.pendingOrders" sub="可继续支付或取消的订单" />
      <MetricCard label="进行中订单" :value="userMetrics.activeOrders" sub="已支付和待支付订单总数" />
      <MetricCard label="待出行预约" :value="userMetrics.upcomingReservations" sub="尚未完成的预约数量" />
      <MetricCard label="我的收藏" :value="userMetrics.favorites" sub="已收藏的茶室与茶叶" />
    </div>

    <div class="section-card">
      <div class="section-card__header">
        <h3 class="section-card__title">趋势概览</h3>
      </div>
      <div class="section-card__body">
        <TrendChart title="预约热度" :values="[18, 25, 22, 30, 28, 35, 33]" />
      </div>
    </div>

    <div class="profile-grid" :class="{ 'is-two-column': !canViewMemberAccount }">
      <div class="section-card">
        <div class="section-card__header">
          <h3 class="section-card__title">基础资料</h3>
        </div>
        <div class="section-card__body">
          <div class="avatar-editor">
            <div class="avatar-editor__preview" :class="{ 'has-image': Boolean(avatarPreview) }">
              <img v-if="avatarPreview" :src="avatarPreview" alt="avatar" />
              <span v-else>{{ avatarFallback }}</span>
            </div>
            <div class="avatar-editor__hint">
              <strong>头像设置</strong>
              <span>直接从电脑里选择照片上传，聊天和导航会同步显示。</span>
              <el-upload class="avatar-uploader" :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="onAvatarChange">
                <el-button type="primary" :loading="avatarUploading">选择头像</el-button>
              </el-upload>
            </div>
          </div>

          <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-position="top">
            <el-form-item label="账号">
              <el-input :model-value="authStore.user?.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" />
            </el-form-item>
            <el-button type="primary" @click="saveProfile">保存资料</el-button>
          </el-form>
        </div>
      </div>

      <div class="section-card">
        <div class="section-card__header">
          <h3 class="section-card__title">安全设置</h3>
        </div>
        <div class="section-card__body">
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-button type="primary" @click="changePassword">更新密码</el-button>
          </el-form>
        </div>
      </div>

      <div v-if="canViewMemberAccount" class="section-card">
        <div class="section-card__header">
          <h3 class="section-card__title">会员账户</h3>
        </div>
        <div class="section-card__body">
          <div class="metric-card">
            <div class="metric-card__label">当前余额</div>
            <div class="metric-card__value">¥{{ account?.balance ?? 0 }}</div>
            <div class="metric-card__sub">会员等级：{{ account?.memberLevel ?? 'NORMAL' }}</div>
          </div>
          <div class="account-actions">
            <el-button type="primary" @click="goRecharge">立即充值</el-button>
            <span>充值后可在会员中心查看最近充值记录、成长进度和等级规则。</span>
          </div>
        </div>
      </div>
      <div v-if="isUser" class="section-card">
        <div class="section-card__header">
          <h3 class="section-card__title">{{ reviewPanelTitle }}</h3>
        </div>
        <div class="section-card__body">
          <div class="review-toolbar">
            <el-input
              v-model="reviewKeyword"
              clearable
              :placeholder="reviewSearchPlaceholder"
            />
          </div>

          <div v-loading="reviewsLoading" class="review-list">
            <template v-if="pagedDisplayReviews.length">
              <article v-for="review in pagedDisplayReviews" :key="review.id" class="review-card">
                <div class="review-card__head">
                  <div>
                    <strong>{{ review.teaRoomName }}</strong>
                    <span>{{ reviewOrderNoLabel }}：{{ review.orderNo }}</span>
                  </div>
                  <el-rate :model-value="review.rating" disabled text-color="#c58b39" />
                </div>
                <p>{{ review.content || reviewEmptyContentText }}</p>
              </article>
            </template>

            <el-empty
              v-else
              :description="hasAnyReview ? reviewNoMatchText : reviewNoReservationText"
            />
          </div>

          <el-pagination
            v-if="filteredDisplayReviews.length"
            background
            layout="prev, pager, next"
            :current-page="reviewPage"
            :page-size="reviewPageSize"
            :total="filteredDisplayReviews.length"
            :pager-count="5"
            @current-change="handleReviewPageChange"
          />
        </div>
      </div>
    </div>

    <div v-if="recommendations" class="section-card">
      <div class="section-card__header">
        <h3 class="section-card__title">个性化推荐</h3>
      </div>
      <div class="section-card__body">
        <el-table :data="recommendations.recommendedTeas" class="data-table" style="width: 100%">
          <el-table-column prop="name" label="茶叶名称" />
          <el-table-column prop="category" label="分类" />
          <el-table-column prop="price" label="价格" />
          <el-table-column prop="stock" label="库存" />
        </el-table>
      </div>
    </div>

    <div v-if="isUser && userRegistrations.length" class="section-card">
      <div class="section-card__header">
        <h3 class="section-card__title">我的活动报名</h3>
      </div>
      <div class="section-card__body">
        <el-table :data="userRegistrations" class="data-table" style="width: 100%">
          <el-table-column prop="activityId" label="活动 ID" width="120" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.cancelled ? 'danger' : 'success'">{{ row.cancelled ? '已取消' : '有效' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-hero {
  display: flex;
  align-items: center;
  gap: 14px;
}

.profile-avatar,
.avatar-editor__preview {
  display: grid;
  place-items: center;
  overflow: hidden;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(32, 96, 75, 0.16), rgba(220, 176, 111, 0.22));
  color: #21493b;
  font-weight: 800;
}

.profile-avatar {
  width: 58px;
  height: 58px;
  font-size: 24px;
}

.profile-avatar.has-image,
.avatar-editor__preview.has-image {
  background: #eef3ef;
}

.profile-avatar img,
.avatar-editor__preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-hero__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profile-hero__meta strong {
  font-size: 20px;
  color: #234738;
}

.profile-hero__meta span {
  color: #6b7e75;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr);
  gap: 18px;
  margin-top: 18px;
}

.profile-grid.is-two-column {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.avatar-editor {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 22px;
  padding: 16px 18px;
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(245, 240, 230, 0.9), rgba(238, 248, 242, 0.96));
}

.avatar-editor__preview {
  width: 88px;
  height: 88px;
  flex: 0 0 88px;
  font-size: 32px;
}

.avatar-editor__hint {
  display: flex;
  flex-direction: column;
  gap: 10px;
  color: #60756c;
}

.avatar-editor__hint strong {
  color: #234738;
  font-size: 16px;
}

.avatar-uploader {
  margin-top: 2px;
}

.metric-card {
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(135deg, #fffaf1, #f5f8f2);
}

.metric-card__label {
  color: #7a887f;
  font-size: 14px;
}

.metric-card__value {
  margin-top: 10px;
  color: #21493b;
  font-size: 32px;
  font-weight: 800;
}

.metric-card__sub {
  margin-top: 8px;
  color: #6d7a73;
}

.account-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
  color: #6d7a73;
  line-height: 1.7;
}

.review-toolbar {
  margin-bottom: 16px;
}

.review-list {
  min-height: 240px;
}

.review-card + .review-card {
  margin-top: 14px;
}

.review-card {
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(32, 96, 75, 0.08);
  background: linear-gradient(135deg, rgba(255, 250, 241, 0.96), rgba(244, 249, 246, 0.94));
}

.review-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.review-card__head strong {
  display: block;
  color: #234738;
  font-size: 16px;
}

.review-card__head span {
  display: block;
  margin-top: 6px;
  color: #708078;
  font-size: 13px;
}

.review-card p {
  margin: 12px 0 0;
  color: #5d6d66;
  line-height: 1.8;
}

:deep(.review-card .el-rate) {
  min-width: 120px;
}

:deep(.review-list .el-empty) {
  padding: 28px 0 10px;
}

:deep(.review-toolbar .el-input__wrapper) {
  min-height: 42px;
}

:deep(.review-list + .el-pagination) {
  margin-top: 16px;
}

@media (max-width: 1100px) {
  .metric-grid,
  .profile-grid,
  .profile-grid.is-two-column {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .metric-grid,
  .profile-grid,
  .profile-grid.is-two-column {
    grid-template-columns: 1fr;
  }

  .avatar-editor,
  .account-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .review-card__head {
    flex-direction: column;
  }
}
</style>

