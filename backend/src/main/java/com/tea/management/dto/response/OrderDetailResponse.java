package com.tea.management.dto.response;

import java.util.List;
/**
 * OrderDetailResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record OrderDetailResponse(
        OrderSummaryResponse order,
        List<OrderItemResponse> items
) {
}
