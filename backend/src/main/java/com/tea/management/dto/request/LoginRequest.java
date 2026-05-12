package com.tea.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * LoginRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record LoginRequest(
        @Size(min = 4, max = 20)
        @NotBlank String username,
        @Size(min = 6, max = 20)
        @NotBlank String password
) {
}
