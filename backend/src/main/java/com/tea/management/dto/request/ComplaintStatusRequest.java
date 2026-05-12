package com.tea.management.dto.request;

import jakarta.validation.constraints.NotBlank;
/**
 * ComplaintStatusRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record ComplaintStatusRequest(
        @NotBlank String status
) {
}

