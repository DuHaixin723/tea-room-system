// 类型定义：统一描述接口数据和业务对象的结构。

export type UserRole = 'USER' | 'STAFF' | 'ADMIN'
export type StaffApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname: string
  phone: string
  email?: string
  role: 'USER' | 'STAFF'
  staffRealName?: string
  staffIdCardNo?: string
  staffCertificationImages?: string[]
  staffApplicationNote?: string
}

export interface LoginResult {
  id: number
  username: string
  nickname: string
  avatarUrl?: string
  role: UserRole
  token: string
}

export interface RegisterResult {
  id: number
  username: string
  nickname: string
  phone: string
  email?: string
  avatarUrl?: string
  role: UserRole
  enabled: boolean
  staffRealName?: string
  staffIdCardNo?: string
  staffCertificationImages?: string[]
  staffApplicationNote?: string
  staffApprovalStatus?: StaffApprovalStatus
  staffApprovalRemark?: string
  createdAt: string
}

export interface CurrentUser {
  id: number
  username: string
  nickname: string
  avatarUrl?: string
  role: UserRole
  token: string
}
