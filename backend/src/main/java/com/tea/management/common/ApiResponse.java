package com.tea.management.common;
/**
 * ApiResponse 通用数据结构，用来统一系统中的公共返回格式。
 */
public record ApiResponse<T>(boolean success, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "ok", data);
    }
}
