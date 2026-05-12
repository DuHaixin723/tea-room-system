package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.dto.request.DispatchCouponByLevelRequest;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.dto.response.UserCouponResponse;
import com.tea.management.security.SecurityUtils;
import com.tea.management.service.UserCouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * UserCouponController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@Validated
@Tag(name = "优惠券")
public class UserCouponController {

    private final UserCouponService userCouponService;

    @GetMapping
    @Operation(summary = "查询用户优惠券")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ApiResponse<PageResponse<UserCouponResponse>> list(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 12, sort = "id") Pageable pageable
    ) {
        if (SecurityUtils.hasRole("USER")) {
            userId = SecurityUtils.requireUserId();
        }
        if (SecurityUtils.hasRole("ADMIN") && userId == null) {
            return ApiResponse.ok(PageResponse.from(userCouponService.listAll(pageable), ResponseMapper::toUserCouponResponse));
        }
        if (userId == null) {
            userId = SecurityUtils.requireUserId();
        }
        return ApiResponse.ok(PageResponse.from(userCouponService.listByUser(userId, pageable), ResponseMapper::toUserCouponResponse));
    }

    @PostMapping("/dispatch-weekly")
    @Operation(summary = "手动补发本周会员优惠券")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> dispatchWeekly() {
        return ApiResponse.ok(userCouponService.dispatchWeeklyCoupons());
    }

    @PostMapping("/dispatch-level")
    @Operation(summary = "按会员等级发放优惠券")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> dispatchByLevel(@RequestBody @Validated DispatchCouponByLevelRequest request) {
        return ApiResponse.ok(userCouponService.dispatchCouponsByLevel(request.level()));
    }
}
