// 配置文件：集中维护菜单、常量等前端静态配置。

import type { UserRole } from '@/types/auth'

export interface MenuItem {
  title: string
  icon: string
  path: string
  roles: UserRole[]
}

export const menuItems: MenuItem[] = [
  { title: '首页', icon: 'House', path: '/home', roles: ['USER'] },
  { title: '茶室员工作台', icon: 'Briefcase', path: '/staff', roles: ['STAFF'] },
  { title: '管理首页', icon: 'DataAnalysis', path: '/dashboard', roles: ['ADMIN'] },
  { title: '用户管理', icon: 'User', path: '/users', roles: ['ADMIN'] },
  { title: '茶室管理', icon: 'OfficeBuilding', path: '/tea-rooms', roles: ['ADMIN', 'STAFF'] },
  { title: '浏览茶室', icon: 'OfficeBuilding', path: '/tea-rooms', roles: ['USER'] },
  { title: '茶叶商城', icon: 'Tickets', path: '/teas', roles: ['ADMIN', 'USER'] },
  { title: '预约中心', icon: 'Calendar', path: '/reservations', roles: ['ADMIN', 'STAFF', 'USER'] },
  { title: '评价反馈', icon: 'ChatDotRound', path: '/reviews', roles: ['ADMIN', 'STAFF', 'USER'] },
  { title: '设备报障', icon: 'WarningFilled', path: '/reports', roles: ['ADMIN', 'STAFF'] },
  { title: '活动中心', icon: 'Flag', path: '/activities', roles: ['ADMIN', 'STAFF', 'USER'] },
  { title: '订单中心', icon: 'ShoppingCart', path: '/orders', roles: ['ADMIN', 'STAFF', 'USER'] },
  { title: '在线咨询', icon: 'Service', path: '/consultations', roles: ['ADMIN', 'STAFF', 'USER'] },
  { title: '投诉处理', icon: 'CircleClose', path: '/complaints', roles: ['ADMIN', 'STAFF', 'USER'] },
  { title: '智能推荐', icon: 'MagicStick', path: '/recommendations', roles: ['USER'] },
  { title: '我的收藏', icon: 'StarFilled', path: '/favorites', roles: ['USER'] },
  { title: '会员优惠券', icon: 'Discount', path: '/coupons', roles: ['ADMIN', 'USER'] },
  { title: '账户充值', icon: 'CreditCard', path: '/recharge', roles: ['USER'] },
  { title: '系统设置', icon: 'Setting', path: '/settings', roles: ['ADMIN'] },
  { title: '个人中心', icon: 'Avatar', path: '/profile', roles: ['ADMIN', 'STAFF', 'USER'] },
]
