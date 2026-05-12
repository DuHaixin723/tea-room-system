<script setup lang="ts">
// 共享组件：封装可复用的界面块与交互单元。

import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ChatDotRound, Close, Promotion } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { renderMarkdown } from '@/utils/markdown'
import { getToken } from '@/utils/storage'
import { useAuthStore } from '@/stores/auth'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
}

type DragMode = 'trigger' | 'panel'

const TRIGGER_SIZE = 64
const PANEL_WIDTH = 420
const PANEL_HEIGHT = 680
const EDGE_GAP = 20
const PANEL_GAP = 12

const authStore = useAuthStore()
const open = ref(false)
const loading = ref(false)
const input = ref('')
const bodyRef = ref<HTMLElement>()
const widgetRef = ref<HTMLElement>()
const panelRef = ref<HTMLElement>()
const position = ref({ x: 0, y: 0 })

let dragState:
  | {
      pointerId: number
      offsetX: number
      offsetY: number
      mode: DragMode
      moved: boolean
      triggerDx: number
      triggerDy: number
    }
  | null = null

const assistantTitle = computed(() => (authStore.role === 'STAFF' ? '茶室员 AI 助手' : '茶语助手'))
const assistantIntro = computed(() => {
  if (authStore.role === 'STAFF') {
    return `### 茶室员 AI 助手
你可以直接问我：
- 如何安排茶室接待与服务流程
- 遇到预约冲突时怎么协调处理
- 怎样向管理员说明现场情况
- 茶室、订单、咨询相关的操作建议`
  }

  return `### 茶语助手
你可以直接问我：
- 茶室怎么预约
- 买茶如何和预约搭配
- 订单和充值怎么查看
- 今天适合喝什么茶`
})

const inputPlaceholder = computed(() =>
  authStore.role === 'STAFF'
    ? '例如：我负责的茶室今天预约较满，怎么和管理员协调安排？'
    : '例如：我预约了茶室，适合买什么茶？',
)

const storageKey = computed(() => {
  const role = authStore.role ?? 'guest'
  const userId = authStore.user?.id ?? 'anonymous'
  return `tea-ai-widget-position:${role}:${userId}`
})

const messages = ref<ChatMessage[]>([
  {
    id: Date.now(),
    role: 'assistant',
    content: assistantIntro.value,
  },
])

const canSend = computed(() => input.value.trim().length > 0 && !loading.value)

function currentPanelWidth() {
  if (typeof window === 'undefined') return PANEL_WIDTH
  return Math.min(PANEL_WIDTH, window.innerWidth - EDGE_GAP * 2)
}

function currentPanelHeight() {
  if (typeof window === 'undefined') return PANEL_HEIGHT
  return Math.min(PANEL_HEIGHT, window.innerHeight - EDGE_GAP * 2)
}

function clampTriggerPosition(nextX: number, nextY: number) {
  if (typeof window === 'undefined') {
    return { x: nextX, y: nextY }
  }

  return {
    x: Math.min(Math.max(EDGE_GAP, nextX), Math.max(EDGE_GAP, window.innerWidth - TRIGGER_SIZE - EDGE_GAP)),
    y: Math.min(Math.max(EDGE_GAP, nextY), Math.max(EDGE_GAP, window.innerHeight - TRIGGER_SIZE - EDGE_GAP)),
  }
}

function clampPanelPosition(nextX: number, nextY: number) {
  if (typeof window === 'undefined') {
    return { x: nextX, y: nextY }
  }

  const width = currentPanelWidth()
  const height = currentPanelHeight()

  return {
    x: Math.min(Math.max(EDGE_GAP, nextX), Math.max(EDGE_GAP, window.innerWidth - width - EDGE_GAP)),
    y: Math.min(Math.max(EDGE_GAP, nextY), Math.max(EDGE_GAP, window.innerHeight - height - EDGE_GAP)),
  }
}

function savePosition() {
  localStorage.setItem(storageKey.value, JSON.stringify(position.value))
}

function applyDefaultPosition() {
  if (typeof window === 'undefined') return
  position.value = clampTriggerPosition(window.innerWidth - TRIGGER_SIZE - 28, window.innerHeight - TRIGGER_SIZE - 28)
}

