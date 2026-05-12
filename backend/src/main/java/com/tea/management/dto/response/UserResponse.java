package com.tea.management.dto.response;

import com.tea.management.domain.enums.RoleType;
import com.tea.management.domain.enums.StaffApprovalStatus;

import java.time.LocalDateTime;
import java.util.List;
/**
 * UserResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record UserResponse(
        Long id,
        String username,
        String nickname,
        String phone,
        String email,
        String avatarUrl,
        String staffRealName,
        String staffIdCardNo,
        List<String> staffCertificationImages,
        String staffApplicationNote,
        StaffApprovalStatus staffApprovalStatus,
        String staffApprovalRemark,
        RoleType role,
        Boolean enabled,
        LocalDateTime createdAt
) {
}
