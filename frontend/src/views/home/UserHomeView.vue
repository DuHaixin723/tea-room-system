<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { activityApi, teaApi, teaRoomApi } from '@/api/modules/management'
import { fetchRecommendations } from '@/api/modules/dashboard'
import { useAuthStore } from '@/stores/auth'
import { resolveAssetUrl } from '@/utils/asset'
import type {
  ActivityRecord,
  RecommendationRecord,
  TeaRecord,
  TeaRoomRecord,
} from '@/types/business'
import { formatStatus } from '@/utils/status'

const authStore = useAuthStore()
const router = useRouter()

const loading = ref(true)
const recommended = ref<RecommendationRecord | null>(null)
const teaRooms = ref<TeaRoomRecord[]>([])
const teas = ref<TeaRecord[]>([])
const activities = ref<ActivityRecord[]>([])
const alive = ref(true)

const nickname = computed(() => authStore.user?.nickname || authStore.user?.username || '用户')
const publishedActivities = computed(() =>
  [...activities.value]
    .filter((item) => item.status === 'PUBLISHED')
    .sort((a, b) => dayjs(a.startTime).valueOf() - dayjs(b.startTime).valueOf()),
)
const carouselActivities = computed(() => publishedActivities.value.slice(0, 5))
const latestActivities = computed(() => publishedActivities.value.slice(0, 3))
const roomShowcase = computed(() => {
  const source = recommended.value?.recommendedTeaRooms?.length ? recommended.value.recommendedTeaRooms : teaRooms.value
  return source.slice(0, 4)
})
const teaShowcase = computed(() => {
  const source = recommended.value?.recommendedTeas?.length ? recommended.value.recommendedTeas : teas.value
  return source.slice(0, 6)
})
const roomImage = (room: TeaRoomRecord) => resolveAssetUrl(room.imageUrl) || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1200&q=80'
const teaImage = (tea: TeaRecord) => resolveAssetUrl(tea.imageUrl) || 'https://images.unsplash.com/photo-1563822249548-9a72b6353cd1?auto=format&fit=crop&w=900&q=80'
const activityImage = (activity: ActivityRecord) => resolveAssetUrl(activity.imageUrl) || 'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=1200&q=80'
const activitySummary = (activity: ActivityRecord) => activity.content?.trim() || '查看活动时间、报名名额与现场安排。'

async function load() {
  loading.value = true
  try {
    const userId = authStore.user?.id
    const [roomPage, teaPage, activityPage, rec] = await Promise.all([
      teaRoomApi.rooms({ page: 0, size: 8, sort: 'id,desc' }),
      teaApi.page({ page: 0, size: 8, sort: 'id,desc' }),
      activityApi.page({ page: 0, size: 12, sort: 'id,desc' }),
      userId ? fetchRecommendations(userId) : Promise.resolve({ recommendedTeas: [], recommendedTeaRooms: [] }),
    ])

    teaRooms.value = roomPage.content ?? []
    teas.value = teaPage.content ?? []
    activities.value = activityPage.content ?? []
    recommended.value = rec ?? null
  } catch (error) {
    console.error(error)
    if (!alive.value || !authStore.isAuthenticated) return
    ElMessage.error('首页加载失败')
  } finally {
    if (alive.value) {
      loading.value = false
    }
  }
}

function goBookRoom(roomId: number) {
  router.push({ path: '/tea-rooms', query: { bookRoomId: String(roomId) } })
}

onMounted(load)
onBeforeUnmount(() => {
  alive.value = false
})
</script>

