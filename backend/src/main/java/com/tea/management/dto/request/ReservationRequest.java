package com.tea.management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
/**
 * ReservationRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record ReservationRequest(
        @NotNull Long userId,
        @NotNull Long teaRoomId,
        @NotNull LocalDateTime reservedStartTime,
        @NotNull LocalDateTime reservedEndTime,
        @NotNull @Min(1) Integer partySize,
        @Size(max = 255)
        String remark
) {
}

