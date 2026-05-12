package com.tea.management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * MemberRechargeRecordResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record MemberRechargeRecordResponse(
        Long id,
        Long userId,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Long operatorUserId,
        String remark,
        LocalDateTime createdAt
) {
}
