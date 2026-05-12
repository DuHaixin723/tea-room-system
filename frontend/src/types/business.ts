// 类型定义：统一描述接口数据和业务对象的结构。

export interface StatisticOverview {
  userCount: number
  staffCount: number
  adminCount: number
  teaRoomCount: number
  teaCount: number
  reservationCount: number
  activityCount: number
  orderCount: number
  totalOrderAmount: number
}

export interface UserRecord {
  id: number
  username: string
  nickname: string
  phone: string
  email?: string
  avatarUrl?: string
  staffRealName?: string
  staffIdCardNo?: string
  staffCertificationImages?: string[]
  staffApplicationNote?: string
  staffApprovalStatus?: 'PENDING' | 'APPROVED' | 'REJECTED'
  staffApprovalRemark?: string
  role: 'USER' | 'STAFF' | 'ADMIN'
  enabled: boolean
  createdAt: string
}

export interface TeaRoomTypeRecord {
  id: number
  name: string
  description?: string
  basePrice: number
  pricingMode: 'PER_ROOM' | 'PER_PERSON'
  minCapacity: number
  maxCapacity: number
}

export interface TeaRoomRecord {
  id: number
  typeId: number
  staffUserId?: number
  name: string
  capacity: number
  enabled: boolean
  location?: string
  imageUrl?: string
  businessStartTime: string
  businessEndTime: string
  description?: string
}

export interface TeaRecord {
  id: number
  name: string
  category: string
  price: number
  stock: number
  imageUrl?: string
  description?: string
  enabled: boolean
}

export interface ReservationRecord {
  id: number
  userId: number
  teaRoomId: number
  reservedStartTime: string
  reservedEndTime: string
  partySize: number
  status: string
  remark?: string
  noShowReason?: string
  noShowOrderNo?: string
}

export interface ReservationTimeSlotRecord {
  startTime: string
  endTime: string
  label: string
  remainingCapacity?: number
}

export interface ReservationAvailabilityRecord {
  teaRoomId: number
  teaRoomCapacity: number
  requestedPartySize: number
  estimatedAmount: number
  pricingMode: 'PER_ROOM' | 'PER_PERSON'
  unavailableSlots: ReservationTimeSlotRecord[]
  suggestedSlots: ReservationTimeSlotRecord[]
}

export interface ReviewRecord {
  id: number
  userId: number
  teaRoomId: number
  reservationId?: number
  rating: number
  content?: string
}

export interface ReportRecord {
  id: number
  teaRoomId: number
  reportedBy: number
  title: string
  content: string
  status: string
}

export interface ActivityRecord {
  id: number
  creatorUserId: number
  creatorRole: 'USER' | 'STAFF' | 'ADMIN'
  title: string
  content?: string
  startTime: string
  endTime: string
  capacity: number
  imageUrl?: string
  status: string
  summaryContent?: string
  summarySubmittedAt?: string
}

export interface ActivityRegistrationRecord {
  id: number
  activityId: number
  userId: number
  cancelled: boolean
}

export interface NotificationRecord {
  id: number
  recipientUserId: number
  type: string
  title: string
  content: string
  targetType?: string
  targetId?: number
  routePath?: string
  read: boolean
  createdAt: string
}

export interface OrderRecord {
  id: number
  orderNo: string
  userId: number
  reservationId?: number
  amount: number
  status: string
  paymentMethod?: 'BALANCE' | 'WECHAT' | 'ALIPAY'
  createdAt?: string
  updatedAt?: string
}

export interface OrderItemRecord {
  id: number
  orderId: number
  teaId: number
  quantity: number
  unitPrice: number
}

export interface OrderDetailRecord {
  order: OrderRecord
  items: OrderItemRecord[]
}

export interface FavoriteRecord {
  id: number
  userId: number
  targetId: number
  targetType: string
}

export interface ConsultationSessionRecord {
  id: number
  userId: number
  userUsername?: string
  userNickname?: string
  userAvatarUrl?: string
  orderId?: number
  orderNo?: string
  reservationId?: number
  teaRoomId?: number
  adminUserId?: number
  adminUsername?: string
  adminNickname?: string
  adminAvatarUrl?: string
  staffUserId?: number
  staffUsername?: string
  staffNickname?: string
  staffAvatarUrl?: string
  supportAdminUserId?: number
  supportAdminUsername?: string
  supportAdminNickname?: string
  supportAdminAvatarUrl?: string
  status: string
}

export interface ConsultationMessageRecord {
  id: number
  sessionId: number
  senderId: number
  senderUsername?: string
  senderNickname?: string
  senderAvatarUrl?: string
  mentionedAdmin?: boolean
  mentionedStaff?: boolean
  content: string
  createdAt: string
}

export interface ConsultationDetailRecord {
  session: ConsultationSessionRecord
  order?: OrderDetailRecord
  messages: ConsultationMessageRecord[]
}

export interface SystemConfigRecord {
  id: number
  configKey: string
  configValue: string
  description?: string
}

export interface BasicSystemSettingsRecord {
  siteName: string
  customerServicePhone: string
}

export interface RechargeBonusTierRecord {
  minAmount: number
  bonusAmount: number
}

export interface RechargePolicyRecord {
  enabled: boolean
  tiers: RechargeBonusTierRecord[]
}

export interface StaffComplaintRecord {
  id: number
  userId: number
  teaRoomId: number
  staffUserId: number
  content: string
  status: string
  createdAt: string
}

export interface RecommendationRecord {
  recommendedTeas: TeaRecord[]
  recommendedTeaRooms: TeaRoomRecord[]
}

export interface MemberAccountRecord {
  id: number
  userId: number
  balance: number
  cumulativeRecharge: number
  cumulativeSpend: number
  memberLevel: string
}

export interface MemberRechargeRecord {
  id: number
  userId: number
  amount: number
  balanceAfter: number
  operatorUserId?: number
  remark?: string
  createdAt: string
}

export interface UserCouponRecord {
  id: number
  userId: number
  title: string
  couponCode: string
  thresholdAmount: number
  discountAmount: number
  sourceLevel: string
  issuedWeek: string
  status: 'UNUSED' | 'USED' | 'EXPIRED'
  validFrom: string
  validUntil: string
  createdAt: string
}
