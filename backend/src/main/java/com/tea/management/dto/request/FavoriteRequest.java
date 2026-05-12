package com.tea.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
/**
 * FavoriteRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record FavoriteRequest(
        @NotNull Long userId,
        @NotNull Long targetId,
        @NotBlank String targetType
) {
}
