package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.dto.request.SystemConfigRequest;
import com.tea.management.dto.response.BasicSystemSettingsResponse;
import com.tea.management.dto.response.RechargePolicyResponse;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.dto.response.SystemConfigResponse;
import com.tea.management.service.SystemConfigService;
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
 * SystemConfigController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/system-configs")
@RequiredArgsConstructor
@Validated
@Tag(name = "系统设置")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    @Operation(summary = "查询系统配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<SystemConfigResponse>> list(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(systemConfigService.list(pageable), ResponseMapper::toSystemConfigResponse));
    }

    @GetMapping("/recharge-policy")
    @Operation(summary = "查询会员充值优惠配置")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ApiResponse<RechargePolicyResponse> rechargePolicy() {
        return ApiResponse.ok(systemConfigService.getRechargePolicy());
    }

    @GetMapping("/basic-settings")
    @Operation(summary = "查询基础业务设置")
    public ApiResponse<BasicSystemSettingsResponse> basicSettings() {
        return ApiResponse.ok(systemConfigService.getBasicSettings());
    }

    @PostMapping
    @Operation(summary = "保存系统配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SystemConfigResponse> save(@Valid @RequestBody SystemConfigRequest request) {
        return ApiResponse.ok(ResponseMapper.toSystemConfigResponse(systemConfigService.save(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除系统配置")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        systemConfigService.delete(id);
        return ApiResponse.ok(null);
    }
}
