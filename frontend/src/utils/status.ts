const statusMaps = {
  reservation: {
    PENDING: '待确认',
    CONFIRMED: '已确认',
    USER_CHECKED_IN: '用户已签到',
    STAFF_CONFIRMED_CHECK_IN: '茶室员已确认',
    CHARGING: '进行中',
    NO_SHOW: '已爽约',
    CANCELLED: '已取消',
    COMPLETED: '已完成',
  },
  order: {
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    NO_SHOW: '已爽约',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REFUNDED: '已退款',
  },
  report: {
    PENDING: '待处理',
    PROCESSING: '处理中',
    RESOLVED: '已解决',
    COMPLETED: '已解决',
    CLOSED: '已关闭',
  },
  complaint: {
    PENDING: '待处理',
    PROCESSING: '处理中',
    RESOLVED: '已完成',
    COMPLETED: '已完成',
    REJECTED: '已驳回',
  },
  activity: {
    PENDING_REVIEW: '待审核',
    REJECTED: '已驳回',
    PUBLISHED: '已发布',
    DRAFT: '草稿',
    CLOSED: '已关闭',
    CANCELLED: '已取消',
  },
  consultation: {
    OPEN: '进行中',
    CLOSED: '已关闭',
  },
} as const

export type StatusKind = keyof typeof statusMaps

export function formatStatus(kind: StatusKind, value?: string | null) {
  if (!value) return '-'
  return statusMaps[kind][value as keyof (typeof statusMaps)[StatusKind]] ?? value
}
