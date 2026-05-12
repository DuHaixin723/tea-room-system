package com.tea.management.service;

import com.tea.management.domain.entity.EquipmentReport;
import com.tea.management.domain.entity.TeaRoom;
import com.tea.management.domain.enums.ReportStatus;
import com.tea.management.dto.request.EquipmentReportRequest;
import com.tea.management.dto.request.ReportStatusRequest;
import com.tea.management.dto.response.StaffNotificationResponse;
import com.tea.management.exception.ResourceNotFoundException;
import com.tea.management.repository.EquipmentReportRepository;
import com.tea.management.repository.TeaRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
/**
 * EquipmentReportService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class EquipmentReportService {

    private final EquipmentReportRepository reportRepository;
    private final TeaRoomRepository teaRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public EquipmentReport create(EquipmentReportRequest request) {
        EquipmentReport report = new EquipmentReport();
        report.setTeaRoomId(request.teaRoomId());
        report.setReportedBy(request.reportedBy());
        report.setTitle(request.title());
        report.setContent(request.content());
        report.setStatus(ReportStatus.PENDING);
        EquipmentReport saved = reportRepository.save(report);
        TeaRoom teaRoom = teaRoomRepository.findById(saved.getTeaRoomId()).orElse(null);
        if (teaRoom != null && teaRoom.getStaffUserId() != null) {
            notificationService.create(
                    teaRoom.getStaffUserId(),
                    "EQUIPMENT_REPORT_CREATED",
                    "有新的报障待处理",
                    "茶室“" + teaRoom.getName() + "”新增报障：“" + saved.getTitle() + "”。",
                    "REPORT",
                    saved.getId(),
                    "/reports"
            );
        }
        notificationService.notifyAdmins(
                "EQUIPMENT_REPORT_CREATED",
                "有新的设备报障",
                "报障“" + saved.getTitle() + "”已提交，请关注处理进度。",
                "REPORT",
                saved.getId(),
                "/reports"
        );
        return saved;
    }

    public Page<EquipmentReport> listAll(Pageable pageable) {
        return reportRepository.findAll(pageable);
    }

    public Page<EquipmentReport> listByStaff(Long staffUserId, Pageable pageable) {
        List<Long> teaRoomIds = teaRoomRepository.findByStaffUserId(staffUserId).stream().map(TeaRoom::getId).toList();
        if (teaRoomIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return reportRepository.findByTeaRoomIdIn(teaRoomIds, pageable);
    }

    public boolean isTeaRoomAssignedToStaff(Long teaRoomId, Long staffUserId) {
        return teaRoomRepository.existsByIdAndStaffUserId(teaRoomId, staffUserId);
    }

    public EquipmentReport requireReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("报障记录不存在"));
    }

    public EquipmentReport updateStatus(Long id, ReportStatusRequest request) {
        EquipmentReport report = requireReport(id);
        report.setStatus(parseStatus(request.status()));
        EquipmentReport saved = reportRepository.save(report);
        notifyAssignedStaff(saved);
        return saved;
    }

    private void notifyAssignedStaff(EquipmentReport report) {
        TeaRoom teaRoom = teaRoomRepository.findById(report.getTeaRoomId()).orElse(null);
        if (teaRoom == null || teaRoom.getStaffUserId() == null) {
            return;
        }

        String content = "茶室“" + teaRoom.getName() + "”的报障“" + report.getTitle() + "”已更新为 " + statusLabel(report.getStatus());
        StaffNotificationResponse payload = new StaffNotificationResponse(
                "EQUIPMENT_REPORT_STATUS_UPDATED",
                report.getId(),
                "报障状态已更新",
                content,
                report.getStatus().name(),
                LocalDateTime.now()
        );

        messagingTemplate.convertAndSend("/topic/staff-notice/" + teaRoom.getStaffUserId(), payload);
        notificationService.create(
                teaRoom.getStaffUserId(),
                "EQUIPMENT_REPORT_STATUS_UPDATED",
                "报障状态已更新",
                content,
                "REPORT",
                report.getId(),
                "/reports"
        );
        notificationService.create(
                report.getReportedBy(),
                "EQUIPMENT_REPORT_STATUS_UPDATED",
                "你提交的报障状态已更新",
                "报障“" + report.getTitle() + "”已更新为 " + statusLabel(report.getStatus()),
                "REPORT",
                report.getId(),
                "/reports"
        );
    }

    private String statusLabel(ReportStatus status) {
        return switch (status) {
            case PENDING -> "待处理";
            case PROCESSING -> "处理中";
            case RESOLVED -> "已解决";
            case CLOSED -> "已关闭";
        };
    }

    private ReportStatus parseStatus(String rawStatus) {
        String normalized = rawStatus == null ? "" : rawStatus.trim().toUpperCase();
        if ("COMPLETED".equals(normalized)) {
            return ReportStatus.RESOLVED;
        }
        return ReportStatus.valueOf(normalized);
    }
}
