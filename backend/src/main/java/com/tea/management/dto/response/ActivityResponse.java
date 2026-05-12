package com.tea.management.dto.response;

import com.tea.management.domain.enums.ActivityStatus;
import com.tea.management.domain.enums.RoleType;

import java.time.LocalDateTime;
/**
 * ActivityResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record ActivityResponse(
        Long id,
        Long creatorUserId,
        RoleType creatorRole,
        String title,
        String content,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer capacity,
        String imageUrl,
        ActivityStatus status,
        String summaryContent,
        LocalDateTime summarySubmittedAt
) {
}
