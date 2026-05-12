// 类型定义：统一描述接口数据和业务对象的结构。

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface QueryParams {
  page?: number
  size?: number
  sort?: string
  [key: string]: unknown
}