<template>
  <div class="user-home">
    <section class="home-carousel section-card">
      <div class="section-card__body carousel-body">
        <el-skeleton v-if="loading" :rows="6" animated />
        <el-empty v-else-if="!carouselActivities.length" description="暂时还没有已发布活动" />
        <el-carousel v-else class="activity-carousel" height="360px" indicator-position="outside" trigger="click" autoplay>
          <el-carousel-item v-for="activity in carouselActivities" :key="activity.id">
            <article class="activity-slide" @click="router.push('/activities')">
              <div class="activity-slide__media">
                <div class="activity-slide__image" :style="{ backgroundImage: `url(${activityImage(activity)})` }"></div>
              </div>

              <div class="activity-slide__content">
                <div class="activity-slide__meta">
                  <span>{{ formatStatus('activity', activity.status) }}</span>
                  <span>{{ dayjs(activity.startTime).format('MM-DD HH:mm') }}</span>
                </div>

                <h3>{{ activity.title }}</h3>
                <p>{{ activitySummary(activity) }}</p>

                <div class="activity-slide__facts">
                  <div class="activity-slide__fact">
                    <label>活动时间</label>
                    <strong>{{ dayjs(activity.startTime).format('YYYY-MM-DD HH:mm') }}</strong>
                  </div>
                  <div class="activity-slide__fact">
                    <label>结束时间</label>
                    <strong>{{ dayjs(activity.endTime).format('YYYY-MM-DD HH:mm') }}</strong>
                  </div>
                </div>

                <div class="activity-slide__actions">
                  <el-button type="primary" @click.stop="router.push('/activities')">查看活动</el-button>
                  <el-button @click.stop="router.push('/consultations')">在线咨询</el-button>
                </div>
              </div>
            </article>
          </el-carousel-item>
        </el-carousel>
      </div>
    </section>

    <section class="hero-board">
      <div class="hero-board__intro">
        <div class="identity-chip">
          <span class="identity-chip__tag">USER HOME</span>
          <strong>普通用户首页</strong>
        </div>
        <span class="hero-board__eyebrow">Tea Lifestyle Portal</span>
        <h2>{{ nickname }}，今天想预订茶室、挑选茶叶，还是参加一场活动？</h2>
        <p>首页首屏先展示活动内容，下面保留你的常用入口和数据概览，信息会更集中，也更容易看清。</p>
        <div class="hero-board__actions">
          <el-button type="primary" size="large" @click="router.push('/activities')">查看活动</el-button>
          <el-button size="large" @click="router.push('/tea-rooms')">预约茶室</el-button>
          <el-button size="large" @click="router.push('/teas')">茶叶商城</el-button>
        </div>
      </div>
    </section>

    <section class="quick-strip">
      <button type="button" class="quick-card" @click="router.push('/favorites')">
        <span>我的收藏</span>
        <strong>查看收藏的茶室、茶叶和感兴趣内容</strong>
      </button>
      <button type="button" class="quick-card" @click="router.push('/orders')">
        <span>订单中心</span>
        <strong>统一查看支付状态、金额和订单明细</strong>
      </button>
      <button type="button" class="quick-card" @click="router.push('/consultations')">
        <span>在线咨询</span>
        <strong>随时联系茶室员，快速确认活动和预约问题</strong>
      </button>
    </section>

    <section class="content-grid">
      <div class="section-card">
        <div class="section-card__body">
          <div class="section-head">
            <div>
              <span class="section-tag">Tea Rooms</span>
              <h3>茶室推荐</h3>
            </div>
            <el-button text @click="router.push('/tea-rooms')">全部茶室</el-button>
          </div>

          <div class="room-grid">
            <article v-for="room in roomShowcase" :key="room.id" class="room-card" @click="goBookRoom(room.id)">
              <div class="room-card__image" :style="{ backgroundImage: `url(${roomImage(room)})` }"></div>
              <div class="room-card__body">
                <div class="room-card__top">
                  <h4>{{ room.name }}</h4>
                  <span class="room-card__status" :class="{ 'is-on': room.enabled }">
                    {{ room.enabled ? '可预约' : '暂停开放' }}
                  </span>
                </div>
                <p>{{ room.description || '适合会客、静坐和小型品茗活动。' }}</p>
                <div class="room-card__meta">
                  <span>{{ room.location || '位置待补充' }}</span>
                  <span>容量 {{ room.capacity }} 人</span>
                </div>
              </div>
            </article>
          </div>
        </div>
      </div>

      <div class="section-card">
        <div class="section-card__body">
          <div class="section-head">
            <div>
              <span class="section-tag">Tea Selection</span>
              <h3>茶叶精选</h3>
            </div>
            <el-button text @click="router.push('/teas')">进入商城</el-button>
          </div>

          <div class="tea-grid">
            <article v-for="tea in teaShowcase" :key="tea.id" class="tea-card" @click="router.push('/teas')">
              <div class="tea-card__image" :style="{ backgroundImage: `url(${teaImage(tea)})` }"></div>
              <div class="tea-card__body">
                <strong>{{ tea.name }}</strong>
                <span>{{ tea.category }}</span>
                <em>¥{{ tea.price }}</em>
              </div>
            </article>
          </div>
        </div>
      </div>
    </section>

    <section class="content-grid content-grid--single">
      <div class="section-card">
        <div class="section-card__body">
          <div class="section-head">
            <div>
              <span class="section-tag">Latest Activities</span>
              <h3>更多活动</h3>
            </div>
            <el-button text @click="router.push('/activities')">查看全部</el-button>
          </div>

          <div v-if="latestActivities.length" class="activity-grid">
            <article v-for="activity in latestActivities" :key="activity.id" class="activity-card" @click="router.push('/activities')">
              <div class="activity-card__cover" :style="{ backgroundImage: `url(${activityImage(activity)})` }"></div>
              <div class="activity-card__body">
                <div class="activity-card__badge">{{ formatStatus('activity', activity.status) }}</div>
                <h4>{{ activity.title }}</h4>
                <p>{{ activitySummary(activity) }}</p>
                <span>{{ dayjs(activity.startTime).format('MM-DD HH:mm') }}</span>
              </div>
            </article>
          </div>
          <el-empty v-else description="暂无活动内容" />
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.user-home {
  --home-content-width: 1480px;
  display: flex;
  flex-direction: column;
  width: min(100%, var(--home-content-width));
  margin: 0 auto;
  gap: 18px;
}

