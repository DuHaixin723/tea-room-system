package com.tea.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
/**
 * ConsultationMessageRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record ConsultationMessageRequest(
        @NotNull Long sessionId,
        @NotNull Long senderId,
        @NotBlank String content,
        Boolean mentionAdmin,
        Boolean mentionStaff
) {
}
