package com.tea.management.dto.response;
/**
 * BasicSystemSettingsResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record BasicSystemSettingsResponse(
        String siteName,
        String customerServicePhone
) {
}
