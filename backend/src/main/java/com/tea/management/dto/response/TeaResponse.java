package com.tea.management.dto.response;

import java.math.BigDecimal;
/**
 * TeaResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record TeaResponse(
        Long id,
        String name,
        String category,
        BigDecimal price,
        Integer stock,
        String imageUrl,
        String description,
        Boolean enabled
) {
}
