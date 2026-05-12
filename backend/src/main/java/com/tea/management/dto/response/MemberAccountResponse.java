package com.tea.management.dto.response;

import java.math.BigDecimal;
/**
 * MemberAccountResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record MemberAccountResponse(
        Long id,
        Long userId,
        BigDecimal balance,
        BigDecimal cumulativeRecharge,
        BigDecimal cumulativeSpend,
        String memberLevel
) {
}
