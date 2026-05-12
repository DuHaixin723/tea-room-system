<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoginShell from '@/views/auth/components/LoginShell.vue'
import LoginFormCard from '@/views/auth/components/LoginFormCard.vue'

const route = useRoute()
const router = useRouter()
const redirect = computed(() => (typeof route.query.redirect === 'string' ? route.query.redirect : ''))
const targetQuery = computed(() => (redirect.value ? { redirect: redirect.value } : {}))

function goRegister() {
  router.push({ path: '/register/user', query: targetQuery.value })
}

function goStaffLogin() {
  router.push({ path: '/login/staff', query: targetQuery.value })
}
</script>

<template>
  <LoginShell
    title="欢迎回来"
    subtitle="用户登录后可预约茶室、购买茶叶、报名活动并使用在线咨询。"
    heading="一个入口完成预约、购物与服务"
    description="面向普通用户提供统一入口，围绕茶室预约、商城购买、活动报名和个人中心形成完整体验。"
    :highlights="[
      { title: '预约更顺手', description: '查看茶室时段、提交预约并跟踪预约状态。' },
      { title: '购物更直接', description: '支持茶叶商城下单，并与预约场景联动。' },
    ]"
    variant="client"
  >
    <LoginFormCard portal="client" submit-label="进入用户端" :allow-roles="['USER']">
      <template #actions>
        <div class="entry-actions">
          <button type="button" class="entry-cta entry-cta--primary" @click="goRegister">
            <span class="entry-cta__label">没有账号？立即注册</span>
            <span class="entry-cta__hint">普通用户可直接注册并登录</span>
          </button>
          <button type="button" class="entry-cta entry-cta--ghost" @click="goStaffLogin">
            <span class="entry-cta__label">切换到茶室员入口</span>
            <span class="entry-cta__hint">进入茶室员登录与工作台</span>
          </button>
        </div>
      </template>
    </LoginFormCard>
  </LoginShell>
</template>

<style scoped>
.entry-actions {
  display: grid;
  gap: 10px;
}

.entry-cta {
  appearance: none;
  width: 100%;
  border-radius: 18px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 14px 16px;
  cursor: pointer;
  transition: transform 140ms ease, box-shadow 140ms ease, border-color 140ms ease;
}

.entry-cta:hover {
  transform: translateY(-1px);
}

.entry-cta--primary {
  border: 1px solid rgba(58, 121, 200, 0.18);
  background:
    linear-gradient(180deg, rgba(236, 244, 255, 0.98), rgba(226, 238, 255, 0.94)),
    linear-gradient(135deg, rgba(58, 121, 200, 0.1), transparent);
  color: #21548f;
  box-shadow: 0 12px 28px rgba(58, 121, 200, 0.1);
}

.entry-cta--primary:hover {
  border-color: rgba(58, 121, 200, 0.3);
  box-shadow: 0 16px 32px rgba(58, 121, 200, 0.16);
}

.entry-cta--ghost {
  border: 1px solid rgba(88, 72, 53, 0.14);
  background: rgba(255, 255, 255, 0.88);
  color: #4f4031;
}

.entry-cta--ghost:hover {
  border-color: rgba(88, 72, 53, 0.24);
  box-shadow: 0 12px 28px rgba(74, 56, 35, 0.1);
}

.entry-cta__label {
  font-size: 14px;
  font-weight: 700;
}

.entry-cta__hint {
  font-size: 12px;
  color: rgba(33, 84, 143, 0.78);
}

.entry-cta--ghost .entry-cta__hint {
  color: rgba(79, 64, 49, 0.72);
}
</style>
