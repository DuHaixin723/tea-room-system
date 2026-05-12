<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tickets, UserFilled } from '@element-plus/icons-vue'
import LoginShell from '@/views/auth/components/LoginShell.vue'

const route = useRoute()
const router = useRouter()
const redirect = computed(() => (typeof route.query.redirect === 'string' ? route.query.redirect : ''))
const targetQuery = computed(() => (redirect.value ? { redirect: redirect.value } : {}))

function go(path: string) {
  router.push({ path, query: targetQuery.value })
}
</script>

<template>
  <LoginShell
    title="选择登录身份"
    subtitle="客户端分为普通用户与茶室员两个使用入口。"
    heading="选择适合你的服务入口"
    description="无论是预约茶室、购买茶叶，还是处理预约接待与现场服务，都从这里进入更合适的工作界面。"
    :highlights="[
      { title: '普通用户', description: '适用于预约茶室、购茶下单、查看订单、收藏与个人中心。' },
      { title: '茶室员', description: '适用于处理预约确认、现场接待、活动执行与服务反馈。' },
    ]"
    variant="client"
  >
    <div class="portal-grid">
      <button type="button" class="portal-card" @click="go('/login/user')">
        <span class="portal-card__icon">
          <el-icon :size="22"><UserFilled /></el-icon>
        </span>
        <span class="portal-card__main">
          <span class="portal-card__title">普通用户入口</span>
          <span class="portal-card__desc">预约茶室、购买茶叶、查看订单</span>
        </span>
        <span class="portal-card__meta">进入</span>
      </button>

      <button type="button" class="portal-card portal-card--staff" @click="go('/login/staff')">
        <span class="portal-card__icon">
          <el-icon :size="22"><Tickets /></el-icon>
        </span>
        <span class="portal-card__main">
          <span class="portal-card__title">茶室员入口</span>
          <span class="portal-card__desc">确认预约、接待到店、处理现场事务</span>
        </span>
        <span class="portal-card__meta">进入</span>
      </button>
    </div>
  </LoginShell>
</template>

<style scoped>
.portal-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

.portal-card {
  appearance: none;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.74), rgba(238, 246, 255, 0.96)),
    linear-gradient(145deg, rgba(58, 121, 200, 0.12), transparent);
  padding: 16px 18px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 14px;
  align-items: center;
  cursor: pointer;
  transition:
    transform 140ms ease,
    border-color 140ms ease,
    box-shadow 140ms ease;
  text-align: left;
}

.portal-card--staff {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.74), rgba(245, 238, 255, 0.96)),
    linear-gradient(145deg, rgba(124, 77, 200, 0.12), transparent);
}

.portal-card:hover {
  transform: translateY(-2px);
  border-color: var(--line-strong);
  box-shadow: 0 16px 35px rgba(58, 42, 23, 0.12);
}

.portal-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: rgba(58, 121, 200, 0.12);
  border: 1px solid rgba(58, 121, 200, 0.18);
  color: #1f4c87;
}

.portal-card--staff .portal-card__icon {
  background: rgba(124, 77, 200, 0.12);
  border-color: rgba(124, 77, 200, 0.18);
  color: #4f2a87;
}

.portal-card__main {
  display: grid;
  gap: 6px;
}

.portal-card__title {
  font-weight: 750;
  letter-spacing: -0.03em;
  font-size: 16px;
}

.portal-card__desc,
.portal-card__meta {
  color: var(--muted);
  font-size: 13px;
}
</style>
