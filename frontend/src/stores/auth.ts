// 状态仓库：负责在多个页面之间共享登录态、通知和业务状态。

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { CurrentUser, LoginPayload } from '@/types/auth'
import { getStoredUser, setStoredUser, setToken, clearSession } from '@/utils/storage'
import { login as loginRequest } from '@/api/modules/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<CurrentUser | null>(getStoredUser<CurrentUser>())

  const isAuthenticated = computed(() => Boolean(user.value?.token))
  const role = computed(() => user.value?.role ?? null)

  async function login(payload: LoginPayload) {
    const result = await loginRequest(payload)
    user.value = result
    setToken(result.token)
    setStoredUser(result)
    return result
  }

  function logout() {
    user.value = null
    clearSession()
  }

  function setUser(nextUser: CurrentUser | null) {
    user.value = nextUser
    if (nextUser) {
      setToken(nextUser.token)
      setStoredUser(nextUser)
      return
    }
    clearSession()
  }

  return {
    user,
    role,
    isAuthenticated,
    login,
    logout,
    setUser,
  }
})
