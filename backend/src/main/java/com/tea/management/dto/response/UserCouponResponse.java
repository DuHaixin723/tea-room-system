package com.tea.management.dto.response;

import com.tea.management.domain.enums.CouponStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * UserCouponResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record UserCouponResponse(
        Long id,
        Long userId,
        String title,
        String couponCode,
        BigDecimal thresholdAmount,
        BigDecimal discountAmount,
        String sourceLevel,
        String issuedWeek,
        CouponStatus status,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        LocalDateTime createdAt
) {
}
