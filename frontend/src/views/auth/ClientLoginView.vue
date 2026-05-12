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
const ADMIN_PORT = '5714'

function switchToAdmin() {
  if (typeof window !== 'undefined') {
    const url = `${window.location.protocol}//${window.location.hostname}:${ADMIN_PORT}/login`
    window.location.href = redirect.value ? `${url}?redirect=${encodeURIComponent(redirect.value)}` : url
    return
  }
  router.push({ path: '/login/admin', query: switchQuery.value })
}
</script>

<template>
  <LoginShell
    title="欢迎回来"
    subtitle="客户端支持普通用户与茶室员登录。"
    heading="服务入口已准备好，登录即可继续"
    description="登录后可完成茶室预约、茶叶购买、活动参与、订单追踪与咨询沟通，茶室员也可在此处理服务流程。"
    :highlights="[
      { title: '预约顺畅', description: '查看预约状态、打卡到店、跟进服务进度。' },
      { title: '服务在线', description: '购茶、咨询、推荐和个人资料统一管理。' },
    ]"
    variant="client"
  >
    <LoginFormCard portal="client" submit-label="进入客户端" :allow-roles="['USER', 'STAFF']">
      <template #actions>
        <el-link type="primary" :underline="false" @click="switchToAdmin">前往管理员登录</el-link>
      </template>
    </LoginFormCard>
  </LoginShell>
</template>
