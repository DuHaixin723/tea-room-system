package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.dto.response.StatisticResponse;
import com.tea.management.service.StatisticService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * StatisticController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "统计分析")
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping("/overview")
    @Operation(summary = "统计总览")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StatisticResponse> overview() {
        return ApiResponse.ok(statisticService.overview());
    }
}
