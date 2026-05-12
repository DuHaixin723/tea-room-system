package com.tea.management.dto.response;

import com.tea.management.domain.enums.OrderStatus;
import com.tea.management.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * OrderSummaryResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record OrderSummaryResponse(
        Long id,
        String orderNo,
        Long userId,
        Long reservationId,
        BigDecimal amount,
        OrderStatus status,
        PaymentMethod paymentMethod,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
