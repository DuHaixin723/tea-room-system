// 状态仓库：负责在多个页面之间共享登录态、通知和业务状态。

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { AxiosError } from 'axios'
import { consultationApi } from '@/api/modules/management'
import { useAuthStore } from '@/stores/auth'
import { getToken } from '@/utils/storage'
import type { ConsultationMessageRecord, ConsultationSessionRecord } from '@/types/business'

function storageKey(userId: number, role: string) {
  return `tea-console-consultation-unread:${role}:${userId}`
}

export const useConsultationStore = defineStore('consultation', () => {
  const authStore = useAuthStore()
  const sessions = ref<ConsultationSessionRecord[]>([])
  const unreadSessionCounts = ref<Record<number, number>>({})
  const activeSessionId = ref<number | null>(null)
  const connected = ref(false)
  const initialized = ref(false)
  const currentKey = ref<string | null>(null)
  const client = ref<Client | null>(null)
  const subscriptions = new Map<number, () => void>()
  const available = ref(true)

  const enabled = computed(() => ['USER', 'STAFF', 'ADMIN'].includes(authStore.role ?? ''))
  const unreadSessionIds = computed(() => Object.keys(unreadSessionCounts.value).map((id) => Number(id)))
  const unreadCount = computed(() => Object.values(unreadSessionCounts.value).reduce((sum, count) => sum + count, 0))
  function isForbidden(error: unknown) {
    return (error as AxiosError | undefined)?.response?.status === 403
  }

  function senderRole(session: ConsultationSessionRecord, message: ConsultationMessageRecord) {
    if (message.senderId === session.userId) return 'USER'
    if (session.staffUserId && message.senderId === session.staffUserId) return 'STAFF'
    if (session.supportAdminUserId && message.senderId === session.supportAdminUserId) return 'ADMIN'
    return null
  }

  function shouldMarkUnread(session: ConsultationSessionRecord, message: ConsultationMessageRecord) {
    const currentUserId = authStore.user?.id
    const currentRole = authStore.role
    if (!currentUserId || !currentRole) return false
    if (message.senderId === currentUserId) return false
    if (activeSessionId.value === session.id) return false

    if (currentRole === 'ADMIN') {
      return currentUserId === session.supportAdminUserId && Boolean(message.mentionedAdmin)
    }

    if (currentRole === 'STAFF' && currentUserId === session.staffUserId) {
      const fromRole = senderRole(session, message)
      if (fromRole === 'ADMIN') {
        return Boolean(message.mentionedStaff)
      }
      return true
    }

    return true
  }

  function loadUnread() {
    if (!authStore.user?.id || !authStore.role) return
    currentKey.value = storageKey(authStore.user.id, authStore.role)
    const raw = localStorage.getItem(currentKey.value)
    const parsed = raw ? JSON.parse(raw) as unknown : null
    if (Array.isArray(parsed)) {
      unreadSessionCounts.value = Object.fromEntries(parsed.map((id) => [Number(id), 1]))
      return
    }
    unreadSessionCounts.value = parsed && typeof parsed === 'object' ? parsed as Record<number, number> : {}
  }

  function persistUnread() {
    if (!currentKey.value) return
    localStorage.setItem(currentKey.value, JSON.stringify(unreadSessionCounts.value))
  }

  function websocketUrl() {
    if (typeof window === 'undefined') return '/ws-consultation'
    const token = getToken()
    const qs = token ? `?token=${encodeURIComponent(token)}` : ''
    return `${window.location.origin}/ws-consultation${qs}`
  }

  function markUnread(sessionId: number) {
    unreadSessionCounts.value = {
      ...unreadSessionCounts.value,
      [sessionId]: (unreadSessionCounts.value[sessionId] ?? 0) + 1,
    }
    persistUnread()
  }

  function markRead(sessionId: number) {
    if (!unreadSessionCounts.value[sessionId]) return
    const next = { ...unreadSessionCounts.value }
    delete next[sessionId]
    unreadSessionCounts.value = next
    persistUnread()
  }

  function clearSubscriptions() {
    subscriptions.forEach((unsubscribe) => unsubscribe())
    subscriptions.clear()
  }

  function syncSubscriptions() {
    if (!client.value?.connected) return
    const nextIds = new Set(sessions.value.map((session) => session.id))

    subscriptions.forEach((unsubscribe, sessionId) => {
      if (!nextIds.has(sessionId)) {
        unsubscribe()
        subscriptions.delete(sessionId)
      }
    })

    for (const session of sessions.value) {
      if (subscriptions.has(session.id)) continue
      const subscription = client.value.subscribe(`/topic/consultation/${session.id}`, (frame) => {
        const message = JSON.parse(frame.body) as ConsultationMessageRecord
        if (!shouldMarkUnread(session, message)) return
        markUnread(session.id)
      })
      subscriptions.set(session.id, () => subscription.unsubscribe())
    }
  }

  async function refreshSessions() {
    if (!enabled.value || !available.value) return
    try {
      const page = await consultationApi.sessions({ page: 0, size: 100 })
      sessions.value = page.content ?? []
      syncSubscriptions()
    } catch (error) {
      if (isForbidden(error)) {
        available.value = false
        sessions.value = []
        unreadSessionCounts.value = {}
        clearSubscriptions()
        client.value?.deactivate()
        client.value = null
        return
      }
      throw error
    }
  }

  function connect() {
    if (client.value || !enabled.value || !available.value) return
    const stomp = new Client({
      webSocketFactory: () => new SockJS(websocketUrl()),
      reconnectDelay: 3000,
      onConnect: async () => {
        connected.value = true
        try {
          await refreshSessions()
        } catch {
          connected.value = false
        }
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
    if (!enabled.value || initialized.value) return
    loadUnread()
    initialized.value = true
    try {
      await refreshSessions()
    } catch (error) {
      if (isForbidden(error)) {
        available.value = false
        return
      }
      console.warn('consultation init failed', error)
    }
  }

  function setActiveSession(sessionId: number | null) {
    activeSessionId.value = sessionId
    if (sessionId != null) {
      markRead(sessionId)
    }
  }

  function reset() {
    initialized.value = false
    activeSessionId.value = null
    connected.value = false
    available.value = true
    sessions.value = []
    unreadSessionCounts.value = {}
    currentKey.value = null
    clearSubscriptions()
    client.value?.deactivate()
    client.value = null
  }

  return {
    connected,
    sessions,
    unreadCount,
    unreadSessionIds,
    available,
    init,
    ensureConnected,
    refreshSessions,
    setActiveSession,
    markRead,
    reset,
  }
})
