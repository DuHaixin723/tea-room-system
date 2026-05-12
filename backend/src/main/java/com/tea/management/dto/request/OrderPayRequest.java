package com.tea.management.dto.request;

import jakarta.validation.constraints.NotBlank;
/**
 * OrderPayRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record OrderPayRequest(
        @NotBlank String paymentMethod
) {
}
