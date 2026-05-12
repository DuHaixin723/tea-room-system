// 状态仓库：负责在多个页面之间共享登录态、通知和业务状态。

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { usersApi } from '@/api/modules/management'
import { useAuthStore } from '@/stores/auth'
import type { MemberAccountRecord } from '@/types/business'

export const useMemberStore = defineStore('member', () => {
  const account = ref<MemberAccountRecord | null>(null)
  const loading = ref(false)
  const authStore = useAuthStore()

  const balance = computed(() => account.value?.balance ?? 0)

  async function refreshAccount(forceUserId?: number) {
    const userId = forceUserId ?? authStore.user?.id
    if (authStore.role !== 'USER' || !userId) {
      account.value = null
      return null
    }

    loading.value = true
    try {
      account.value = await usersApi.account(userId)
      return account.value
    } finally {
      loading.value = false
    }
  }

  function setAccount(nextAccount: MemberAccountRecord | null) {
    account.value = nextAccount
  }

  function reset() {
    account.value = null
    loading.value = false
  }

  return {
    account,
    balance,
    loading,
    refreshAccount,
    setAccount,
    reset,
  }
})
