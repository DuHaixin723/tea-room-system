<script setup lang="ts">
// 页面文件：负责组织当前页面的数据加载、交互行为和展示内容。

import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456',
})

async function submit() {
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-hero">
      <div class="login-hero__badge">Tea Room Operations</div>
      <h1>面向管理员、茶室员与用户的一体化企业运营前台</h1>
      <p>
        统一承载预约、订单、活动、茶叶、咨询与系统配置。界面围绕角色权限、经营数据和业务操作台设计，适合真实项目继续扩展。
      </p>
      <div class="login-hero__panel">
        <div>
          <strong>管理目标</strong>
          <span>稳定运营、实时可视、数据集中</span>
        </div>
        <div>
          <strong>系统亮点</strong>
          <span>权限清晰、模块完整、接口联调友好</span>
        </div>
      </div>
    </div>

    <div class="login-card">
      <div class="login-card__top">
        <div class="login-card__title">欢迎登录</div>
        <div class="login-card__subtitle">茶室管理系统控制台</div>
      </div>

      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-submit" :loading="loading" @click="submit">
          进入控制台
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: 1.2fr 0.9fr;
  min-height: 100vh;
  padding: 32px;
  gap: 28px;
}

.login-hero,
.login-card {
  border: 1px solid var(--line);
  border-radius: 32px;
  box-shadow: var(--shadow);
}

.login-hero {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 44px;
  background:
    radial-gradient(circle at top, rgba(215, 162, 85, 0.25), transparent 30%),
    linear-gradient(145deg, rgba(76, 51, 27, 0.96), rgba(39, 25, 13, 0.98));
  color: #f7f1e8;
}

.login-hero h1 {
  max-width: 560px;
  margin: 22px 0 14px;
  font-size: 54px;
  line-height: 1.02;
  letter-spacing: -0.06em;
}

.login-hero p {
  max-width: 560px;
  color: rgba(247, 241, 232, 0.72);
  font-size: 16px;
  line-height: 1.8;
}

.login-hero__badge {
  display: inline-flex;
  width: fit-content;
  padding: 8px 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.login-hero__panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 28px;
}

.login-hero__panel > div {
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.06);
}

.login-hero__panel strong,
.login-hero__panel span {
  display: block;
}

.login-hero__panel span {
  margin-top: 10px;
  color: rgba(247, 241, 232, 0.7);
}

.login-card {
  padding: 38px;
  background: rgba(255, 252, 245, 0.9);
  backdrop-filter: blur(16px);
}

.login-card__top {
  margin-bottom: 28px;
}

.login-card__title {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.05em;
}

.login-card__subtitle {
  margin-top: 10px;
  color: var(--muted);
}

.login-submit {
  width: 100%;
  margin-top: 12px;
}

@media (max-width: 1024px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-hero h1 {
    font-size: 40px;
  }
}
</style>
