package com.tea.management.controller;

import com.tea.management.common.ApiResponse;
import com.tea.management.dto.request.LoginRequest;
import com.tea.management.dto.request.RegisterRequest;
import com.tea.management.dto.response.LoginResponse;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.dto.response.UserResponse;
import com.tea.management.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
/**
 * AuthController 接口层，负责接收前端请求并把业务交给服务层处理。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "注册")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(ResponseMapper.toUserResponse(authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "登录")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
