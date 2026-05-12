package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.dto.request.ReviewRequest;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.dto.response.ReviewResponse;
import com.tea.management.security.SecurityUtils;
import com.tea.management.service.ReviewService;
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
 * ReviewController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Validated
@Tag(name = "评价管理")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "查询评价")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','USER')")
    public ApiResponse<PageResponse<ReviewResponse>> list(@RequestParam(required = false) Long staffUserId,
                                                          @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        if (SecurityUtils.hasRole("USER")) {
            Long userId = SecurityUtils.requireUserId();
            return ApiResponse.ok(PageResponse.from(reviewService.listByUser(userId, pageable), ResponseMapper::toReviewResponse));
        }
        if (SecurityUtils.hasRole("STAFF")) {
            staffUserId = SecurityUtils.requireUserId();
        }
        if (staffUserId != null) {
            return ApiResponse.ok(PageResponse.from(reviewService.listByStaff(staffUserId, pageable), ResponseMapper::toReviewResponse));
        }
        return ApiResponse.ok(PageResponse.from(reviewService.listAll(pageable), ResponseMapper::toReviewResponse));
    }

    @PostMapping
    @Operation(summary = "提交评价")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<ReviewResponse> create(@Valid @RequestBody ReviewRequest request) {
        Long currentUserId = SecurityUtils.requireUserId();
        if (!currentUserId.equals(request.userId())) {
            SecurityUtils.deny("禁止为他人提交评价");
        }
        return ApiResponse.ok(ResponseMapper.toReviewResponse(reviewService.create(request)));
    }
}
