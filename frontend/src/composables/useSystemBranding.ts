// 组合式函数：抽离可复用的页面逻辑，减少重复代码。

import { computed, ref } from 'vue'
import { systemConfigApi } from '@/api/modules/management'

const defaultSiteName = '茶室平台'
const defaultCustomerServicePhone = '400-000-0000'
const siteName = ref(defaultSiteName)
const customerServicePhone = ref(defaultCustomerServicePhone)
const loaded = ref(false)
let loadingPromise: Promise<void> | null = null

async function loadBranding(force = false) {
  if (loadingPromise && !force) return loadingPromise

  loadingPromise = (async () => {
    try {
      const data = await systemConfigApi.basicSettings()
      siteName.value = data.siteName?.trim() || defaultSiteName
      customerServicePhone.value = data.customerServicePhone?.trim() || defaultCustomerServicePhone
      loaded.value = true
      if (typeof document !== 'undefined') {
        document.title = siteName.value
      }
    } catch (error) {
      const status = (error as { response?: { status?: number } })?.response?.status
      if (status !== 401 && status !== 403) {
        throw error
      }

      loaded.value = true
      if (typeof document !== 'undefined') {
        document.title = siteName.value
      }
    } finally {
      loadingPromise = null
    }
  })()

  return loadingPromise
}

function setBranding(next: { siteName?: string; customerServicePhone?: string }) {
  siteName.value = next.siteName?.trim() || defaultSiteName
  customerServicePhone.value = next.customerServicePhone?.trim() || defaultCustomerServicePhone
  loaded.value = true
  if (typeof document !== 'undefined') {
    document.title = siteName.value
  }
}

export function useSystemBranding() {
  return {
    siteName: computed(() => siteName.value),
    customerServicePhone: computed(() => customerServicePhone.value),
    brandingLoaded: computed(() => loaded.value),
    loadBranding,
    refreshBranding: () => loadBranding(true),
    setBranding,
  }
}