.hero-board,
.content-grid {
  display: grid;
  gap: 18px;
}

.home-carousel,
.hero-board {
  width: 100%;
  margin-right: auto;
  margin-left: auto;
}

.carousel-body {
  padding-top: 16px;
}

.activity-carousel {
  overflow: hidden;
  border-radius: 28px;
}

.activity-slide {
  display: grid;
  grid-template-columns: minmax(260px, 0.88fr) minmax(0, 1.12fr);
  gap: 18px;
  height: 360px;
  padding: 18px;
  border-radius: 28px;
  background:
    linear-gradient(135deg, rgba(255, 249, 239, 0.98), rgba(241, 249, 245, 0.96)),
    #fff;
  cursor: pointer;
}

.activity-slide__media {
  min-width: 0;
}

.activity-slide__image {
  height: 100%;
  border-radius: 22px;
  background-position: center;
  background-size: cover;
}

.activity-slide__content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding: 6px 4px;
}

.activity-slide__meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.activity-slide__meta span,
.hero-board__eyebrow,
.section-tag,
.activity-card__badge {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(38, 106, 84, 0.08);
  color: #2f6a56;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.activity-slide h3 {
  margin: 14px 0 10px;
  color: #1f4537;
  font-size: 32px;
  line-height: 1.18;
}

.activity-slide p {
  margin: 0;
  color: #617f73;
  font-size: 15px;
  line-height: 1.8;
}

