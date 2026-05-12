package com.tea.management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
/**
 * RechargeRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record RechargeRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        Long couponId
) {
}