function loadPosition() {
  if (typeof window === 'undefined') return

  const raw = localStorage.getItem(storageKey.value)
  if (!raw) {
    applyDefaultPosition()
    return
  }

  try {
    const parsed = JSON.parse(raw) as { x?: number; y?: number }
    if (typeof parsed?.x === 'number' && typeof parsed?.y === 'number') {
      position.value = clampTriggerPosition(parsed.x, parsed.y)
      return
    }
  } catch {
    // ignore
  }

  applyDefaultPosition()
}

function resolvePanelPlacement() {
  if (typeof window === 'undefined') {
    return { left: '0px', top: '0px', width: `${PANEL_WIDTH}px`, height: `${PANEL_HEIGHT}px` }
  }

  const width = currentPanelWidth()
  const height = currentPanelHeight()
  const desiredX = position.value.x + TRIGGER_SIZE - width
  const desiredY = position.value.y - height - PANEL_GAP
  const clamped = clampPanelPosition(desiredX, desiredY)

  return {
    left: `${clamped.x - position.value.x}px`,
    top: `${clamped.y - position.value.y}px`,
    width: `${width}px`,
    height: `${height}px`,
  }
}

const panelStyle = computed(() => resolvePanelPlacement())

function toggleOpen() {
  if (dragState?.moved) return
  open.value = !open.value
}

function clearConversation() {
  messages.value = [
    {
      id: Date.now(),
      role: 'assistant',
      content: `${assistantIntro.value}\n\n会话已清空，你可以继续提问。`,
    },
  ]
}

function scrollToBottom() {
  nextTick(() => {
    const body = bodyRef.value
    if (body) body.scrollTop = body.scrollHeight
  })
}

function parseSseBuffer(buffer: string) {
  const events: Array<{ event: string; data: string }> = []
  let remaining = buffer.replace(/\r\n/g, '\n')

  while (true) {
    const separatorIndex = remaining.indexOf('\n\n')
    if (separatorIndex === -1) break
    const block = remaining.slice(0, separatorIndex)
    remaining = remaining.slice(separatorIndex + 2)

    let event = 'message'
    const dataLines: string[] = []
    for (const line of block.split('\n')) {
      if (line.startsWith('event:')) event = line.slice(6).trim()
      if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
    }
    events.push({ event, data: dataLines.join('\n') })
  }

  return { events, remaining }
}

