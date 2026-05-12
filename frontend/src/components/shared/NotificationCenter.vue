<script setup lang="ts">
// 共享组件：封装可复用的界面块与交互单元。

import { computed, onMounted, watch } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import type { NotificationRecord } from '@/types/business'

const router = useRouter()
const authStore = useAuthStore()
const notificationStore = useNotificationStore()

const notifications = computed(() => notificationStore.items)

async function handleClick(item: NotificationRecord) {
  try {
    await notificationStore.markRead(item)
    if (item.routePath) {
      router.push(item.routePath)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '通知处理失败')
  }
}

async function handleReadAll() {
  try {
    await notificationStore.markAllRead()
    ElMessage.success('已全部标记为已读')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

function handlePopoverShow() {
  notificationStore.ensureConnected()
}

onMounted(() => {
  if (authStore.isAuthenticated) {
    notificationStore.init().catch(() => undefined)
  }
})

watch(
  () => [authStore.isAuthenticated, authStore.user?.id] as const,
  ([isAuthenticated]) => {
    if (!isAuthenticated) {
      notificationStore.reset()
      return
    }
    notificationStore.init().catch(() => undefined)
  },
)
</script>

<template>
  <el-popover placement="bottom-end" :width="360" trigger="click" popper-class="notification-popover" @show="handlePopoverShow">
    <template #reference>
      <el-badge :value="notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount" :hidden="notificationStore.unreadCount < 1">
        <el-button circle>
          <el-icon><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>

    <div class="notice-panel">
      <div class="notice-panel__head">
        <div>
          <h4>通知中心</h4>
          <p>{{ notificationStore.connected ? '实时同步中' : '已开启轮询刷新' }}</p>
        </div>
        <el-button text @click="handleReadAll">全部已读</el-button>
      </div>

      <el-scrollbar max-height="420px">
        <div v-if="notifications.length" class="notice-list">
          <button
            v-for="item in notifications"
            :key="item.id"
            type="button"
            class="notice-card"
            :class="{ 'is-unread': !item.read }"
            @click="handleClick(item)"
          >
            <div class="notice-card__top">
              <strong>{{ item.title }}</strong>
              <span>{{ dayjs(item.createdAt).format('MM-DD HH:mm') }}</span>
            </div>
            <p>{{ item.content }}</p>
          </button>
        </div>
        <el-empty v-else description="暂无通知" :image-size="72" />
      </el-scrollbar>
    </div>
  </el-popover>
</template>

<style scoped>
.notice-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.notice-panel__head h4 {
  margin: 0;
  font-size: 16px;
}

.notice-panel__head p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-card {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  background: #fff;
  text-align: left;
  transition: border-color 0.15s ease, transform 0.15s ease, box-shadow 0.15s ease;
}

.notice-card.is-unread {
  border-color: rgba(230, 162, 60, 0.4);
  background: linear-gradient(135deg, rgba(255, 249, 235, 0.96), rgba(255, 255, 255, 0.98));
}

.notice-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.notice-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.notice-card__top strong {
  font-size: 14px;
}

.notice-card__top span {
  color: var(--muted);
  font-size: 12px;
  white-space: nowrap;
}

.notice-card p {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.65;
}
</style>
