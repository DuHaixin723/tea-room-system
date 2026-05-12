package com.tea.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
/**
 * TeaRoomRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record TeaRoomRequest(
        @NotNull Long typeId,
        Long staffUserId,
        @Size(max = 100)
        @NotBlank String name,
        @Positive
        @NotNull Integer capacity,
        @NotNull Boolean enabled,
        @Size(max = 255)
        String location,
        @Size(max = 255)
        String imageUrl,
        @NotNull LocalTime businessStartTime,
        @NotNull LocalTime businessEndTime,
        String description
) {
}
