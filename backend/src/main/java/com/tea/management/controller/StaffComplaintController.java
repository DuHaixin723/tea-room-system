package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.dto.request.ComplaintStatusRequest;
import com.tea.management.dto.request.StaffComplaintRequest;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.dto.response.StaffComplaintResponse;
import com.tea.management.security.SecurityUtils;
import com.tea.management.service.StaffComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
/**
 * StaffComplaintController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
@Validated
@Tag(name = "茶室员投诉")
public class StaffComplaintController {

    private final StaffComplaintService complaintService;

    @GetMapping
    @Operation(summary = "查询投诉列表")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<PageResponse<StaffComplaintResponse>> page(@RequestParam(required = false) Long userId,
                                                                  @RequestParam(required = false) Long staffUserId,
                                                                  @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        if (SecurityUtils.hasRole("USER")) {
            userId = SecurityUtils.requireUserId();
        }
        if (SecurityUtils.hasRole("STAFF")) {
            staffUserId = SecurityUtils.requireUserId();
        }

        if (userId != null) {
            return ApiResponse.ok(PageResponse.from(complaintService.listByUserId(userId, pageable), ResponseMapper::toStaffComplaintResponse));
        }
        if (staffUserId != null) {
            return ApiResponse.ok(PageResponse.from(complaintService.listByStaffUserId(staffUserId, pageable), ResponseMapper::toStaffComplaintResponse));
        }
        return ApiResponse.ok(PageResponse.from(complaintService.listAll(pageable), ResponseMapper::toStaffComplaintResponse));
    }

    @PostMapping
    @Operation(summary = "提交投诉")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<StaffComplaintResponse> create(@Valid @RequestBody StaffComplaintRequest request) {
        Long userId = SecurityUtils.requireUserId();
        return ApiResponse.ok(ResponseMapper.toStaffComplaintResponse(complaintService.create(userId, request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "更新投诉状态")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ApiResponse<StaffComplaintResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody ComplaintStatusRequest request) {
        if (SecurityUtils.hasRole("STAFF")) {
            Long staffUserId = SecurityUtils.requireUserId();
            var complaint = complaintService.require(id);
            if (!staffUserId.equals(complaint.getStaffUserId())) {
                SecurityUtils.deny("无权限处理该投诉");
            }
        }
        return ApiResponse.ok(ResponseMapper.toStaffComplaintResponse(complaintService.updateStatus(id, request)));
    }
}
