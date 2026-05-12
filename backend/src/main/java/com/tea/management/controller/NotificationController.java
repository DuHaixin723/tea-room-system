package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.dto.response.NotificationResponse;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.security.SecurityUtils;
import com.tea.management.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * NotificationController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "通知中心")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "查询我的通知")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<PageResponse<NotificationResponse>> page(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.ok(PageResponse.from(notificationService.listByRecipient(userId, pageable), ResponseMapper::toNotificationResponse));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "查询未读通知数")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.ok(notificationService.unreadCount(SecurityUtils.requireUserId()));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "标记单条通知已读")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id, SecurityUtils.requireUserId());
        return ApiResponse.ok(null);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "标记全部通知已读")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<Integer> markAllRead() {
        return ApiResponse.ok(notificationService.markAllRead(SecurityUtils.requireUserId()));
    }
}
