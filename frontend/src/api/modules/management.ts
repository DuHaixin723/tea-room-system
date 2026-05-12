// 接口模块：按业务分类封装请求方法，页面直接调用这里的函数。

import { deleteData, getData, getPage, patchData, postData, putData } from '@/api/helpers'
import http from '@/api/http'
import type {
  ActivityRecord,
  ActivityRegistrationRecord,
  ConsultationDetailRecord,
  ConsultationMessageRecord,
  ConsultationSessionRecord,
  FavoriteRecord,
  MemberAccountRecord,
  MemberRechargeRecord,
  NotificationRecord,
  OrderDetailRecord,
  OrderRecord,
  RechargePolicyRecord,
  ReportRecord,
  ReservationAvailabilityRecord,
  ReservationRecord,
  ReviewRecord,
  SystemConfigRecord,
  BasicSystemSettingsRecord,
  TeaRecord,
  TeaRoomRecord,
  TeaRoomTypeRecord,
  UserRecord,
  UserCouponRecord,
  StaffComplaintRecord,
} from '@/types/business'
import type { QueryParams } from '@/types/api'

export const usersApi = {
  page: (params?: QueryParams) => getPage<UserRecord>('/api/users', params),
  update: (id: number, payload: unknown) => putData<UserRecord>(`/api/users/${id}`, payload),
  staffApproval: (id: number, payload: unknown) => patchData<UserRecord>(`/api/users/${id}/staff-approval`, payload),
  remove: (id: number) => deleteData(`/api/users/${id}`),
  getProfile: (id: number) => getData<UserRecord>(`/api/users/${id}/profile`),
  profile: (id: number, payload: unknown) => patchData<UserRecord>(`/api/users/${id}/profile`, payload),
  uploadAvatar: async (id: number, file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    const { data } = await http.post<{ success: boolean; message: string; data: UserRecord }>(`/api/users/${id}/avatar`, formData)
    return data.data
  },
  password: (id: number, payload: unknown) => patchData<void>(`/api/users/${id}/password`, payload),
  account: (id: number) => getData<MemberAccountRecord>(`/api/users/${id}/account`),
  rechargeRecords: (id: number) => getData<MemberRechargeRecord[]>(`/api/users/${id}/recharge-records`),
  recharge: (id: number, payload: unknown) => postData<MemberAccountRecord>(`/api/users/${id}/recharge`, payload),
}

export const fileApi = {
  uploadImage: async (file: File, folder = 'images') => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('folder', folder)
    const { data } = await http.post<{ success: boolean; message: string; data: { url: string } }>('/api/files/images', formData)
    return data.data.url
  },
  uploadPublicImage: async (file: File, folder = 'staff-applications') => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('folder', folder)
    const { data } = await http.post<{ success: boolean; message: string; data: { url: string } }>('/api/files/public-images', formData)
    return data.data.url
  },
}

export const couponApi = {
  page: (params?: QueryParams) => getPage<UserCouponRecord>('/api/coupons', params),
  dispatchWeekly: () => postData<number>('/api/coupons/dispatch-weekly', {}),
  dispatchByLevel: (level: string) => postData<number>('/api/coupons/dispatch-level', { level }),
}

export const teaRoomApi = {
  roomTypes: (params?: QueryParams) => getPage<TeaRoomTypeRecord>('/api/tea-rooms/types', params),
  createRoomType: (payload: unknown) => postData<TeaRoomTypeRecord>('/api/tea-rooms/types', payload),
  updateRoomType: (id: number, payload: unknown) => putData<TeaRoomTypeRecord>(`/api/tea-rooms/types/${id}`, payload),
  rooms: (params?: QueryParams) => getPage<TeaRoomRecord>('/api/tea-rooms', params),
  createRoom: (payload: unknown) => postData<TeaRoomRecord>('/api/tea-rooms', payload),
  updateRoom: (id: number, payload: unknown) => putData<TeaRoomRecord>(`/api/tea-rooms/${id}`, payload),
  removeRoom: (id: number) => deleteData(`/api/tea-rooms/${id}`),
}

export const teaApi = {
  page: (params?: QueryParams) => getPage<TeaRecord>('/api/teas', params),
  create: (payload: unknown) => postData<TeaRecord>('/api/teas', payload),
  update: (id: number, payload: unknown) => putData<TeaRecord>(`/api/teas/${id}`, payload),
  remove: (id: number) => deleteData(`/api/teas/${id}`),
}

