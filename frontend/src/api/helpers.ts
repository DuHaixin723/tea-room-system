// 接口基础层：统一处理请求地址、请求发送和返回结果。

import type { ApiResponse, PageResponse, QueryParams } from '@/types/api'
import http from '@/api/http'

export async function getPage<T>(url: string, params?: QueryParams) {
  const { data } = await http.get<ApiResponse<PageResponse<T>>>(url, { params })
  return data.data
}

export async function getData<T>(url: string, params?: QueryParams) {
  const { data } = await http.get<ApiResponse<T>>(url, { params })
  return data.data
}

export async function postData<T>(url: string, payload?: unknown) {
  const { data } = await http.post<ApiResponse<T>>(url, payload)
  return data.data
}

export async function putData<T>(url: string, payload?: unknown) {
  const { data } = await http.put<ApiResponse<T>>(url, payload)
  return data.data
}

export async function patchData<T>(url: string, payload?: unknown) {
  const { data } = await http.patch<ApiResponse<T>>(url, payload)
  return data.data
}

export async function deleteData(url: string) {
  const { data } = await http.delete<ApiResponse<null>>(url)
  return data.data
}
