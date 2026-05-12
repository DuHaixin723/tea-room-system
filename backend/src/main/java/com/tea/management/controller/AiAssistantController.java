package com.tea.management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tea.management.dto.request.AiChatRequest;
import com.tea.management.exception.BusinessException;
import com.tea.management.service.AiAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
/**
 * AiAssistantController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/ai-assistant")
@RequiredArgsConstructor
@Tag(name = "AI 助手")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 助手流式对话")
    @PreAuthorize("hasAnyRole('USER','STAFF')")
    public SseEmitter streamChat(@Valid @RequestBody AiChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        CompletableFuture.runAsync(() -> {
            try {
                aiAssistantService.streamChat(request, chunk -> send(emitter, "chunk", Map.of("content", chunk)));
                send(emitter, "done", Map.of("done", true));
                emitter.complete();
            } catch (BusinessException ex) {
                send(emitter, "error", Map.of("message", ex.getMessage()));
                emitter.complete();
            } catch (Exception ex) {
                send(emitter, "error", Map.of("message", "AI 助手暂时不可用"));
                emitter.completeWithError(ex);
            }
        });

        emitter.onTimeout(emitter::complete);
        emitter.onCompletion(() -> {
        });
        return emitter;
    }

    private void send(SseEmitter emitter, String eventName, Map<String, Object> payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(objectMapper.writeValueAsString(payload)));
        } catch (IOException e) {
            throw new BusinessException("AI 消息推送失败");
        }
    }
}
