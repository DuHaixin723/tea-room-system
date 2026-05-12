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

function goUserLogin() {
  router.push({ path: '/login/user', query: targetQuery.value })
}

function goRegister() {
  router.push({ path: '/register/user', query: targetQuery.value })
}
</script>

<template>
  <LoginShell
    title="茶室员登录"
    subtitle="处理预约确认、到店接待、活动执行与服务反馈等工作内容。"
    heading="面向茶室员的服务工作台"
    description="登录后可查看负责的预约、处理现场服务事务，并配合活动与咨询流程。"
    :highlights="[
      { title: '预约处理', description: '确认预约、跟进入店状态并衔接后续服务。' },
      { title: '现场运营', description: '处理活动安排、用户反馈和门店服务事项。' },
    ]"
    variant="client"
  >
    <LoginFormCard portal="client" submit-label="进入茶室员端" :allow-roles="['STAFF']">
      <template #actions>
        <div class="entry-actions">
          <button type="button" class="entry-cta entry-cta--primary" @click="goUserLogin">
            <span class="entry-cta__label">切换到用户入口</span>
            <span class="entry-cta__hint">返回普通用户登录与消费入口</span>
          </button>
          <button type="button" class="entry-cta entry-cta--ghost" @click="goRegister">
            <span class="entry-cta__label">去注册并提交审核资料</span>
            <span class="entry-cta__hint">可切换为茶室员申请并上传多张审核图片</span>
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
  border: 1px solid rgba(45, 107, 83, 0.18);
  background:
    linear-gradient(180deg, rgba(239, 248, 244, 0.98), rgba(227, 241, 234, 0.94)),
    linear-gradient(135deg, rgba(45, 107, 83, 0.08), transparent);
  color: #255844;
  box-shadow: 0 12px 28px rgba(45, 107, 83, 0.1);
}

.entry-cta--primary:hover {
  border-color: rgba(45, 107, 83, 0.3);
  box-shadow: 0 16px 32px rgba(45, 107, 83, 0.16);
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
  color: rgba(37, 88, 68, 0.76);
}

.entry-cta--ghost .entry-cta__hint {
  color: rgba(79, 64, 49, 0.72);
}
</style>
