package com.tea.management.dto.response;
/**
 * ActivityRegistrationResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record ActivityRegistrationResponse(
        Long id,
        Long activityId,
        Long userId,
        Boolean cancelled
) {
}
