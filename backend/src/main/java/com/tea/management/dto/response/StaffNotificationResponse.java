package com.tea.management.dto.response;

import java.time.LocalDateTime;
/**
 * StaffNotificationResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record StaffNotificationResponse(
        String type,
        Long targetId,
        String title,
        String content,
        String status,
        LocalDateTime createdAt
) {
}
