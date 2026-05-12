package com.tea.management.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tea.management.config.AiAssistantProperties;
import com.tea.management.dto.request.AiChatRequest;
import com.tea.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
/**
 * AiAssistantService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final AiAssistantProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public void streamChat(AiChatRequest request, Consumer<String> onChunk) {
        if (!properties.enabled() || properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new BusinessException("AI 助手尚未配置，请先设置 app.ai.api-key");
        }

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(properties.baseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofMinutes(2))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(buildPayload(request)))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new BusinessException("AI 服务调用失败: " + errorBody);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || !line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    String content = extractContent(data);
                    if (content != null && !content.isEmpty()) {
                        onChunk.accept(content);
                    }
                }
            }
        } catch (IOException e) {
            throw new BusinessException("AI 服务连接失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("AI 服务调用已中断");
        }
    }

    private String buildPayload(AiChatRequest request) throws IOException {
        List<Map<String, String>> messages = new ArrayList<>();
        String systemPrompt = properties.systemPrompt();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        if (request.history() != null) {
            for (AiChatRequest.HistoryMessage message : request.history()) {
                messages.add(Map.of(
                        "role", normalizeRole(message.role()),
                        "content", message.content()
                ));
            }
        }
        messages.add(Map.of("role", "user", "content", request.message()));

        Map<String, Object> payload = Map.of(
                "model", properties.model(),
                "stream", true,
                "messages", messages
        );
        return objectMapper.writeValueAsString(payload);
    }

    private String extractContent(String data) throws IOException {
        JsonNode root = objectMapper.readTree(data);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return null;
        }
        JsonNode firstChoice = choices.get(0);
        JsonNode deltaContent = firstChoice.path("delta").path("content");
        if (!deltaContent.isMissingNode() && !deltaContent.isNull()) {
            return deltaContent.asText();
        }
        JsonNode messageContent = firstChoice.path("message").path("content");
        if (!messageContent.isMissingNode() && !messageContent.isNull()) {
            return messageContent.asText();
        }
        return null;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String resolved = (baseUrl == null || baseUrl.isBlank()) ? "https://api.openai.com/v1" : baseUrl.trim();
        return resolved.endsWith("/") ? resolved.substring(0, resolved.length() - 1) : resolved;
    }

    private String normalizeRole(String role) {
        return switch (role.toLowerCase()) {
            case "assistant" -> "assistant";
            case "system" -> "system";
            default -> "user";
        };
    }
}
