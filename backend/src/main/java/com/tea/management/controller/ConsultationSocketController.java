package com.tea.management.controller;

import com.tea.management.dto.request.ConsultationMessageRequest;
import com.tea.management.service.ConsultationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
/**
 * ConsultationSocketController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@Controller
@RequiredArgsConstructor
public class ConsultationSocketController {

    private final ConsultationService consultationService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/consultation.send")
    public void sendMessage(ConsultationMessageRequest request) {
        var message = consultationService.sendMessageResponse(request);
        messagingTemplate.convertAndSend("/topic/consultation/" + request.sessionId(), message);
    }
}
