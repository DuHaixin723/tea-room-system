package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.dto.request.TeaRoomRequest;
import com.tea.management.dto.request.TeaRoomTypeRequest;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.dto.response.TeaRoomResponse;
import com.tea.management.dto.response.TeaRoomTypeResponse;
import com.tea.management.security.SecurityUtils;
import com.tea.management.service.TeaRoomService;
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
 * TeaRoomController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/tea-rooms")
@RequiredArgsConstructor
@Validated
@Tag(name = "茶室管理")
public class TeaRoomController {

    private final TeaRoomService teaRoomService;

    @GetMapping("/types")
    @Operation(summary = "查询茶室类型")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<PageResponse<TeaRoomTypeResponse>> listTypes(@PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(teaRoomService.listTypes(pageable), ResponseMapper::toTeaRoomTypeResponse));
    }

    @PostMapping("/types")
    @Operation(summary = "新增茶室类型")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeaRoomTypeResponse> createType(@Valid @RequestBody TeaRoomTypeRequest request) {
        return ApiResponse.ok(ResponseMapper.toTeaRoomTypeResponse(teaRoomService.createType(request)));
    }

    @PutMapping("/types/{id}")
    @Operation(summary = "修改茶室类型")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeaRoomTypeResponse> updateType(@PathVariable Long id, @Valid @RequestBody TeaRoomTypeRequest request) {
        return ApiResponse.ok(ResponseMapper.toTeaRoomTypeResponse(teaRoomService.updateType(id, request)));
    }

    @GetMapping
    @Operation(summary = "查询茶室列表")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<PageResponse<TeaRoomResponse>> listRooms(@RequestParam(required = false) Long staffUserId,
                                                                @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        if (SecurityUtils.hasRole("STAFF")) {
            staffUserId = SecurityUtils.requireUserId();
        }
        if (staffUserId != null) {
            return ApiResponse.ok(PageResponse.from(teaRoomService.listRoomsByStaff(staffUserId, pageable), ResponseMapper::toTeaRoomResponse));
        }
        return ApiResponse.ok(PageResponse.from(teaRoomService.listRooms(pageable), ResponseMapper::toTeaRoomResponse));
    }

    @PostMapping
    @Operation(summary = "新增茶室")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeaRoomResponse> createRoom(@Valid @RequestBody TeaRoomRequest request) {
        return ApiResponse.ok(ResponseMapper.toTeaRoomResponse(teaRoomService.createRoom(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改茶室")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeaRoomResponse> updateRoom(@PathVariable Long id, @Valid @RequestBody TeaRoomRequest request) {
        return ApiResponse.ok(ResponseMapper.toTeaRoomResponse(teaRoomService.updateRoom(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除茶室")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteRoom(@PathVariable Long id) {
        teaRoomService.deleteRoom(id);
        return ApiResponse.ok(null);
    }
}

