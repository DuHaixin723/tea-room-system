package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.domain.enums.OrderStatus;
import com.tea.management.dto.request.OrderCreateRequest;
import com.tea.management.dto.request.OrderItemsUpdateRequest;
import com.tea.management.dto.request.OrderPayRequest;
import com.tea.management.dto.request.OrderStatusRequest;
import com.tea.management.dto.response.OrderDetailResponse;
import com.tea.management.dto.response.OrderSummaryResponse;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.security.SecurityUtils;
import com.tea.management.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * OrderController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "璁㈠崟绠＄悊")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "鏌ヨ璁㈠崟")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<PageResponse<OrderSummaryResponse>> list(@RequestParam(required = false) Long userId,
                                                                @RequestParam(required = false) Long staffUserId,
                                                                @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        if (SecurityUtils.hasRole("USER")) {
            userId = SecurityUtils.requireUserId();
            staffUserId = null;
        } else if (SecurityUtils.hasRole("STAFF")) {
            userId = null;
            staffUserId = SecurityUtils.requireUserId();
        }

        if (userId != null) {
            return ApiResponse.ok(PageResponse.from(orderService.listByUser(userId, pageable), ResponseMapper::toOrderSummaryResponse));
        }
        if (staffUserId != null) {
            return ApiResponse.ok(PageResponse.from(orderService.listByStaff(staffUserId, pageable), ResponseMapper::toOrderSummaryResponse));
        }
        return ApiResponse.ok(PageResponse.from(orderService.listAll(pageable), ResponseMapper::toOrderSummaryResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "鏌ヨ璁㈠崟璇︽儏")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<OrderDetailResponse> detail(@PathVariable Long id) {
        var order = orderService.requireOrder(id);
        if (SecurityUtils.hasRole("USER")) {
            Long currentUserId = SecurityUtils.requireUserId();
            if (!currentUserId.equals(order.getUserId())) {
                SecurityUtils.deny("鏃犳潈鏌ョ湅璇ヨ鍗?");
            }
        } else if (SecurityUtils.hasRole("STAFF")) {
            Long staffUserId = SecurityUtils.requireUserId();
            if (!orderService.isOrderAssignedToStaff(order, staffUserId)) {
                SecurityUtils.deny("鏃犳潈鏌ョ湅璇ヨ鍗?");
            }
        }
        return ApiResponse.ok(orderService.detail(id));
    }

    @PostMapping
    @Operation(summary = "鍒涘缓璁㈠崟")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<OrderSummaryResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        Long currentUserId = SecurityUtils.requireUserId();
        if (!currentUserId.equals(request.userId())) {
            SecurityUtils.deny("绂佹涓轰粬浜哄垱寤鸿鍗?");
        }
        return ApiResponse.ok(ResponseMapper.toOrderSummaryResponse(orderService.create(request)));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "鏀粯璁㈠崟")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<OrderSummaryResponse> pay(@PathVariable Long id,
                                                 @Valid @RequestBody OrderPayRequest request) {
        Long currentUserId = SecurityUtils.requireUserId();
        return ApiResponse.ok(ResponseMapper.toOrderSummaryResponse(orderService.pay(id, currentUserId, request)));
    }

    @PutMapping("/{id}/items")
    @Operation(summary = "淇敼璁㈠崟鏄庣粏")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ApiResponse<OrderSummaryResponse> updateItems(@PathVariable Long id,
                                                         @Valid @RequestBody OrderItemsUpdateRequest request) {
        if (SecurityUtils.hasRole("STAFF")) {
            Long staffUserId = SecurityUtils.requireUserId();
            var order = orderService.requireOrder(id);
            if (!orderService.isOrderAssignedToStaff(order, staffUserId)) {
                SecurityUtils.deny("鏃犳潈淇敼璇ヨ鍗?");
            }
        }
        return ApiResponse.ok(ResponseMapper.toOrderSummaryResponse(orderService.updateItems(id, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "鍙樻洿璁㈠崟鐘舵€?")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<OrderSummaryResponse> updateStatus(@PathVariable Long id,
                                                          @Valid @RequestBody OrderStatusRequest request) {
        OrderStatus next = OrderStatus.valueOf(request.status().toUpperCase());
        if (SecurityUtils.hasRole("USER")) {
            Long currentUserId = SecurityUtils.requireUserId();
            var order = orderService.requireOrder(id);
            if (!currentUserId.equals(order.getUserId())) {
                SecurityUtils.deny("鏃犳潈鎿嶄綔璇ヨ鍗?");
            }
            if (next == OrderStatus.CANCELLED) {
                if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                    SecurityUtils.deny("浠呭緟鏀粯璁㈠崟鍙彇娑?");
                }
            } else if (next == OrderStatus.COMPLETED) {
                if (!orderService.canUserCompleteEarly(order)) {
                    SecurityUtils.deny("褰撳墠璁㈠崟鏆備笉鏀寔鎻愬墠绂诲簵");
                }
            } else {
                SecurityUtils.deny("鐢ㄦ埛浠呭厑璁稿彇娑堣鍗曟垨鎻愬墠绂诲簵");
            }
        } else if (SecurityUtils.hasRole("STAFF")) {
            Long staffUserId = SecurityUtils.requireUserId();
            var order = orderService.requireOrder(id);
            if (!orderService.isOrderAssignedToStaff(order, staffUserId)) {
                SecurityUtils.deny("鏃犳潈鎿嶄綔璇ヨ鍗?");
            }
            if (next == OrderStatus.COMPLETED) {
                SecurityUtils.deny("当前订单完成需按既有完成流程处理");
            }
        } else if (next == OrderStatus.COMPLETED) {
            SecurityUtils.deny("当前订单完成需按既有完成流程处理");
        }
        return ApiResponse.ok(ResponseMapper.toOrderSummaryResponse(orderService.updateStatus(id, request)));
    }
}

