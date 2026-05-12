package com.tea.management.dto.response;

import java.math.BigDecimal;
/**
 * RechargeBonusTierResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record RechargeBonusTierResponse(
        BigDecimal minAmount,
        BigDecimal bonusAmount
) {
}
