package com.tea.management.dto.request;

import jakarta.validation.constraints.NotBlank;
/**
 * ReservationStatusRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record ReservationStatusRequest(
        @NotBlank String status
) {
}
