package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.domain.entity.ConsultationSession;
import com.tea.management.domain.enums.RoleType;
import com.tea.management.dto.request.ConsultationMessageRequest;
import com.tea.management.dto.request.ConsultationSessionRequest;
import com.tea.management.dto.response.ConsultationDetailResponse;
import com.tea.management.dto.response.ConsultationMessageResponse;
import com.tea.management.dto.response.ConsultationSessionResponse;
import com.tea.management.security.SecurityUtils;
import com.tea.management.service.ConsultationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * ConsultationController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
@Validated
@Tag(name = "在线咨询")
public class ConsultationController {

    private final ConsultationService consultationService;

    @GetMapping("/sessions")
    @Operation(summary = "查询会话")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<PageResponse<ConsultationSessionResponse>> listSessions(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        if (SecurityUtils.hasRole("USER") || SecurityUtils.hasRole("STAFF")) {
            Long currentUserId = SecurityUtils.requireUserId();
            RoleType currentRole = resolveRole();
            return ApiResponse.ok(PageResponse.from(
                    consultationService.listParticipantSessions(currentRole, currentUserId, pageable),
                    consultationService::toSessionResponse
            ));
        }

        if (userId != null) {
            return ApiResponse.ok(PageResponse.from(
                    consultationService.listUserSessions(userId, pageable),
                    consultationService::toSessionResponse
            ));
        }

        return ApiResponse.ok(PageResponse.from(
                consultationService.listAllSessions(pageable),
                consultationService::toSessionResponse
        ));
    }

    @PostMapping("/sessions")
    @Operation(summary = "创建会话")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<ConsultationSessionResponse> createSession(@Valid @RequestBody ConsultationSessionRequest request) {
        RoleType currentRole = resolveRole();
        Long currentUserId = SecurityUtils.requireUserId();

        if (currentRole == RoleType.USER && !currentUserId.equals(request.userId())) {
            SecurityUtils.deny("禁止为他人创建会话");
        }
        if (currentRole == RoleType.ADMIN && !currentUserId.equals(request.userId())) {
            SecurityUtils.deny("管理员不能直接替顾客创建订单会话");
        }

        return ApiResponse.ok(consultationService.toSessionResponse(
                consultationService.createSession(request, currentRole, currentUserId)
        ));
    }

    @PatchMapping("/sessions/{id}/close")
    @Operation(summary = "关闭会话")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<ConsultationSessionResponse> closeSession(@PathVariable Long id) {
        if (!SecurityUtils.hasRole("ADMIN")) {
            assertParticipant(consultationService.requireSession(id));
        }
        return ApiResponse.ok(consultationService.toSessionResponse(consultationService.closeSession(id)));
    }

    @PostMapping("/sessions/{id}/delete")
    @Operation(summary = "删除会话及聊天记录")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        ConsultationSession session = consultationService.requireSession(id);
        if (!SecurityUtils.hasRole("ADMIN")) {
            assertParticipant(session);
        }
        consultationService.deleteSession(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "查询会话详情")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<ConsultationDetailResponse> detail(@PathVariable Long id) {
        ConsultationSession session = consultationService.requireSession(id);
        if (!SecurityUtils.hasRole("ADMIN")) {
            assertParticipant(session);
        }
        return ApiResponse.ok(consultationService.detail(id));
    }

    @PostMapping("/messages")
    @Operation(summary = "发送消息")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<ConsultationMessageResponse> sendMessage(@Valid @RequestBody ConsultationMessageRequest request) {
        Long currentUserId = SecurityUtils.requireUserId();
        if (!currentUserId.equals(request.senderId())) {
            SecurityUtils.deny("禁止冒用他人身份发送消息");
        }

        ConsultationSession session = consultationService.requireSession(request.sessionId());
        if (!SecurityUtils.hasRole("ADMIN")) {
            assertParticipant(session);
        }

        return ApiResponse.ok(consultationService.sendMessageResponse(request));
    }

    private void assertParticipant(ConsultationSession session) {
        Long currentUserId = SecurityUtils.requireUserId();
        RoleType role = resolveRole();
        if (!consultationService.isParticipant(session, currentUserId, role)) {
            SecurityUtils.deny("无权访问该会话");
        }
    }

    private RoleType resolveRole() {
        if (SecurityUtils.hasRole("ADMIN")) return RoleType.ADMIN;
        if (SecurityUtils.hasRole("STAFF")) return RoleType.STAFF;
        return RoleType.USER;
    }
}
