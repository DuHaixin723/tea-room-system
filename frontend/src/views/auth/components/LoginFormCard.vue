<script setup lang="ts">
// 共享组件：封装可复用的界面块与交互单元。

import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import type { UserRole } from '@/types/auth'

type Portal = 'admin' | 'client'

const props = defineProps<{
  portal: Portal
  submitLabel: string
  allowRoles?: UserRole[]
}>()

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref<FormInstance>()

const storageKey = computed(() => `tea-login:last-username:${props.portal}`)
const defaultUsername = localStorage.getItem(storageKey.value) ?? ''

const form = reactive({
  username: defaultUsername,
  password: '',
})

const rules: FormRules<typeof form> = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 20, message: '账号长度需在 4 到 20 个字符之间', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 个字符之间', trigger: 'blur' },
  ],
}

const rememberUsername = ref(Boolean(defaultUsername))
const defaultRedirect = computed(() => (props.portal === 'admin' ? '/dashboard' : '/home'))
const isLoginPath = (value: string) => /^\/login(?:\/|$|\?)/.test(value)

const redirectTo = computed(() => {
  const value = route.query.redirect
  if (typeof value !== 'string' || !value || isLoginPath(value)) {
    return defaultRedirect.value
  }
  return value
})

function homeOf(role: UserRole) {
  if (props.portal === 'admin') return '/dashboard'
  return role === 'STAFF' ? '/staff' : '/home'
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const result = await authStore.login({ username: form.username, password: form.password })

    if (rememberUsername.value && form.username) {
      localStorage.setItem(storageKey.value, form.username)
    } else {
      localStorage.removeItem(storageKey.value)
    }

    if (props.allowRoles?.length && !props.allowRoles.includes(result.role)) {
      authStore.logout()
      const tips =
        props.portal === 'admin'
          ? '这里是管理员入口，请使用管理员账号登录。'
          : '这里是客户端入口，请使用普通用户或茶室员账号登录。'
      ElMessage.warning(tips)
      await router.replace({ path: '/login', query: { redirect: redirectTo.value } })
      return
    }

    ElMessage.success('登录成功')
    router.push(redirectTo.value || homeOf(result.role))
  } catch {
    // Request errors are already surfaced by the HTTP interceptor.
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
    <el-form-item label="账号" prop="username">
      <el-input v-model="form.username" placeholder="请输入用户名" size="large" :prefix-icon="User" autocomplete="username" />
    </el-form-item>
    <el-form-item label="密码" prop="password">
      <el-input
        v-model="form.password"
        type="password"
        show-password
        placeholder="请输入密码"
        size="large"
        :prefix-icon="Lock"
        autocomplete="current-password"
        @keydown.enter="submit"
      />
    </el-form-item>

    <div class="login-actions">
      <el-checkbox v-model="rememberUsername">记住账号</el-checkbox>
      <slot name="actions" />
    </div>

    <el-button type="primary" size="large" class="login-submit" :loading="loading" @click="submit">
      {{ submitLabel }}
    </el-button>
  </el-form>
</template>

<style scoped>
.login-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 4px 0 16px;
  color: var(--muted);
}

.login-submit {
  width: 100%;
  height: 46px;
  border-radius: 14px;
}
</style>
