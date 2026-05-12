package com.tea.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * AiAssistantProperties 配置类，用来声明系统运行时需要的框架配置。
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiAssistantProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        String systemPrompt
) {
}