export const reservationApi = {
  page: (params?: QueryParams) => getPage<ReservationRecord>('/api/reservations', params),
  availability: (params?: QueryParams) => getData<ReservationAvailabilityRecord>('/api/reservations/availability', params),
  create: (payload: unknown) => postData<ReservationRecord>('/api/reservations', payload),
  status: (id: number, payload: unknown) => patchData<ReservationRecord>(`/api/reservations/${id}/status`, payload),
  noShow: (id: number, payload: unknown) => patchData<ReservationRecord>(`/api/reservations/${id}/no-show`, payload),
}

export const reviewApi = {
  page: (params?: QueryParams) => getPage<ReviewRecord>('/api/reviews', params),
  create: (payload: unknown) => postData<ReviewRecord>('/api/reviews', payload),
}

export const reportApi = {
  page: (params?: QueryParams) => getPage<ReportRecord>('/api/equipment-reports', params),
  create: (payload: unknown) => postData<ReportRecord>('/api/equipment-reports', payload),
  status: (id: number, payload: unknown) => patchData<ReportRecord>(`/api/equipment-reports/${id}/status`, payload),
}

export const activityApi = {
  page: (params?: QueryParams) => getPage<ActivityRecord>('/api/activities', params),
  create: (payload: unknown) => postData<ActivityRecord>('/api/activities', payload),
  update: (id: number, payload: unknown) => putData<ActivityRecord>(`/api/activities/${id}`, payload),
  status: (id: number, payload: unknown) => patchData<ActivityRecord>(`/api/activities/${id}/status`, payload),
  summary: (id: number, payload: unknown) => patchData<ActivityRecord>(`/api/activities/${id}/summary`, payload),
  registrations: (params?: QueryParams) => getPage<ActivityRegistrationRecord>('/api/activities/registrations', params),
  register: (payload: unknown) => postData<ActivityRegistrationRecord>('/api/activities/registrations', payload),
  cancelRegistration: (id: number) => patchData<ActivityRegistrationRecord>(`/api/activities/registrations/${id}/cancel`),
}

export const notificationApi = {
  page: (params?: QueryParams) => getPage<NotificationRecord>('/api/notifications', params),
  unreadCount: () => getData<number>('/api/notifications/unread-count'),
  markRead: (id: number) => patchData<void>(`/api/notifications/${id}/read`),
  markAllRead: () => patchData<number>('/api/notifications/read-all'),
}

export const orderApi = {
  page: (params?: QueryParams) => getPage<OrderRecord>('/api/orders', params),
  create: (payload: unknown) => postData<OrderRecord>('/api/orders', payload),
  pay: (id: number, payload: unknown) => postData<OrderRecord>(`/api/orders/${id}/pay`, payload),
  detail: (id: number) => getData<OrderDetailRecord>(`/api/orders/${id}`),
  updateItems: (id: number, payload: unknown) => putData<OrderRecord>(`/api/orders/${id}/items`, payload),
  status: (id: number, payload: unknown) => patchData<OrderRecord>(`/api/orders/${id}/status`, payload),
}

export const favoriteApi = {
  page: (params?: QueryParams) => getPage<FavoriteRecord>('/api/favorites', params),
  create: (payload: unknown) => postData<FavoriteRecord>('/api/favorites', payload),
  remove: (id: number) => deleteData(`/api/favorites/${id}`),
}

export const consultationApi = {
  sessions: (params?: QueryParams) => getPage<ConsultationSessionRecord>('/api/consultations/sessions', params),
  createSession: (payload: unknown) => postData<ConsultationSessionRecord>('/api/consultations/sessions', payload),
  closeSession: (id: number) => patchData<ConsultationSessionRecord>(`/api/consultations/sessions/${id}/close`),
  deleteSession: (id: number) => postData<void>(`/api/consultations/sessions/${id}/delete`, {}),
  detail: (id: number) => getData<ConsultationDetailRecord>(`/api/consultations/sessions/${id}`),
  sendMessage: (payload: unknown) => postData<ConsultationMessageRecord>('/api/consultations/messages', payload),
}

export const systemConfigApi = {
  page: (params?: QueryParams) => getPage<SystemConfigRecord>('/api/system-configs', params),
  basicSettings: () => getData<BasicSystemSettingsRecord>('/api/system-configs/basic-settings'),
  rechargePolicy: () => getData<RechargePolicyRecord>('/api/system-configs/recharge-policy'),
  save: (payload: unknown) => postData<SystemConfigRecord>('/api/system-configs', payload),
  remove: (id: number) => deleteData(`/api/system-configs/${id}`),
}

export const complaintApi = {
  page: (params?: QueryParams) => getPage<StaffComplaintRecord>('/api/complaints', params),
  create: (payload: unknown) => postData<StaffComplaintRecord>('/api/complaints', payload),
  status: (id: number, payload: unknown) => patchData<StaffComplaintRecord>(`/api/complaints/${id}/status`, payload),
}
