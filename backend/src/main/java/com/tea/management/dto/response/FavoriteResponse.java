package com.tea.management.dto.response;
/**
 * FavoriteResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record FavoriteResponse(
        Long id,
        Long userId,
        Long targetId,
        String targetType
) {
}
