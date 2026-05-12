package com.tea.management.dto.response;
/**
 * SystemConfigResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record SystemConfigResponse(
        Long id,
        String configKey,
        String configValue,
        String description
) {
}
