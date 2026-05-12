<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { useRouter } from 'vue-router'
import { Monitor, UserFilled } from '@element-plus/icons-vue'
import LoginShell from '@/views/auth/components/LoginShell.vue'

const router = useRouter()

function go(path: string) {
  router.push(path)
}
</script>

<template>
  <LoginShell
    title="选择平台入口"
    subtitle="根据你的身份进入管理端或客户端。"
    heading="一个平台，两种使用场景"
    description="管理员负责平台运营与配置管理，客户端承载用户预约、购茶和茶室员现场服务处理。"
    :highlights="[
      { title: '管理端', description: '适用于平台管理、订单履约、用户维护、统计报表与系统配置。' },
      { title: '客户端', description: '适用于普通用户预约购茶，以及茶室员处理预约与现场服务。' },
    ]"
    variant="neutral"
  >
    <div class="portal-grid">
      <button type="button" class="portal-card" @click="go('/login/admin')">
        <span class="portal-card__icon">
          <el-icon :size="22"><Monitor /></el-icon>
        </span>
        <span class="portal-card__main">
          <span class="portal-card__title">管理端</span>
          <span class="portal-card__desc">管理员运营控制台</span>
        </span>
        <span class="portal-card__meta">进入</span>
      </button>

      <button type="button" class="portal-card portal-card--client" @click="go('/login/client')">
        <span class="portal-card__icon">
          <el-icon :size="22"><UserFilled /></el-icon>
        </span>
        <span class="portal-card__main">
          <span class="portal-card__title">客户端</span>
          <span class="portal-card__desc">普通用户与茶室员服务入口</span>
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
    linear-gradient(180deg, rgba(255, 255, 255, 0.76), rgba(248, 242, 232, 0.96)),
    linear-gradient(145deg, rgba(123, 88, 50, 0.09), transparent);
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

.portal-card--client {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.74), rgba(238, 246, 255, 0.96)),
    linear-gradient(145deg, rgba(58, 121, 200, 0.12), transparent);
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
  background: rgba(123, 88, 50, 0.12);
  border: 1px solid rgba(123, 88, 50, 0.16);
  color: var(--brand-deep);
}

.portal-card--client .portal-card__icon {
  background: rgba(58, 121, 200, 0.12);
  border-color: rgba(58, 121, 200, 0.18);
  color: #1f4c87;
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
