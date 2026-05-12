package com.tea.management.dto.response;

import com.tea.management.domain.entity.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ResponseMapper {

    private ResponseMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getStaffRealName(),
                user.getStaffIdCardNo(),
                certificationImagesOf(user),
                user.getStaffApplicationNote(),
                user.getStaffApprovalStatus(),
                user.getStaffApprovalRemark(),
                user.getRole(),
                user.getEnabled(),
                user.getCreatedAt()
        );
    }

    public static MemberAccountResponse toMemberAccountResponse(MemberAccount account) {
        return new MemberAccountResponse(
                account.getId(),
                account.getUserId(),
                account.getBalance(),
                account.getCumulativeRecharge(),
                account.getCumulativeSpend(),
                account.getMemberLevel()
        );
    }

    public static MemberRechargeRecordResponse toMemberRechargeRecordResponse(MemberRechargeRecord record) {
        return new MemberRechargeRecordResponse(
                record.getId(),
                record.getUserId(),
                record.getAmount(),
                record.getBalanceAfter(),
                record.getOperatorUserId(),
                record.getRemark(),
                record.getCreatedAt()
        );
    }

    public static TeaRoomTypeResponse toTeaRoomTypeResponse(TeaRoomType type) {
        return new TeaRoomTypeResponse(
                type.getId(),
                type.getName(),
                type.getDescription(),
                type.getBasePrice(),
                type.getPricingMode().name(),
                type.getMinCapacity(),
                type.getMaxCapacity()
        );
    }

    public static TeaRoomResponse toTeaRoomResponse(TeaRoom room) {
        return new TeaRoomResponse(
                room.getId(),
                room.getTypeId(),
                room.getStaffUserId(),
                room.getName(),
                room.getCapacity(),
                room.getEnabled(),
                room.getLocation(),
                room.getImageUrl(),
                room.getBusinessStartTime(),
                room.getBusinessEndTime(),
                room.getDescription()
        );
    }

    public static TeaResponse toTeaResponse(Tea tea) {
        return new TeaResponse(tea.getId(), tea.getName(), tea.getCategory(), tea.getPrice(), tea.getStock(), tea.getImageUrl(), tea.getDescription(), tea.getEnabled());
    }

    public static ReservationResponse toReservationResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getTeaRoomId(),
                reservation.getReservedStartTime(),
                reservation.getReservedEndTime(),
                reservation.getPartySize(),
                reservation.getStatus(),
                reservation.getRemark(),
                reservation.getNoShowReason(),
                reservation.getNoShowOrderNo()
        );
    }

    public static ReviewResponse toReviewResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getUserId(),
                review.getTeaRoomId(),
                review.getReservationId(),
                review.getRating(),
                review.getContent()
        );
    }

    public static EquipmentReportResponse toEquipmentReportResponse(EquipmentReport report) {
        return new EquipmentReportResponse(report.getId(), report.getTeaRoomId(), report.getReportedBy(), report.getTitle(), report.getContent(), report.getStatus());
    }

    public static ActivityResponse toActivityResponse(Activity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getCreatorUserId(),
                activity.getCreatorRole(),
                activity.getTitle(),
                activity.getContent(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getCapacity(),
                activity.getImageUrl(),
                activity.getStatus(),
                activity.getSummaryContent(),
                activity.getSummarySubmittedAt()
        );
    }

    public static ActivityRegistrationResponse toActivityRegistrationResponse(ActivityRegistration registration) {
        return new ActivityRegistrationResponse(registration.getId(), registration.getActivityId(), registration.getUserId(), registration.getCancelled());
    }

    public static OrderSummaryResponse toOrderSummaryResponse(ProductOrder order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                order.getReservationId(),
                order.getAmount(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public static OrderItemResponse toOrderItemResponse(OrderItem item) {
        return new OrderItemResponse(item.getId(), item.getOrderId(), item.getTeaId(), item.getQuantity(), item.getUnitPrice());
    }

    public static FavoriteResponse toFavoriteResponse(Favorite favorite) {
        return new FavoriteResponse(favorite.getId(), favorite.getUserId(), favorite.getTargetId(), favorite.getTargetType());
    }

    public static UserCouponResponse toUserCouponResponse(UserCoupon coupon) {
        return new UserCouponResponse(
                coupon.getId(),
                coupon.getUserId(),
                coupon.getTitle(),
                coupon.getCouponCode(),
                coupon.getThresholdAmount(),
                coupon.getDiscountAmount(),
                coupon.getSourceLevel(),
                coupon.getIssuedWeek(),
                coupon.getStatus(),
                coupon.getValidFrom(),
                coupon.getValidUntil(),
                coupon.getCreatedAt()
        );
    }

    public static ConsultationSessionResponse toConsultationSessionResponse(ConsultationSession session) {
        return new ConsultationSessionResponse(
                session.getId(),
                session.getUserId(),
                null,
                null,
                null,
                session.getOrderId(),
                null,
                null,
                session.getTeaRoomId(),
                session.getAdminUserId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                session.getStatus()
        );
    }

    public static ConsultationMessageResponse toConsultationMessageResponse(ConsultationMessage message) {
        return new ConsultationMessageResponse(message.getId(), message.getSessionId(), message.getSenderId(), null, null, null, null, null, message.getContent(), message.getCreatedAt());
    }

    public static StaffComplaintResponse toStaffComplaintResponse(StaffComplaint complaint) {
        return new StaffComplaintResponse(
                complaint.getId(),
                complaint.getUserId(),
                complaint.getTeaRoomId(),
                complaint.getStaffUserId(),
                complaint.getContent(),
                complaint.getStatus(),
                complaint.getCreatedAt()
        );
    }

    public static NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipientUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getRoutePath(),
                notification.getRead(),
                notification.getCreatedAt()
        );
    }

    public static SystemConfigResponse toSystemConfigResponse(SystemConfig config) {
        return new SystemConfigResponse(config.getId(), config.getConfigKey(), config.getConfigValue(), config.getDescription());
    }

    public static OrderDetailResponse toOrderDetailResponse(ProductOrder order, List<OrderItem> items) {
        return new OrderDetailResponse(
                toOrderSummaryResponse(order),
                items.stream().map(ResponseMapper::toOrderItemResponse).toList()
        );
    }

    public static ConsultationDetailResponse toConsultationDetailResponse(ConsultationSession session, List<ConsultationMessage> messages) {
        return new ConsultationDetailResponse(
                toConsultationSessionResponse(session),
                messages.stream().map(ResponseMapper::toConsultationMessageResponse).toList()
        );
    }

    private static List<String> certificationImagesOf(User user) {
        if (user.getStaffCertificationImages() != null && !user.getStaffCertificationImages().isBlank()) {
            return Arrays.stream(user.getStaffCertificationImages().split("\\R"))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
        }
        if (user.getStaffCertificationUrl() != null && !user.getStaffCertificationUrl().isBlank()) {
            return List.of(user.getStaffCertificationUrl().trim());
        }
        return Collections.emptyList();
    }
}
