package com.tea.management.dto.response;

import java.math.BigDecimal;
/**
 * StatisticResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record StatisticResponse(
        long userCount,
        long staffCount,
        long adminCount,
        long teaRoomCount,
        long teaCount,
        long reservationCount,
        long activityCount,
        long orderCount,
        BigDecimal totalOrderAmount
) {
}
