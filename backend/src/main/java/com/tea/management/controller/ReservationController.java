package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.domain.enums.ReservationStatus;
import com.tea.management.dto.request.ReservationNoShowRequest;
import com.tea.management.dto.request.ReservationRequest;
import com.tea.management.dto.request.ReservationStatusRequest;
import com.tea.management.dto.response.ReservationAvailabilityResponse;
import com.tea.management.dto.response.ReservationResponse;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.security.SecurityUtils;
import com.tea.management.service.OrderService;
import com.tea.management.service.ReservationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * ReservationController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Validated
@Tag(name = "预约管理")
public class ReservationController {

    private final ReservationService reservationService;
    private final OrderService orderService;

    /**
     * 按当前登录角色返回可查看的预约记录。
     * 普通用户只能看自己的预约，茶室员只能看自己负责茶室的预约，管理员可以看全部。
     */
    @GetMapping
    @Operation(summary = "查询预约")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<PageResponse<ReservationResponse>> list(@RequestParam(required = false) Long userId,
                                                               @RequestParam(required = false) Long staffUserId,
                                                               @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        if (SecurityUtils.hasRole("USER")) {
            userId = SecurityUtils.requireUserId();
            staffUserId = null;
        } else if (SecurityUtils.hasRole("STAFF")) {
            staffUserId = SecurityUtils.requireUserId();
            userId = null;
        }

        if (userId != null) {
            return ApiResponse.ok(PageResponse.from(reservationService.listByUser(userId, pageable), ResponseMapper::toReservationResponse));
        }
        if (staffUserId != null) {
            return ApiResponse.ok(PageResponse.from(reservationService.listByStaff(staffUserId, pageable), ResponseMapper::toReservationResponse));
        }
        return ApiResponse.ok(PageResponse.from(reservationService.listAll(pageable), ResponseMapper::toReservationResponse));
    }

    /**
     * 在用户提交预约前，先计算不可预约时段和可推荐时段。
     */
    @GetMapping("/availability")
    @Operation(summary = "查询茶室可预约时段")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<ReservationAvailabilityResponse> availability(@RequestParam Long teaRoomId,
                                                                     @RequestParam(required = false) LocalDateTime start,
                                                                     @RequestParam(required = false) LocalDateTime end,
                                                                     @RequestParam(required = false) Integer durationMinutes,
                                                                     @RequestParam(required = false) Integer partySize) {
        return ApiResponse.ok(reservationService.availability(teaRoomId, start, end, durationMinutes, partySize));
    }

    /**
     * 确认请求中的用户就是当前登录用户后，再创建预约记录。
     */
    @PostMapping
    @Operation(summary = "创建预约")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<ReservationResponse> create(@Valid @RequestBody ReservationRequest request) {
        Long currentUserId = SecurityUtils.requireUserId();
        if (!currentUserId.equals(request.userId())) {
            SecurityUtils.deny("禁止为他人创建预约");
        }
        return ApiResponse.ok(ResponseMapper.toReservationResponse(reservationService.create(request)));
    }

    /**
     * 在权限和支付条件通过后，推动预约进入下一个业务状态。
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "变更预约状态")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<ReservationResponse> updateStatus(@PathVariable Long id,
                                                         @Valid @RequestBody ReservationStatusRequest request) {
        ReservationStatus next = ReservationStatus.valueOf(request.status().toUpperCase());
        var currentReservation = reservationService.requireReservation(id);

        if (next == ReservationStatus.COMPLETED) {
            SecurityUtils.deny("预约完成由系统在结束时间后自动处理");
        }
        if (requiresReservationPayment(next) && !orderService.hasPaidReservationOrder(currentReservation.getId())) {
            if (currentReservation.getStatus() != ReservationStatus.PENDING) {
                orderService.createReservationOrder(currentReservation);
            }
            SecurityUtils.deny("茶室预约费用订单未支付，不能签到、确认到店或开始服务");
        }

        if (SecurityUtils.hasRole("USER")) {
            Long currentUserId = SecurityUtils.requireUserId();
            if (!currentUserId.equals(currentReservation.getUserId())) {
                SecurityUtils.deny("无权操作该预约");
            }
            if (next != ReservationStatus.CANCELLED && next != ReservationStatus.USER_CHECKED_IN) {
                SecurityUtils.deny("用户仅允许取消预约或到店打卡");
            }
            if (next == ReservationStatus.USER_CHECKED_IN) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(currentReservation.getReservedStartTime()) || now.isAfter(currentReservation.getReservedEndTime())) {
                    SecurityUtils.deny("仅可在预约时间范围内到店签到");
                }
            }
        }

        if (SecurityUtils.hasRole("STAFF")) {
            Long staffUserId = SecurityUtils.requireUserId();
            if (!reservationService.isTeaRoomAssignedToStaff(currentReservation.getTeaRoomId(), staffUserId)) {
                SecurityUtils.deny("无权操作该茶室的预约");
            }
            if (next != ReservationStatus.CONFIRMED && next != ReservationStatus.STAFF_CONFIRMED_CHECK_IN) {
                SecurityUtils.deny("茶室员仅允许确认预约或确认到店");
            }
        }

        var saved = reservationService.updateStatus(id, request);
        if (currentReservation.getStatus() == ReservationStatus.PENDING && next == ReservationStatus.CONFIRMED) {
            orderService.createReservationOrder(saved);
        }
        return ApiResponse.ok(ResponseMapper.toReservationResponse(saved));
    }

    @PatchMapping("/{id}/no-show")
    @Operation(summary = "登记用户爽约")
    @PreAuthorize("hasRole('STAFF')")
    public ApiResponse<ReservationResponse> registerNoShow(@PathVariable Long id,
                                                           @Valid @RequestBody ReservationNoShowRequest request) {
        var currentReservation = reservationService.requireReservation(id);
        Long staffUserId = SecurityUtils.requireUserId();
        if (!reservationService.isTeaRoomAssignedToStaff(currentReservation.getTeaRoomId(), staffUserId)) {
            SecurityUtils.deny("无权操作该茶室的预约");
        }
        return ApiResponse.ok(ResponseMapper.toReservationResponse(reservationService.registerNoShow(id, request)));
    }

    private boolean requiresReservationPayment(ReservationStatus next) {
        return next == ReservationStatus.USER_CHECKED_IN
                || next == ReservationStatus.STAFF_CONFIRMED_CHECK_IN
                || next == ReservationStatus.CHARGING;
    }
}
