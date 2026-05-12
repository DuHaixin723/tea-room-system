package com.tea.management.dto.response;

import java.time.LocalDateTime;
/**
 * NotificationResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record NotificationResponse(
        Long id,
        Long recipientUserId,
        String type,
        String title,
        String content,
        String targetType,
        Long targetId,
        String routePath,
        Boolean read,
        LocalDateTime createdAt
) {
}
