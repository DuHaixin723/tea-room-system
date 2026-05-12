package com.tea.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
/**
 * StaffComplaintRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record StaffComplaintRequest(
        @NotNull Long teaRoomId,
        @NotBlank String content
) {
}

