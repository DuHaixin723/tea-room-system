package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.dto.request.ActivityRegistrationRequest;
import com.tea.management.dto.request.ActivityRequest;
import com.tea.management.dto.request.ActivityStatusRequest;
import com.tea.management.dto.request.ActivitySummaryRequest;
import com.tea.management.dto.response.ActivityRegistrationResponse;
import com.tea.management.dto.response.ActivityResponse;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.security.SecurityUtils;
import com.tea.management.service.ActivityService;
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
 * ActivityController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Validated
@Tag(name = "活动管理")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    @Operation(summary = "查询活动列表")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<PageResponse<ActivityResponse>> list(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        if (SecurityUtils.hasRole("USER")) {
            return ApiResponse.ok(PageResponse.from(activityService.listPublishedActivities(pageable), ResponseMapper::toActivityResponse));
        }
        return ApiResponse.ok(PageResponse.from(activityService.listActivities(pageable), ResponseMapper::toActivityResponse));
    }

    @PostMapping
    @Operation(summary = "新增活动")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ApiResponse<ActivityResponse> create(@Valid @RequestBody ActivityRequest request) {
        Long creatorUserId = SecurityUtils.requireUserId();
        return ApiResponse.ok(ResponseMapper.toActivityResponse(
                activityService.createActivity(request, creatorUserId, SecurityUtils.currentRole(), SecurityUtils.hasRole("ADMIN"))
        ));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改活动")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ActivityResponse> update(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
        return ApiResponse.ok(ResponseMapper.toActivityResponse(activityService.updateActivity(id, request, true)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "审核活动")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ActivityResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody ActivityStatusRequest request) {
        return ApiResponse.ok(ResponseMapper.toActivityResponse(activityService.updateStatus(id, request)));
    }

    @PatchMapping("/{id}/summary")
    @Operation(summary = "提交活动总结")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ApiResponse<ActivityResponse> submitSummary(@PathVariable Long id, @Valid @RequestBody ActivitySummaryRequest request) {
        Long operatorUserId = SecurityUtils.requireUserId();
        return ApiResponse.ok(ResponseMapper.toActivityResponse(activityService.submitSummary(id, operatorUserId, SecurityUtils.hasRole("ADMIN"), request)));
    }

    @GetMapping("/registrations")
    @Operation(summary = "查询活动报名")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<PageResponse<ActivityRegistrationResponse>> listRegistrations(@RequestParam(required = false) Long userId,
                                                                                     @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        if (SecurityUtils.hasRole("USER")) {
            userId = SecurityUtils.requireUserId();
        }
        if (userId != null) {
            return ApiResponse.ok(PageResponse.from(activityService.listUserRegistrations(userId, pageable), ResponseMapper::toActivityRegistrationResponse));
        }
        return ApiResponse.ok(PageResponse.from(activityService.listRegistrations(pageable), ResponseMapper::toActivityRegistrationResponse));
    }

    @PostMapping("/registrations")
    @Operation(summary = "报名活动")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<ActivityRegistrationResponse> register(@Valid @RequestBody ActivityRegistrationRequest request) {
        Long currentUserId = SecurityUtils.requireUserId();
        if (!currentUserId.equals(request.userId())) {
            SecurityUtils.deny("禁止为他人报名活动");
        }
        return ApiResponse.ok(ResponseMapper.toActivityRegistrationResponse(activityService.register(request)));
    }

    @PatchMapping("/registrations/{id}/cancel")
    @Operation(summary = "取消活动报名")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<ActivityRegistrationResponse> cancel(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.requireUserId();
        var registration = activityService.requireRegistration(id);
        if (!currentUserId.equals(registration.getUserId())) {
            SecurityUtils.deny("无权限取消他人报名记录");
        }
        return ApiResponse.ok(ResponseMapper.toActivityRegistrationResponse(activityService.cancelRegistration(id)));
    }
}
