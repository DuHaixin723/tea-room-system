package com.tea.management.dto.response;

import com.tea.management.domain.enums.ConsultationStatus;
/**
 * ConsultationSessionResponse 响应对象，负责把后端整理后的结果返回给前端。
 */
public record ConsultationSessionResponse(
        Long id,
        Long userId,
        String userUsername,
        String userNickname,
        String userAvatarUrl,
        Long orderId,
        String orderNo,
        Long reservationId,
        Long teaRoomId,
        Long adminUserId,
        String adminUsername,
        String adminNickname,
        String adminAvatarUrl,
        Long staffUserId,
        String staffUsername,
        String staffNickname,
        String staffAvatarUrl,
        Long supportAdminUserId,
        String supportAdminUsername,
        String supportAdminNickname,
        String supportAdminAvatarUrl,
        ConsultationStatus status
) {
}
