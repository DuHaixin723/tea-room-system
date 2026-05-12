package com.tea.management.dto.request;

import jakarta.validation.constraints.NotNull;
/**
 * ConsultationSessionRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record ConsultationSessionRequest(
        @NotNull Long userId,
        @NotNull Long orderId,
        Long adminUserId
) {
}
