package com.tea.management.dto.response;

import java.math.BigDecimal;
/**
 * TeaRoomTypeResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record TeaRoomTypeResponse(
        Long id,
        String name,
        String description,
        BigDecimal basePrice,
        String pricingMode,
        Integer minCapacity,
        Integer maxCapacity
) {
}
