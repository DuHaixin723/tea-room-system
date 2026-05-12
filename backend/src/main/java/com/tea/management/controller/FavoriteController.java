package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.dto.request.FavoriteRequest;
import com.tea.management.dto.response.FavoriteResponse;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.security.SecurityUtils;
import com.tea.management.service.FavoriteService;
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
 * FavoriteController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Validated
@Tag(name = "收藏管理")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    @Operation(summary = "查询收藏")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<PageResponse<FavoriteResponse>> list(@RequestParam Long userId,
                                                            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        Long currentUserId = SecurityUtils.requireUserId();
        if (!currentUserId.equals(userId)) {
            SecurityUtils.deny("无权限查看他人收藏");
        }
        return ApiResponse.ok(PageResponse.from(favoriteService.listByUser(userId, pageable), ResponseMapper::toFavoriteResponse));
    }

    @PostMapping
    @Operation(summary = "新增收藏")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<FavoriteResponse> create(@Valid @RequestBody FavoriteRequest request) {
        Long currentUserId = SecurityUtils.requireUserId();
        if (!currentUserId.equals(request.userId())) {
            SecurityUtils.deny("禁止为他人创建收藏");
        }
        return ApiResponse.ok(ResponseMapper.toFavoriteResponse(favoriteService.create(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除收藏")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.requireUserId();
        var favorite = favoriteService.requireFavorite(id);
        if (!currentUserId.equals(favorite.getUserId())) {
            SecurityUtils.deny("无权限删除他人收藏");
        }
        favoriteService.delete(id);
        return ApiResponse.ok(null);
    }
}

