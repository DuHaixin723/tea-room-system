// 接口模块：按业务分类封装请求方法，页面直接调用这里的函数。

import { getData } from '@/api/helpers'
import type { RecommendationRecord, StatisticOverview } from '@/types/business'

export const fetchOverview = () => getData<StatisticOverview>('/api/statistics/overview')
export const fetchRecommendations = (userId: number, reservationId?: number) =>
  getData<RecommendationRecord>('/api/recommendations', reservationId ? { userId, reservationId } : { userId })
