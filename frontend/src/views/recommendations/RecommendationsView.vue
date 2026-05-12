<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { fetchRecommendations } from '@/api/modules/dashboard'
import type { RecommendationRecord } from '@/types/business'
import PageIntro from '@/components/shared/PageIntro.vue'

const authStore = useAuthStore()
const router = useRouter()
const data = ref<RecommendationRecord | null>(null)
const loading = ref(true)

const teaCards = computed(() => data.value?.recommendedTeas ?? [])
const roomCards = computed(() => data.value?.recommendedTeaRooms ?? [])

onMounted(async () => {
  try {
    if (authStore.user?.id) {
      data.value = await fetchRecommendations(authStore.user.id)
    }
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-shell">
    <PageIntro
      title="个性化推荐"
      description="综合收藏、预约、订单和评价偏好，优先推荐更贴近你口味与场景的茶叶和茶室。"
    />

    <div class="recommendation-summary">
      <div class="summary-card">
        <span>推荐茶叶</span>
        <strong>{{ teaCards.length }}</strong>
        <p>根据你偏好的茶类、价格区间和历史下单行为筛选。</p>
      </div>
      <div class="summary-card">
        <span>推荐茶室</span>
        <strong>{{ roomCards.length }}</strong>
        <p>结合预约记录、常用人数范围和常去位置优先排序。</p>
      </div>
    </div>

    <div class="recommend-grid">
      <section class="section-card">
        <div class="section-card__header">
          <h3 class="section-card__title">为你推荐的茶叶</h3>
        </div>
        <div class="section-card__body">
          <el-skeleton v-if="loading" :rows="5" animated />
          <div v-else class="card-list">
            <article v-for="tea in teaCards" :key="tea.id" class="recommend-card">
              <div class="recommend-card__meta">茶叶推荐</div>
              <h4>{{ tea.name }}</h4>
              <p>{{ tea.description || '匹配你的品类偏好与价格接受区间。' }}</p>
              <div class="recommend-card__facts">
                <span>{{ tea.category }}</span>
                <span>库存 {{ tea.stock }}</span>
              </div>
              <div class="recommend-card__footer">
                <strong>￥{{ tea.price }}</strong>
                <el-button type="primary" plain @click="router.push('/teas')">查看茶叶</el-button>
              </div>
            </article>
          </div>
        </div>
      </section>

      <section class="section-card">
        <div class="section-card__header">
          <h3 class="section-card__title">更适合你的茶室</h3>
        </div>
        <div class="section-card__body">
          <el-skeleton v-if="loading" :rows="5" animated />
          <div v-else class="card-list">
            <article v-for="room in roomCards" :key="room.id" class="recommend-card">
              <div class="recommend-card__meta">茶室推荐</div>
              <h4>{{ room.name }}</h4>
              <p>{{ room.description || '结合你的预约场景与偏好位置进行推荐。' }}</p>
              <div class="recommend-card__facts">
                <span>{{ room.location || '位置待补充' }}</span>
                <span>{{ room.capacity }} 人</span>
              </div>
              <div class="recommend-card__footer">
                <strong>{{ room.enabled ? '可预约' : '暂停开放' }}</strong>
                <el-button type="primary" plain @click="router.push({ path: '/tea-rooms', query: { bookRoomId: String(room.id) } })">
                  去预约
                </el-button>
              </div>
            </article>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.recommendation-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.summary-card {
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(247, 243, 236, 0.96));
}

.summary-card span {
  color: var(--muted);
  font-size: 13px;
}

.summary-card strong {
  display: block;
  margin-top: 10px;
  font-size: 30px;
}

.summary-card p {
  margin: 10px 0 0;
  color: var(--muted);
  line-height: 1.7;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.card-list {
  display: grid;
  gap: 14px;
}

.recommend-card {
  padding: 18px;
  border: 1px solid rgba(123, 88, 50, 0.14);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(249, 245, 239, 0.96)),
    linear-gradient(135deg, rgba(215, 162, 85, 0.08), transparent 60%);
}

.recommend-card__meta {
  color: #9a835f;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.recommend-card h4 {
  margin: 10px 0 8px;
  font-size: 22px;
  letter-spacing: -0.03em;
}

.recommend-card p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.recommend-card__facts,
.recommend-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.recommend-card__facts {
  margin-top: 14px;
  color: var(--muted);
  font-size: 13px;
}

.recommend-card__footer {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(123, 88, 50, 0.1);
}

@media (max-width: 960px) {
  .recommendation-summary,
  .recommend-grid {
    grid-template-columns: 1fr;
  }
}
</style>
