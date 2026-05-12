package com.tea.management.dto.response;

import com.tea.management.domain.enums.RoleType;
/**
 * LoginResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record LoginResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        RoleType role,
        String token
) {
}