async function sendMessage() {
  const question = input.value.trim()
  if (!question || loading.value) return

  open.value = true
  loading.value = true
  input.value = ''

  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: question,
  })

  const assistantMessage: ChatMessage = {
    id: Date.now() + 1,
    role: 'assistant',
    content: '',
  }
  messages.value.push(assistantMessage)
  scrollToBottom()

  const history = messages.value
    .filter((item) => item.id !== assistantMessage.id)
    .slice(-12)
    .map((item) => ({ role: item.role, content: item.content }))

  try {
    const response = await fetch('/api/ai-assistant/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${getToken() ?? ''}`,
      },
      body: JSON.stringify({
        message: question,
        history,
      }),
    })

    if (!response.ok || !response.body) {
      throw new Error('AI 助手连接失败')
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const parsed = parseSseBuffer(buffer)
      buffer = parsed.remaining

      for (const item of parsed.events) {
        const payload = item.data ? JSON.parse(item.data) : {}
        if (item.event === 'chunk' && payload.content) {
          assistantMessage.content += payload.content
          messages.value = [...messages.value]
          scrollToBottom()
        }
        if (item.event === 'error') {
          throw new Error(payload.message || 'AI 助手暂时不可用')
        }
      }
    }

    if (!assistantMessage.content.trim()) {
      assistantMessage.content = '抱歉，这次没有收到有效回复，请稍后再试。'
      messages.value = [...messages.value]
    }
  } catch (error) {
    messages.value = messages.value.filter((item) => item.id !== assistantMessage.id)
    ElMessage.error(error instanceof Error ? error.message : 'AI 助手暂时不可用')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function stopDragging() {
  if (!dragState) return
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerup', handlePointerUp)
  window.removeEventListener('pointercancel', handlePointerUp)
  dragState = null
  savePosition()
}

function handlePointerMove(event: PointerEvent) {
  if (!dragState || event.pointerId !== dragState.pointerId) return

  if (dragState.mode === 'trigger') {
    const nextX = event.clientX - dragState.offsetX
    const nextY = event.clientY - dragState.offsetY
    if (Math.abs(nextX - position.value.x) > 3 || Math.abs(nextY - position.value.y) > 3) {
      dragState.moved = true
    }
    position.value = clampTriggerPosition(nextX, nextY)
    return
  }

  const desiredPanelX = event.clientX - dragState.offsetX
  const desiredPanelY = event.clientY - dragState.offsetY
  const clampedPanel = clampPanelPosition(desiredPanelX, desiredPanelY)
  const nextTriggerX = clampedPanel.x + dragState.triggerDx
  const nextTriggerY = clampedPanel.y + dragState.triggerDy

  if (Math.abs(nextTriggerX - position.value.x) > 3 || Math.abs(nextTriggerY - position.value.y) > 3) {
    dragState.moved = true
  }

  position.value = clampTriggerPosition(nextTriggerX, nextTriggerY)
}

function handlePointerUp() {
  stopDragging()
}

function startDragging(event: PointerEvent, mode: DragMode) {
  if (event.button !== 0 || typeof window === 'undefined') return

  const targetRect =
    mode === 'panel'
      ? panelRef.value?.getBoundingClientRect()
      : widgetRef.value?.getBoundingClientRect()

  if (!targetRect) return

  dragState = {
    pointerId: event.pointerId,
    offsetX: event.clientX - targetRect.left,
    offsetY: event.clientY - targetRect.top,
    mode,
    moved: false,
    triggerDx: position.value.x - targetRect.left,
    triggerDy: position.value.y - targetRect.top,
  }

  window.addEventListener('pointermove', handlePointerMove)
  window.addEventListener('pointerup', handlePointerUp)
  window.addEventListener('pointercancel', handlePointerUp)
}

function handleResize() {
  if (typeof window === 'undefined') return
  position.value = clampTriggerPosition(position.value.x, position.value.y)
  savePosition()
}

watch(open, (value) => {
  if (value) {
    scrollToBottom()
  } else {
    position.value = clampTriggerPosition(position.value.x, position.value.y)
    savePosition()
  }
})

watch(assistantIntro, (value) => {
  if (messages.value.length === 1 && messages.value[0]?.role === 'assistant') {
    messages.value = [{ ...messages.value[0], content: value }]
  }
})

watch(
  storageKey,
  () => {
    loadPosition()
  },
  { immediate: true },
)

if (typeof window !== 'undefined') {
  window.addEventListener('resize', handleResize)
}

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', handleResize)
  }
  stopDragging()
})
</script>

<template>
  <div
    ref="widgetRef"
    class="assistant-widget"
    :style="{ left: `${position.x}px`, top: `${position.y}px` }"
  >
    <transition name="assistant-panel">
      <section v-if="open" ref="panelRef" class="assistant-panel" :style="panelStyle">
        <header class="assistant-panel__header" @pointerdown="(event) => startDragging(event, 'panel')">
          <div>
            <p class="assistant-panel__eyebrow">AI Assistant</p>
            <h3>{{ assistantTitle }}</h3>
          </div>
          <div class="assistant-panel__actions">
            <el-button text @click="clearConversation">清空</el-button>
            <button class="icon-button" type="button" @click="toggleOpen">
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </header>

        <div ref="bodyRef" class="assistant-panel__body">
          <article
            v-for="item in messages"
            :key="item.id"
            class="message-card"
            :class="item.role === 'user' ? 'is-user' : 'is-assistant'"
          >
            <div class="message-card__label">{{ item.role === 'user' ? '我' : assistantTitle }}</div>
            <div
              v-if="item.role === 'assistant'"
              class="message-card__content markdown-body"
              v-html="renderMarkdown(item.content)"
            />
            <div v-else class="message-card__content user-text">{{ item.content }}</div>
          </article>
          <div v-if="loading" class="streaming-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>

        <footer class="assistant-panel__footer">
          <el-input
            v-model="input"
            type="textarea"
            resize="none"
            :rows="3"
            maxlength="1200"
            show-word-limit
            :placeholder="inputPlaceholder"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <div class="assistant-panel__submit">
            <span>支持 Markdown、代码块和流式回复</span>
            <el-button type="primary" :icon="Promotion" :disabled="!canSend" :loading="loading" @click="sendMessage">
              发送
            </el-button>
          </div>
        </footer>
      </section>
    </transition>

    <button
      class="assistant-trigger"
      type="button"
      @pointerdown="(event) => startDragging(event, 'trigger')"
      @click="toggleOpen"
    >
      <el-icon><ChatDotRound /></el-icon>
      <span>AI</span>
    </button>
  </div>
