package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.dto.request.EquipmentReportRequest;
import com.tea.management.dto.request.ReportStatusRequest;
import com.tea.management.dto.response.EquipmentReportResponse;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.security.SecurityUtils;
import com.tea.management.service.EquipmentReportService;
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
/**
 * EquipmentReportController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/equipment-reports")
@RequiredArgsConstructor
@Validated
@Tag(name = "设备报障")
public class EquipmentReportController {

    private final EquipmentReportService reportService;

    @GetMapping
    @Operation(summary = "查询报障")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ApiResponse<PageResponse<EquipmentReportResponse>> list(
            @RequestParam(required = false) Long staffUserId,
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        if (SecurityUtils.hasRole("STAFF")) {
            staffUserId = SecurityUtils.requireUserId();
        }
        if (staffUserId != null) {
            return ApiResponse.ok(PageResponse.from(reportService.listByStaff(staffUserId, pageable), ResponseMapper::toEquipmentReportResponse));
        }
        return ApiResponse.ok(PageResponse.from(reportService.listAll(pageable), ResponseMapper::toEquipmentReportResponse));
    }

    @PostMapping
    @Operation(summary = "提交报障")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ApiResponse<EquipmentReportResponse> create(@Valid @RequestBody EquipmentReportRequest request) {
        Long currentUserId = SecurityUtils.requireUserId();
        if (!currentUserId.equals(request.reportedBy())) {
            SecurityUtils.deny("禁止冒用他人身份提交报障");
        }
        if (SecurityUtils.hasRole("STAFF") && !reportService.isTeaRoomAssignedToStaff(request.teaRoomId(), currentUserId)) {
            SecurityUtils.deny("无权限为该茶室提交报障");
        }
        return ApiResponse.ok(ResponseMapper.toEquipmentReportResponse(reportService.create(request)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "变更报障状态")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<EquipmentReportResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ReportStatusRequest request
    ) {
        return ApiResponse.ok(ResponseMapper.toEquipmentReportResponse(reportService.updateStatus(id, request)));
    }
}
