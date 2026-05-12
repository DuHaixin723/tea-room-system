package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.common.PageResponse;
import com.tea.management.domain.enums.RoleType;
import com.tea.management.dto.request.ChangePasswordRequest;
import com.tea.management.dto.request.RechargeRequest;
import com.tea.management.dto.request.StaffApprovalRequest;
import com.tea.management.dto.request.UserUpdateRequest;
import com.tea.management.dto.response.MemberAccountResponse;
import com.tea.management.dto.response.MemberRechargeRecordResponse;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.dto.response.UserResponse;
import com.tea.management.security.SecurityUtils;
import com.tea.management.service.AvatarStorageService;
import com.tea.management.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
/**
 * UserController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户管理")
public class UserController {

    private final UserService userService;
    private final AvatarStorageService avatarStorageService;

    @GetMapping
    @Operation(summary = "查询用户列表")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> list(@RequestParam(required = false) String role,
                                                        @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        if (role == null || role.isBlank()) {
            return ApiResponse.ok(PageResponse.from(userService.listAll(pageable), ResponseMapper::toUserResponse));
        }
        return ApiResponse.ok(PageResponse.from(userService.listByRole(RoleType.valueOf(role.toUpperCase()), pageable), ResponseMapper::toUserResponse));
    }

    @PutMapping("/{id}")
    @Operation(summary = "管理员修改用户信息")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok(ResponseMapper.toUserResponse(userService.update(id, request)));
    }

    @PatchMapping("/{id}/staff-approval")
    @Operation(summary = "审核茶室员申请")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> reviewStaffApproval(@PathVariable Long id, @Valid @RequestBody StaffApprovalRequest request) {
        return ApiResponse.ok(ResponseMapper.toUserResponse(userService.reviewStaffApproval(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "查询个人资料")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<UserResponse> profile(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.requireUserId();
        if (!currentUserId.equals(id)) {
            SecurityUtils.deny("仅允许查看自己的个人资料");
        }
        return ApiResponse.ok(ResponseMapper.toUserResponse(userService.getUser(id)));
    }

    @GetMapping("/{id}/account")
    @Operation(summary = "查询会员账户")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ApiResponse<MemberAccountResponse> account(@PathVariable Long id) {
        if (SecurityUtils.hasRole("USER")) {
            Long currentUserId = SecurityUtils.requireUserId();
            if (!currentUserId.equals(id)) {
                SecurityUtils.deny("无权限查看他人账户");
            }
        }
        return ApiResponse.ok(ResponseMapper.toMemberAccountResponse(userService.getAccount(id)));
    }

    @GetMapping("/{id}/recharge-records")
    @Operation(summary = "查询会员充值记录")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ApiResponse<java.util.List<MemberRechargeRecordResponse>> rechargeRecords(@PathVariable Long id) {
        if (SecurityUtils.hasRole("USER")) {
            Long currentUserId = SecurityUtils.requireUserId();
            if (!currentUserId.equals(id)) {
                SecurityUtils.deny("无权限查看他人充值记录");
            }
        }
        return ApiResponse.ok(userService.getRechargeRecords(id).stream().map(ResponseMapper::toMemberRechargeRecordResponse).toList());
    }

    @PostMapping("/{id}/recharge")
    @Operation(summary = "会员充值")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ApiResponse<MemberAccountResponse> recharge(@PathVariable Long id, @Valid @RequestBody RechargeRequest request) {
        if (SecurityUtils.hasRole("USER")) {
            Long currentUserId = SecurityUtils.requireUserId();
            if (!currentUserId.equals(id)) {
                SecurityUtils.deny("无权限为他人充值");
            }
        }
        return ApiResponse.ok(ResponseMapper.toMemberAccountResponse(userService.recharge(id, request)));
    }

    @PatchMapping("/{id}/profile")
    @Operation(summary = "修改个人资料")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<UserResponse> updateProfile(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        Long currentUserId = SecurityUtils.requireUserId();
        if (!currentUserId.equals(id)) {
            SecurityUtils.deny("仅允许修改自己的个人资料");
        }
        return ApiResponse.ok(ResponseMapper.toUserResponse(userService.updateProfile(id, request)));
    }

    @PostMapping("/{id}/avatar")
    @Operation(summary = "上传头像")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<UserResponse> uploadAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        Long currentUserId = SecurityUtils.requireUserId();
        if (!currentUserId.equals(id)) {
            SecurityUtils.deny("仅允许上传自己的头像");
        }
        String avatarUrl = avatarStorageService.storeAvatar(file, userService.getUser(id).getAvatarUrl());
        return ApiResponse.ok(ResponseMapper.toUserResponse(userService.updateAvatar(id, avatarUrl)));
    }

    @PatchMapping("/{id}/password")
    @Operation(summary = "修改密码")
    @PreAuthorize("hasAnyRole('USER','STAFF','ADMIN')")
    public ApiResponse<Void> changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequest request) {
        Long currentUserId = SecurityUtils.requireUserId();
        if (!currentUserId.equals(id)) {
            SecurityUtils.deny("仅允许修改自己的密码");
        }
        userService.changePassword(id, request);
        return ApiResponse.ok(null);
    }
}