</template>

<style scoped>
.assistant-widget {
  position: fixed;
  z-index: 40;
}

.assistant-trigger {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(145deg, #264d3d, #d4a15d);
  color: #fff8ee;
  box-shadow: 0 20px 40px rgba(33, 52, 41, 0.28);
  cursor: grab;
  touch-action: none;
  user-select: none;
}

.assistant-trigger:active {
  cursor: grabbing;
}

.assistant-trigger .el-icon {
  font-size: 22px;
}

.assistant-trigger span {
  margin-top: 2px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.assistant-panel {
  position: absolute;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(167, 131, 87, 0.22);
  border-radius: 26px;
  background:
    radial-gradient(circle at top right, rgba(228, 201, 144, 0.22), transparent 28%),
    linear-gradient(180deg, rgba(255, 252, 246, 0.98), rgba(247, 241, 231, 0.98));
  box-shadow: 0 28px 60px rgba(26, 36, 30, 0.22);
  overflow: hidden;
}

.assistant-panel__header,
.assistant-panel__footer {
  padding: 16px 18px;
  background: rgba(255, 252, 246, 0.88);
}

.assistant-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
  cursor: grab;
  touch-action: none;
  user-select: none;
}

.assistant-panel__header:active {
  cursor: grabbing;
}

.assistant-panel__eyebrow {
  margin: 0;
  color: #9c7c4d;
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.assistant-panel__header h3 {
  margin: 4px 0 0;
  font-size: 20px;
}

.assistant-panel__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-button {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 12px;
  background: rgba(32, 57, 46, 0.08);
  color: #284b3c;
  cursor: pointer;
}

.assistant-panel__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 14px;
  overflow: auto;
  padding: 18px;
}

.message-card {
  max-width: 92%;
  padding: 14px 16px;
  border-radius: 20px;
}

.message-card.is-user {
  align-self: flex-end;
  border-bottom-right-radius: 8px;
  background: linear-gradient(145deg, #28503f, #1e3f32);
  color: #fff8ef;
}

.message-card.is-assistant {
  align-self: flex-start;
  border: 1px solid rgba(37, 58, 48, 0.08);
  border-bottom-left-radius: 8px;
  background: #fffdf8;
  color: #21372d;
}

.message-card__label {
  margin-bottom: 8px;
  color: #8d7553;
  font-size: 12px;
}

.message-card.is-user .message-card__label {
  color: rgba(255, 248, 239, 0.72);
}

.message-card__content {
  word-break: break-word;
  line-height: 1.7;
}

.user-text {
  white-space: pre-wrap;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 0 0 10px;
  line-height: 1.35;
}

.markdown-body :deep(p) {
  margin: 0 0 10px;
}

.markdown-body :deep(ul) {
  margin: 0 0 10px;
  padding-left: 18px;
}

.markdown-body :deep(pre) {
  margin: 10px 0;
  padding: 12px;
  border-radius: 14px;
  background: #1f2a26;
  color: #f7f2ea;
  overflow: auto;
}

.markdown-body :deep(code) {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(40, 75, 60, 0.08);
  font-family: Consolas, 'Courier New', monospace;
}

.markdown-body :deep(pre code) {
  padding: 0;
  background: transparent;
}

.markdown-body :deep(a) {
  color: #1d6a52;
}

.assistant-panel__footer {
  border-top: 1px solid rgba(15, 23, 42, 0.06);
}

.assistant-panel__submit {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

.assistant-panel__submit span {
  color: var(--muted);
  font-size: 12px;
}

.streaming-indicator {
  display: inline-flex;
  gap: 6px;
  padding: 0 4px;
}

.streaming-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #9c7c4d;
  animation: pulse 1.1s infinite ease-in-out;
}

.streaming-indicator span:nth-child(2) {
  animation-delay: 0.18s;
}

.streaming-indicator span:nth-child(3) {
  animation-delay: 0.36s;
}

.assistant-panel-enter-active,
.assistant-panel-leave-active {
  transition: 0.22s ease;
}

.assistant-panel-enter-from,
.assistant-panel-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.98);
}

@keyframes pulse {
  0%, 80%, 100% {
    transform: translateY(0);
    opacity: 0.42;
  }
  40% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .assistant-panel__submit {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
