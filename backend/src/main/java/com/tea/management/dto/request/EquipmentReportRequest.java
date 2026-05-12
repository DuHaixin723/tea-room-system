package com.tea.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
/**
 * EquipmentReportRequest 请求对象，负责承接前端提交到后端的参数。
 */
public record EquipmentReportRequest(
        @NotNull Long teaRoomId,
        @NotNull Long reportedBy,
        @Size(max = 100)
        @NotBlank String title,
        @NotBlank String content
) {
}
