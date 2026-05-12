package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.dto.request.TeaRequest;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.dto.response.TeaResponse;
import com.tea.management.service.TeaService;
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
 * TeaController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/teas")
@RequiredArgsConstructor
@Validated
@Tag(name = "茶叶管理")
public class TeaController {

    private final TeaService teaService;

    @GetMapping
    @Operation(summary = "查询茶叶列表")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<PageResponse<TeaResponse>> list(@RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Boolean enabled,
                                                       @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(teaService.list(keyword, enabled, pageable), ResponseMapper::toTeaResponse));
    }

    @PostMapping
    @Operation(summary = "新增茶叶")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeaResponse> create(@Valid @RequestBody TeaRequest request) {
        return ApiResponse.ok(ResponseMapper.toTeaResponse(teaService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改茶叶")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeaResponse> update(@PathVariable Long id, @Valid @RequestBody TeaRequest request) {
        return ApiResponse.ok(ResponseMapper.toTeaResponse(teaService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除茶叶")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        teaService.delete(id);
        return ApiResponse.ok(null);
    }
}
