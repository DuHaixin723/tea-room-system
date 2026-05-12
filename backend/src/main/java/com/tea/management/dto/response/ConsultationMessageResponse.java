package com.tea.management.dto.response;

import java.time.LocalDateTime;
/**
 * ConsultationMessageResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record ConsultationMessageResponse(
        Long id,
        Long sessionId,
        Long senderId,
        String senderUsername,
        String senderNickname,
        String senderAvatarUrl,
        Boolean mentionedAdmin,
        Boolean mentionedStaff,
        String content,
        LocalDateTime createdAt
) {
}
