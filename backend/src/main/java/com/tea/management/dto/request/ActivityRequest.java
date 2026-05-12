package com.tea.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
/**
 * ActivityRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record ActivityRequest(
        @Size(max = 100)
        @NotBlank String title,
        String content,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @Positive
        @NotNull Integer capacity,
        @Size(max = 255)
        String imageUrl,
        @NotBlank String status
) {
}
