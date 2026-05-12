package com.tea.management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
/**
 * UserUpdateRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record UserUpdateRequest(
        @Size(min = 2, max = 20)
        String nickname,
        @Pattern(regexp = "^1\\d{10}$")
        String phone,
        @Email
        String email,
        @Size(max = 255)
        String avatarUrl,
        Boolean enabled
) {
}
