package com.tea.management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
/**
 * RegisterRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record RegisterRequest(
        @Size(min = 4, max = 20)
        @NotBlank String username,
        @Size(min = 6, max = 20)
        @NotBlank String password,
        @Size(min = 2, max = 20)
        @NotBlank String nickname,
        @Pattern(regexp = "^1\\d{10}$")
        @NotBlank String phone,
        @Email
        String email,
        @NotNull String role,
        String staffRealName,
        String staffIdCardNo,
        List<@Size(max = 255) String> staffCertificationImages,
        @Size(max = 500)
        String staffApplicationNote
) {
}
