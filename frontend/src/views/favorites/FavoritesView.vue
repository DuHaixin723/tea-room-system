<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { favoriteApi, teaApi, teaRoomApi } from '@/api/modules/management'
import { useAuthStore } from '@/stores/auth'
import { resolveAssetUrl } from '@/utils/asset'
import type { FavoriteRecord, TeaRecord, TeaRoomRecord } from '@/types/business'
import { usePagedTable } from '@/composables/usePagedTable'
import PageIntro from '@/components/shared/PageIntro.vue'

interface FavoriteDisplayRow extends FavoriteRecord {
  imageUrl?: string
  targetName: string
  targetSubtitle: string
  targetDescription: string
}

const authStore = useAuthStore()
const keyword = ref('')
const teaMap = ref(new Map<number, TeaRecord>())
const teaRoomMap = ref(new Map<number, TeaRoomRecord>())

const table = usePagedTable<FavoriteRecord>((params) => {
  const requestParams = { ...params }
  if (authStore.user?.id) requestParams.userId = authStore.user.id
  return favoriteApi.page(requestParams)
})

const visibleRows = computed<FavoriteDisplayRow[]>(() => {
  const query = keyword.value.trim().toLowerCase()

  return table.rows
    .map((row) => ({
      ...row,
      ...detailOf(row),
    }))
    .filter((row) => {
      if (!query) return true
      return [
        String(row.id),
        String(row.targetId),
        row.targetType,
        String(row.userId),
        row.targetName,
        row.targetSubtitle,
        row.targetDescription,
      ]
        .join(' ')
        .toLowerCase()
        .includes(query)
    })
})

function teaImageUrl(tea?: TeaRecord) {
  return resolveAssetUrl(tea?.imageUrl) || 'https://images.unsplash.com/photo-1563822249548-9a72b6353cd1?auto=format&fit=crop&w=900&q=80'
}

function roomImageUrl(room?: TeaRoomRecord) {
  return resolveAssetUrl(room?.imageUrl) || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1200&q=80'
}

function detailOf(row: FavoriteRecord) {
  if (row.targetType === 'TEA') {
    const tea = teaMap.value.get(row.targetId)
    if (tea) {
      return {
        imageUrl: teaImageUrl(tea),
        targetName: tea.name,
        targetSubtitle: `${tea.category} · ￥${tea.price.toFixed(2)}`,
        targetDescription: tea.description || `库存 ${tea.stock}，${tea.enabled ? '当前可购买' : '当前已下架'}`,
      }
    }
    return {
      imageUrl: undefined,
      targetName: `茶叶 #${row.targetId}`,
      targetSubtitle: '茶叶商品',
      targetDescription: '未找到商品详情',
    }
  }

  if (row.targetType === 'TEA_ROOM') {
    const teaRoom = teaRoomMap.value.get(row.targetId)
    if (teaRoom) {
      return {
        imageUrl: roomImageUrl(teaRoom),
        targetName: teaRoom.name,
        targetSubtitle: `茶室 · 容量 ${teaRoom.capacity} 人`,
        targetDescription: teaRoom.location || teaRoom.description || (teaRoom.enabled ? '当前可预约' : '当前停用'),
      }
    }
    return {
      imageUrl: undefined,
      targetName: `茶室 #${row.targetId}`,
      targetSubtitle: '茶室资源',
      targetDescription: '未找到茶室详情',
    }
  }

  return {
    imageUrl: undefined,
    targetName: `目标 #${row.targetId}`,
    targetSubtitle: row.targetType,
    targetDescription: '暂不支持的收藏类型',
  }
}

async function loadReferenceData() {
  const [teaPage, teaRoomPage] = await Promise.all([
    teaApi.page({ page: 0, size: 200 }),
    teaRoomApi.rooms({ page: 0, size: 200 }),
  ])

  teaMap.value = new Map((teaPage.content ?? []).map((item) => [item.id, item]))
  teaRoomMap.value = new Map((teaRoomPage.content ?? []).map((item) => [item.id, item]))
}

async function loadAll() {
  await Promise.all([table.load(), loadReferenceData()])
}

async function remove(row: FavoriteRecord) {
  await favoriteApi.remove(row.id)
  ElMessage.success('已取消收藏')
  await table.load()
}

onMounted(() => {
  void loadAll()
})
</script>

<template>
  <div class="page-shell">
    <PageIntro title="我的收藏" description="集中查看收藏记录，支持快速取消收藏，并直接查看收藏商品和茶室详情。" />

    <div class="section-card">
      <div class="section-card__body">
        <div class="toolbar">
          <div class="toolbar-left">
            <el-input v-model="keyword" clearable placeholder="搜索 收藏ID / 名称 / 类型 / 位置" style="width: 360px" />
          </div>
        </div>

        <el-table v-loading="table.loading" :data="visibleRows" class="data-table">
          <el-table-column prop="id" label="收藏ID" width="100" />
          <el-table-column label="图片" width="160">
            <template #default="{ row }">
              <div class="favorite-thumb">
                <el-image
                  v-if="row.imageUrl"
                  :src="row.imageUrl"
                  :preview-src-list="[row.imageUrl]"
                  preview-teleported
                  fit="cover"
                  class="favorite-thumb__image"
                />
                <div v-else class="favorite-thumb__empty">无图</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="收藏内容" min-width="460">
            <template #default="{ row }">
              <div class="favorite-target">
                <div class="favorite-target__name">{{ row.targetName }}</div>
                <div class="favorite-target__meta">{{ row.targetSubtitle }}</div>
                <div class="favorite-target__desc">{{ row.targetDescription }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-tag :type="row.targetType === 'TEA' ? 'success' : 'warning'" effect="plain">
                {{ row.targetType === 'TEA' ? '茶叶' : row.targetType === 'TEA_ROOM' ? '茶室' : row.targetType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetId" label="目标ID" width="120" />
          <el-table-column fixed="right" label="操作" width="120">
            <template #default="{ row }">
              <el-button link type="danger" @click="remove(row)">取消</el-button>
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
  </div>
</template>

<style scoped>
.favorite-thumb {
  display: flex;
  align-items: center;
}

.favorite-thumb__image {
  width: 104px;
  height: 104px;
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: #f6f3ed;
}

.favorite-thumb__empty {
  display: grid;
  place-items: center;
  width: 104px;
  height: 104px;
  border-radius: 18px;
  background: #f6f3ed;
  color: var(--muted);
  font-size: 14px;
}

.favorite-target {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.favorite-target__name {
  color: var(--text);
  font-size: 20px;
  font-weight: 700;
}

.favorite-target__meta {
  color: var(--brand);
  font-size: 15px;
}

.favorite-target__desc {
  color: var(--muted);
  font-size: 15px;
  line-height: 1.7;
}

:deep(.data-table .cell) {
  padding-top: 14px;
  padding-bottom: 14px;
}
</style>