.activity-slide__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.activity-slide__fact {
  padding: 14px 16px;
  border: 1px solid rgba(32, 96, 75, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
}

.activity-slide__fact label {
  display: block;
  color: #678174;
  font-size: 12px;
}

.activity-slide__fact strong {
  display: block;
  margin-top: 8px;
  color: #21493b;
  font-size: 16px;
  line-height: 1.5;
}

.activity-slide__actions,
.hero-board__actions,
.quick-strip,
.section-head,
.room-card__top,
.room-card__meta {
  display: flex;
  align-items: center;
}

.activity-slide__actions {
  gap: 12px;
  margin-top: 20px;
}

.hero-board {
  padding: 28px;
  border-radius: 34px;
  background:
    radial-gradient(circle at top left, rgba(255, 223, 169, 0.42), transparent 28%),
    radial-gradient(circle at right bottom, rgba(163, 215, 193, 0.3), transparent 24%),
    linear-gradient(135deg, #fff8ee 0%, #f3faf7 48%, #eaf6f0 100%);
  box-shadow: 0 24px 58px rgba(26, 74, 58, 0.08);
}

.identity-chip {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(31, 90, 71, 0.12);
  box-shadow: 0 12px 26px rgba(31, 90, 71, 0.08);
}

.identity-chip__tag {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: #1f5a47;
  color: #fffaf3;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.identity-chip strong {
  color: #214b3d;
  font-size: 15px;
  font-weight: 800;
}

.hero-board h2 {
  margin: 16px 0 12px;
  color: #1f4638;
  font-size: 42px;
  line-height: 1.08;
}

.hero-board p,
.room-card p,
.activity-card__body p {
  margin: 0;
  color: #617f73;
  line-height: 1.8;
}

.hero-board__actions {
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 22px;
}

.quick-strip {
  width: 100%;
  margin-right: auto;
  margin-left: auto;
  gap: 14px;
}

.quick-card {
  flex: 1;
  padding: 18px 20px;
  border: 1px solid rgba(27, 92, 72, 0.1);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.84);
  text-align: left;
}

.quick-card span,
.quick-card strong {
  display: block;
}

.quick-card span {
  color: #2f6b57;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.quick-card strong {
  margin-top: 8px;
  color: #24483c;
  font-size: 18px;
}

.content-grid {
  width: 100%;
  margin-right: auto;
  margin-left: auto;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.content-grid--single {
  grid-template-columns: minmax(0, 1fr);
}

.section-head {
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.section-head h3 {
  margin: 10px 0 6px;
  color: #1f4638;
  font-size: 28px;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.room-card,
.tea-card,
.activity-card {
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 26px;
  background: #fff;
  box-shadow: 0 18px 40px rgba(19, 54, 42, 0.08);
  cursor: pointer;
}

.room-card__image,
.tea-card__image,
.activity-card__cover {
  background-position: center;
  background-size: cover;
}

.room-card__image {
  height: 220px;
}

.room-card__body,
.activity-card__body {
  padding: 18px;
}

.room-card__top {
  justify-content: space-between;
  gap: 12px;
}

.room-card__top h4,
.activity-card__body h4 {
  margin: 0;
  color: #224738;
  font-size: 22px;
}

.room-card__status {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(107, 114, 128, 0.12);
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
}

.room-card__status.is-on {
  background: rgba(37, 151, 97, 0.12);
  color: #24734f;
}

.room-card__meta {
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.room-card__meta span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f3f1ea;
  color: #5f6e67;
  font-size: 12px;
}

.tea-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.tea-card__image {
  height: 170px;
}

.tea-card__body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
}

.tea-card__body strong {
  color: #24463a;
  font-size: 16px;
}

.tea-card__body span {
  color: #6a8579;
  font-size: 12px;
}

.tea-card__body em {
  color: #1e5a45;
  font-style: normal;
  font-weight: 800;
}

.tea-card__body em {
  font-size: 18px;
}

.activity-grid {
  display: grid;
  gap: 14px;
}

.activity-card {
  display: grid;
  grid-template-columns: 170px minmax(0, 1fr);
}

.activity-card__cover {
  min-height: 100%;
}

.activity-card__body span {
  display: block;
  margin-top: 14px;
  color: #6a8579;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .hero-board,
  .content-grid,
  .room-grid,
  .activity-slide {
    grid-template-columns: 1fr;
  }

  .tea-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .activity-slide {
    height: auto;
  }

  .activity-slide__media {
    min-height: 220px;
  }
}

@media (max-width: 768px) {
  .user-home {
    gap: 16px;
  }

  .hero-board {
    padding: 22px;
  }

  .hero-board h2 {
    font-size: 32px;
  }

  .quick-strip {
    display: grid;
  }

  .tea-grid,
  .activity-card,
  .activity-slide__facts {
    grid-template-columns: 1fr;
  }

  .activity-slide h3 {
    font-size: 28px;
  }

  .activity-slide__actions {
    flex-wrap: wrap;
  }

  .activity-card__cover {
    height: 190px;
  }
}
</style>
