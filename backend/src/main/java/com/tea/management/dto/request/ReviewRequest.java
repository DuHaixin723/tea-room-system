package com.tea.management.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
/**
 * ReviewRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record ReviewRequest(
        @NotNull Long userId,
        @NotNull Long teaRoomId,
        @NotNull Long reservationId,
        @NotNull @Min(1) @Max(5) Integer rating,
        String content
) {
}
