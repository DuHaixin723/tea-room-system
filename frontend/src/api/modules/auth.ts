// 接口模块：按业务分类封装请求方法，页面直接调用这里的函数。

import { postData } from '@/api/helpers'
import type { LoginPayload, LoginResult, RegisterPayload, RegisterResult } from '@/types/auth'

export function login(payload: LoginPayload) {
  return postData<LoginResult>('/api/auth/login', payload)
}

export function register(payload: RegisterPayload) {
  return postData<RegisterResult>('/api/auth/register', payload)
}
