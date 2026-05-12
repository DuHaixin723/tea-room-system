package com.tea.management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
/**
 * ReservationAvailabilityResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record ReservationAvailabilityResponse(
        Long teaRoomId,
        Integer teaRoomCapacity,
        Integer requestedPartySize,
        BigDecimal estimatedAmount,
        String pricingMode,
        List<ReservationTimeSlotResponse> unavailableSlots,
        List<ReservationTimeSlotResponse> suggestedSlots
) {
    public record ReservationTimeSlotResponse(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String label,
            Integer remainingCapacity
    ) {
    }
}
