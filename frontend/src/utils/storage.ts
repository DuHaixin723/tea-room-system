// 工具函数：封装格式化、存储和状态转换等通用能力。

const TOKEN_KEY = 'tea-console-token'
const USER_KEY = 'tea-console-user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getStoredUser<T>() {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? (JSON.parse(raw) as T) : null
}

export function setStoredUser<T>(user: T) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  removeToken()
  localStorage.removeItem(USER_KEY)
}
