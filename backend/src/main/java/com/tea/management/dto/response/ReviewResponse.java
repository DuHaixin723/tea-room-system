package com.tea.management.dto.response;
/**
 * ReviewResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record ReviewResponse(
        Long id,
        Long userId,
        Long teaRoomId,
        Long reservationId,
        Integer rating,
        String content
) {
}
