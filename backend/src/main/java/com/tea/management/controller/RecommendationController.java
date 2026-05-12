package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.dto.response.RecommendationResponse;
import com.tea.management.security.SecurityUtils;
import com.tea.management.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * RecommendationController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Tag(name = "推荐服务")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    @Operation(summary = "获取推荐结果")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<RecommendationResponse> recommend(@RequestParam Long userId,
                                                         @RequestParam(required = false) Long reservationId) {
        Long currentUserId = SecurityUtils.requireUserId();
        if (!currentUserId.equals(userId)) {
            SecurityUtils.deny("无权查看其他用户的推荐结果");
        }
        return ApiResponse.ok(recommendationService.recommend(userId, reservationId));
    }
}
