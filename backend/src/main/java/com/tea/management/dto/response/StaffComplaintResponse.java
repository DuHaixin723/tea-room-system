package com.tea.management.dto.response;

import com.tea.management.domain.enums.ComplaintStatus;

import java.time.LocalDateTime;
/**
 * StaffComplaintResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record StaffComplaintResponse(
        Long id,
        Long userId,
        Long teaRoomId,
        Long staffUserId,
        String content,
        ComplaintStatus status,
        LocalDateTime createdAt
) {
}

