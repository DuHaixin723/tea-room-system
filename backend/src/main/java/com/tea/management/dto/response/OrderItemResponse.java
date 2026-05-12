package com.tea.management.dto.response;

import java.math.BigDecimal;
/**
 * OrderItemResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record OrderItemResponse(
        Long id,
        Long orderId,
        Long teaId,
        Integer quantity,
        BigDecimal unitPrice
) {
}
