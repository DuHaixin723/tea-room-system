package com.tea.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * SecurityProperties 配置类，用来声明系统运行时需要的框架配置。
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String jwtSecret,
        Long jwtExpireHours
) {
}
