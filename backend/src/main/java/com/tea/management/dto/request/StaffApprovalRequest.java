package com.tea.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
/**
 * StaffApprovalRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record StaffApprovalRequest(
        @NotBlank
        String status,
        @Size(max = 255)
        String remark
) {
}
