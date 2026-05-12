package com.tea.management.dto.response;

import java.time.LocalTime;
/**
 * TeaRoomResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record TeaRoomResponse(
        Long id,
        Long typeId,
        Long staffUserId,
        String name,
        Integer capacity,
        Boolean enabled,
        String location,
        String imageUrl,
        LocalTime businessStartTime,
        LocalTime businessEndTime,
        String description
) {
}
