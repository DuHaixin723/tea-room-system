package com.tea.management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
/**
 * TeaRoomTypeRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record TeaRoomTypeRequest(
        @Size(max = 50)
        @NotBlank String name,
        @Size(max = 255)
        String description,
        @NotNull @DecimalMin("0.00") BigDecimal basePrice,
        String pricingMode,
        @NotNull @Positive Integer minCapacity,
        @NotNull @Positive Integer maxCapacity
) {
}
