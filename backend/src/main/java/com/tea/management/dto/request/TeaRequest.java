package com.tea.management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
/**
 * TeaRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record TeaRequest(
        @Size(max = 100)
        @NotBlank String name,
        @Size(max = 50)
        @NotBlank String category,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @PositiveOrZero
        @NotNull Integer stock,
        @Size(max = 255)
        String imageUrl,
        String description,
        @NotNull Boolean enabled
) {
}
