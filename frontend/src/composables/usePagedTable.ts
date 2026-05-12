// 组合式函数：抽离可复用的页面逻辑，减少重复代码。

import { reactive } from 'vue'
import type { PageResponse, QueryParams } from '@/types/api'

export interface PagedTable<T> {
  loading: boolean
  rows: T[]
  pager: {
    page: number
    size: number
    total: number
  }
  filters: Record<string, unknown>
  load: (extra?: QueryParams) => Promise<void>
  handlePageChange: (page: number) => Promise<void>
  handleSizeChange: (size: number) => Promise<void>
}

export function usePagedTable<T>(request: (params: QueryParams) => Promise<PageResponse<T>>, initialParams: QueryParams = {}) {
  const table = reactive({
    loading: false,
    rows: [] as T[],
    pager: {
      page: 0,
      size: 10,
      total: 0,
    },
    filters: { ...initialParams } as Record<string, unknown>,
    async load(extra: QueryParams = {}) {
      table.loading = true
      try {
        const data = await request({
          page: table.pager.page,
          size: table.pager.size,
          sort: 'id,desc',
          ...table.filters,
          ...extra,
        })
        const content = Array.isArray((data as unknown as { content?: unknown }).content)
          ? ((data as unknown as { content: T[] }).content ?? [])
          : []
        table.rows = content
        table.pager.page = typeof data?.page === 'number' ? data.page : table.pager.page
        table.pager.size = typeof data?.size === 'number' ? data.size : table.pager.size
        table.pager.total = typeof data?.totalElements === 'number' ? data.totalElements : table.pager.total
      } finally {
        table.loading = false
      }
    },
    async handlePageChange(page: number) {
      table.pager.page = page - 1
      await table.load()
    },
    async handleSizeChange(size: number) {
      table.pager.size = size
      table.pager.page = 0
      await table.load()
    },
  }) as unknown as PagedTable<T>
  return table
}
