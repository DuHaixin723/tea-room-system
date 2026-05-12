package com.tea.management.dto.response;

import com.tea.management.domain.enums.ReservationStatus;

import java.time.LocalDateTime;
/**
 * ReservationResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record ReservationResponse(
        Long id,
        Long userId,
        Long teaRoomId,
        LocalDateTime reservedStartTime,
        LocalDateTime reservedEndTime,
        Integer partySize,
        ReservationStatus status,
        String remark,
        String noShowReason,
        String noShowOrderNo
) {
}
