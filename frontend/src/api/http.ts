// 接口基础层：统一处理请求地址、请求发送和返回结果。

import axios from 'axios'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/api'
import { clearSession, getToken } from '@/utils/storage'

NProgress.configure({ showSpinner: false })

let suppressErrorsUntil = 0

function isAuthRoute(path: string) {
  return /^\/login(?:\/|$|\?)|^\/register(?:\/|$|\?)/.test(path)
}

function resolveLoginRedirectTarget() {
  const current = `${location.pathname}${location.search}${location.hash}`
  if (isAuthRoute(location.pathname) || isAuthRoute(current)) {
    const params = new URLSearchParams(location.search)
    const redirect = params.get('redirect')
    return redirect && !isAuthRoute(redirect) ? `/login?redirect=${encodeURIComponent(redirect)}` : '/login'
  }
  return `/login?redirect=${encodeURIComponent(current)}`
}

export function suppressHttpErrorsFor(durationMs = 1500) {
  suppressErrorsUntil = Date.now() + durationMs
}

function shouldSuppressHttpError(status?: number, code?: string) {
  if (Date.now() > suppressErrorsUntil) return false
  return code === 'ERR_CANCELED' || status !== undefined
}

function sanitizeMessage(raw?: string) {
  const message = raw?.trim()
  if (!message) return '请求失败'

  const lower = message.toLowerCase()
  if (
    lower.includes('sql') ||
    lower.includes('mysql') ||
    lower.includes('jdbc') ||
    lower.includes('constraint') ||
    lower.includes('syntax error') ||
    lower.includes('communications link failure') ||
    lower.includes('table') ||
    lower.includes('column')
  ) {
    return '系统处理失败，请稍后重试'
  }

  return message
}

const http = axios.create({
  baseURL: '/',
  timeout: 20000,
})

http.interceptors.request.use((config) => {
  NProgress.start()
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    NProgress.done()
    const payload = response.data as ApiResponse<unknown>
    if (payload && payload.success === false) {
      const message = sanitizeMessage(payload.message)
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return response
  },
  (error) => {
    NProgress.done()
    const status = error?.response?.status as number | undefined
    const code = error?.code as string | undefined
    if (status === 401) {
      clearSession()
      location.href = resolveLoginRedirectTarget()
      return Promise.reject(error)
    }
    if (shouldSuppressHttpError(status, code)) {
      return Promise.reject(error)
    }

    const message = sanitizeMessage(error?.response?.data?.message || error?.message || '请求异常')
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default http
