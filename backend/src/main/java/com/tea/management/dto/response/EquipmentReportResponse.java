package com.tea.management.dto.response;

import com.tea.management.domain.enums.ReportStatus;
/**
 * EquipmentReportResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record EquipmentReportResponse(
        Long id,
        Long teaRoomId,
        Long reportedBy,
        String title,
        String content,
        ReportStatus status
) {
}
