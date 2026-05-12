<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LoginShell from '@/views/auth/components/LoginShell.vue'
import LoginFormCard from '@/views/auth/components/LoginFormCard.vue'

const route = useRoute()
const router = useRouter()
const redirect = computed(() => (typeof route.query.redirect === 'string' ? route.query.redirect : ''))
const switchQuery = computed(() => (redirect.value ? { redirect: redirect.value } : {}))
const CLIENT_PORT = '5713'

function switchToClient() {
  if (typeof window !== 'undefined') {
    const url = `${window.location.protocol}//${window.location.hostname}:${CLIENT_PORT}/login`
    window.location.href = redirect.value ? `${url}?redirect=${encodeURIComponent(redirect.value)}` : url
    return
  }
  router.push({ path: '/login/client', query: switchQuery.value })
}
</script>

<template>
  <LoginShell
    title="管理员登录"
    subtitle="用于系统管理、用户维护、预约与订单履约管理。"
    heading="统一运营控制台"
    description="管理员可在此处理资源维护、订单状态、充值记录、推荐策略和全局配置，负责平台整体运营管理。"
    :highlights="[
      { title: '权限清晰', description: '仅管理员账号可进入，避免角色混用。' },
      { title: '运营集中', description: '预约、订单、用户、报表与配置统一管理。' },
    ]"
    variant="admin"
  >
    <el-alert
      title="管理员专用入口"
      type="warning"
      :closable="false"
      show-icon
      class="login-tip"
      description="普通用户和茶室员请从客户端入口登录。"
    />
    <LoginFormCard portal="admin" submit-label="进入管理端" :allow-roles="['ADMIN']">
      <template #actions>
        <el-link type="primary" :underline="false" @click="switchToClient">前往客户端登录</el-link>
      </template>
    </LoginFormCard>
  </LoginShell>
</template>

<style scoped>
.login-tip {
  margin-bottom: 16px;
  border-radius: var(--radius-md);
}
</style>
