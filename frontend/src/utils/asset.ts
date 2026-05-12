// 工具函数：封装格式化、存储和状态转换等通用能力。

export function resolveAssetUrl(url?: string | null) {
  const value = url?.trim()
  if (!value) return ''
  if (/^https?:\/\//i.test(value) || value.startsWith('data:')) return value
  if (value.startsWith('/')) return value
  return `/${value}`
}
