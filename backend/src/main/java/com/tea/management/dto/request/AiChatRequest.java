package com.tea.management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
/**
 * AiChatRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record AiChatRequest(
        @NotBlank @Size(max = 4000) String message,
        @Valid List<HistoryMessage> history
) {
    public record HistoryMessage(
            @NotBlank @Size(max = 20) String role,
            @NotBlank @Size(max = 4000) String content
    ) {
    }
}
