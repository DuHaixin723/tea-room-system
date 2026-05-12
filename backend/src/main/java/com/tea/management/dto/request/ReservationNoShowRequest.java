package com.tea.management.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReservationNoShowRequest(
        @NotBlank String orderNo,
        @NotBlank String reason
) {
}
