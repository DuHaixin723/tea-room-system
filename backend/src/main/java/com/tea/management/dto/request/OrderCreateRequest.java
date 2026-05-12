package com.tea.management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
/**
 * OrderCreateRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record OrderCreateRequest(
        @NotNull Long userId,
        Long reservationId,
        @Valid @NotEmpty List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            @NotNull Long teaId,
            @Positive
            @NotNull Integer quantity
    ) {
    }
}
