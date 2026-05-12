package com.tea.management.service;

import com.tea.management.domain.entity.StaffComplaint;
import com.tea.management.domain.enums.ComplaintStatus;
import com.tea.management.domain.enums.ReservationStatus;
import com.tea.management.dto.request.ComplaintStatusRequest;
import com.tea.management.dto.request.StaffComplaintRequest;
import com.tea.management.exception.BusinessException;
import com.tea.management.exception.ResourceNotFoundException;
import com.tea.management.repository.ReservationRepository;
import com.tea.management.repository.StaffComplaintRepository;
import com.tea.management.repository.TeaRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
/**
 * StaffComplaintService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class StaffComplaintService {

    private final StaffComplaintRepository complaintRepository;
    private final ReservationRepository reservationRepository;
    private final TeaRoomRepository teaRoomRepository;
    private final NotificationService notificationService;

    public StaffComplaint create(Long userId, StaffComplaintRequest request) {
        boolean reserved = reservationRepository.existsByUserIdAndTeaRoomIdAndStatusNot(userId, request.teaRoomId(), ReservationStatus.CANCELLED);
        if (!reserved) {
            throw new BusinessException("仅预约过该茶室的用户才可投诉对应茶室员");
        }

        var room = teaRoomRepository.findById(request.teaRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("茶室不存在"));
        if (room.getStaffUserId() == null) {
            throw new BusinessException("该茶室暂未绑定茶室员，无法投诉");
        }

        StaffComplaint complaint = new StaffComplaint();
        complaint.setUserId(userId);
        complaint.setTeaRoomId(request.teaRoomId());
        complaint.setStaffUserId(room.getStaffUserId());
        complaint.setContent(request.content());
        complaint.setStatus(ComplaintStatus.PENDING);
        StaffComplaint saved = complaintRepository.save(complaint);

        notificationService.notifyAdmins(
                "COMPLAINT_CREATED",
                "有新的投诉待处理",
                "茶室投诉已提交，请尽快跟进处理。",
                "COMPLAINT",
                saved.getId(),
                "/complaints"
        );
        notificationService.create(
                room.getStaffUserId(),
                "COMPLAINT_CREATED",
                "收到新的投诉",
                "你收到一条新的服务投诉，请等待管理员处理。",
                "COMPLAINT",
                saved.getId(),
                "/complaints"
        );
        return saved;
    }

    public Page<StaffComplaint> listAll(Pageable pageable) {
        return complaintRepository.findAll(pageable);
    }

    public Page<StaffComplaint> listByUserId(Long userId, Pageable pageable) {
        return complaintRepository.findByUserId(userId, pageable);
    }

    public Page<StaffComplaint> listByStaffUserId(Long staffUserId, Pageable pageable) {
        return complaintRepository.findByStaffUserId(staffUserId, pageable);
    }

    public StaffComplaint require(Long id) {
        return complaintRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("投诉记录不存在"));
    }

    public StaffComplaint updateStatus(Long id, ComplaintStatusRequest request) {
        StaffComplaint complaint = require(id);
        complaint.setStatus(parseStatus(request.status()));
        StaffComplaint saved = complaintRepository.save(complaint);
        notificationService.create(
                saved.getUserId(),
                "COMPLAINT_STATUS_UPDATED",
                "投诉状态已更新",
                "你的投诉状态已更新为 " + saved.getStatus().name() + "。",
                "COMPLAINT",
                saved.getId(),
                "/complaints"
        );
        notificationService.create(
                saved.getStaffUserId(),
                "COMPLAINT_STATUS_UPDATED",
                "投诉状态已更新",
                "涉及你的投诉状态已更新为 " + saved.getStatus().name() + "。",
                "COMPLAINT",
                saved.getId(),
                "/complaints"
        );
        return saved;
    }

    private ComplaintStatus parseStatus(String rawStatus) {
        String normalized = rawStatus == null ? "" : rawStatus.trim().toUpperCase();
        if ("COMPLETED".equals(normalized)) {
            return ComplaintStatus.RESOLVED;
        }
        return ComplaintStatus.valueOf(normalized);
    }
}
