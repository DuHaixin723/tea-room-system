// 状态仓库：负责在多个页面之间共享登录态、通知和业务状态。

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { notificationApi } from '@/api/modules/management'
import { useAuthStore } from '@/stores/auth'
import { getToken } from '@/utils/storage'
import type { NotificationRecord } from '@/types/business'

export const useNotificationStore = defineStore('notification', () => {
  const authStore = useAuthStore()
  const items = ref<NotificationRecord[]>([])
  const unreadCount = ref(0)
  const initialized = ref(false)
  const connected = ref(false)
  const loading = ref(false)
  const client = ref<Client | null>(null)
  const pollTimer = ref<number | null>(null)

  const available = computed(() => Boolean(authStore.isAuthenticated && authStore.user?.id))

  function websocketUrl() {
    if (typeof window === 'undefined') return '/ws-consultation'
    const token = getToken()
    const qs = token ? `?token=${encodeURIComponent(token)}` : ''
    return `${window.location.origin}/ws-consultation${qs}`
  }

  async function load() {
    if (!available.value) return
    loading.value = true
    try {
      const [page, count] = await Promise.all([
        notificationApi.page({ page: 0, size: 20 }),
        notificationApi.unreadCount(),
      ])
      items.value = page.content ?? []
      unreadCount.value = count ?? 0
    } finally {
      loading.value = false
    }
  }

  async function markRead(item: NotificationRecord) {
    if (item.read) return
    await notificationApi.markRead(item.id)
    item.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }

  async function markAllRead() {
    await notificationApi.markAllRead()
    items.value = items.value.map((item) => ({ ...item, read: true }))
    unreadCount.value = 0
  }

  function pushItem(item: NotificationRecord) {
    items.value = [item, ...items.value.filter((current) => current.id !== item.id)].slice(0, 20)
    if (!item.read) {
      unreadCount.value += 1
    }
  }

  function startPolling() {
    stopPolling()
    if (typeof window === 'undefined') return
    pollTimer.value = window.setInterval(() => {
      load().catch(() => undefined)
    }, 30000)
  }

  function stopPolling() {
    if (pollTimer.value != null && typeof window !== 'undefined') {
      window.clearInterval(pollTimer.value)
    }
    pollTimer.value = null
  }

  function connect() {
    if (!available.value || client.value || !authStore.user?.id) return

    const stomp = new Client({
      webSocketFactory: () => new SockJS(websocketUrl()),
      reconnectDelay: 3000,
      onConnect: () => {
        connected.value = true
        stomp.subscribe(`/topic/notifications/${authStore.user?.id}`, (frame) => {
          const payload = JSON.parse(frame.body) as NotificationRecord
          pushItem(payload)
        })
      },
      onStompError: () => {
        connected.value = false
      },
      onWebSocketClose: () => {
        connected.value = false
      },
    })

    stomp.activate()
    client.value = stomp
  }

  function ensureConnected() {
    connect()
  }

  async function init() {
    if (!available.value || initialized.value) return
    initialized.value = true
    await load()
    startPolling()
  }

  function reset() {
    initialized.value = false
    connected.value = false
    items.value = []
    unreadCount.value = 0
    stopPolling()
    client.value?.deactivate()
    client.value = null
  }

  return {
    items,
    unreadCount,
    loading,
    connected,
    init,
    ensureConnected,
    load,
    markRead,
    markAllRead,
    reset,
  }
})
