package com.tea.management.service;

import com.tea.management.domain.entity.Reservation;
import com.tea.management.domain.entity.ProductOrder;
import com.tea.management.domain.entity.OrderItem;
import com.tea.management.domain.entity.Tea;
import com.tea.management.domain.entity.TeaRoom;
import com.tea.management.domain.entity.TeaRoomType;
import com.tea.management.domain.enums.OrderStatus;
import com.tea.management.domain.enums.ReservationStatus;
import com.tea.management.domain.enums.TeaRoomPricingMode;
import com.tea.management.dto.request.ReservationNoShowRequest;
import com.tea.management.dto.request.ReservationRequest;
import com.tea.management.dto.request.ReservationStatusRequest;
import com.tea.management.dto.response.ReservationAvailabilityResponse;
import com.tea.management.exception.BusinessException;
import com.tea.management.exception.ResourceNotFoundException;
import com.tea.management.repository.ReservationRepository;
import com.tea.management.repository.ProductOrderRepository;
import com.tea.management.repository.OrderItemRepository;
import com.tea.management.repository.TeaRepository;
import com.tea.management.repository.TeaRoomRepository;
import com.tea.management.repository.TeaRoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
/**
 * ReservationService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ProductOrderRepository productOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TeaRepository teaRepository;
    private final TeaRoomRepository teaRoomRepository;
    private final TeaRoomTypeRepository teaRoomTypeRepository;
    private final NotificationService notificationService;

    /**
     * 校验预约时间、茶室规则、营业时间和时段冲突后，创建新的预约记录。
     */
    public Reservation create(ReservationRequest request) {
        if (!request.reservedEndTime().isAfter(request.reservedStartTime())) {
            throw new IllegalArgumentException("预约结束时间必须晚于开始时间");
        }

        TeaRoom teaRoom = requireTeaRoom(request.teaRoomId());
        TeaRoomType teaRoomType = requireTeaRoomType(teaRoom.getTypeId());
        validatePartySize(teaRoom, teaRoomType, request.partySize());
        validateBusinessHours(teaRoom, request.reservedStartTime(), request.reservedEndTime());
        validateTimeConflict(teaRoom, teaRoomType, request.reservedStartTime(), request.reservedEndTime(), request.partySize());

        Reservation reservation = new Reservation();
        reservation.setUserId(request.userId());
        reservation.setTeaRoomId(request.teaRoomId());
        reservation.setReservedStartTime(request.reservedStartTime());
        reservation.setReservedEndTime(request.reservedEndTime());
        reservation.setPartySize(request.partySize());
        reservation.setRemark(request.remark());
        reservation.setStatus(ReservationStatus.PENDING);
        Reservation saved = reservationRepository.save(reservation);
        notifyReservationCreated(saved, teaRoom);
        return saved;
    }

    public Page<Reservation> listAll(Pageable pageable) {
        return reservationRepository.findAll(pageable);
    }

    public Page<Reservation> listByUser(Long userId, Pageable pageable) {
        return reservationRepository.findByUserId(userId, pageable);
    }

    public Page<Reservation> listByStaff(Long staffUserId, Pageable pageable) {
        List<Long> teaRoomIds = teaRoomRepository.findByStaffUserId(staffUserId).stream().map(TeaRoom::getId).toList();
        if (teaRoomIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return reservationRepository.findByTeaRoomIdIn(teaRoomIds, pageable);
    }

    public boolean isTeaRoomAssignedToStaff(Long teaRoomId, Long staffUserId) {
        return teaRoomRepository.existsByIdAndStaffUserId(teaRoomId, staffUserId);
    }

    public Reservation requireReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("预约记录不存在"));
    }

    /**
     * 生成预约页需要的可用性数据，里面包含冲突时段和推荐时段。
     */
    public ReservationAvailabilityResponse availability(
            Long teaRoomId,
            LocalDateTime start,
            LocalDateTime end,
            Integer durationMinutes,
            Integer partySize
    ) {
        TeaRoom teaRoom = requireTeaRoom(teaRoomId);
        TeaRoomType teaRoomType = requireTeaRoomType(teaRoom.getTypeId());
        int requestedPartySize = partySize == null || partySize < 1 ? 1 : partySize;
        validatePartySize(teaRoom, teaRoomType, requestedPartySize);

        LocalDateTime rangeStart = start != null ? start : LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime rangeEnd = end != null ? end : rangeStart.plusDays(7);
        int slotMinutes = durationMinutes == null || durationMinutes < 30 ? 120 : durationMinutes;

        List<Reservation> conflicts = findConflictingReservations(teaRoomId, rangeStart, rangeEnd);
        List<ReservationAvailabilityResponse.ReservationTimeSlotResponse> unavailableSlots = conflicts.stream()
                .map(item -> new ReservationAvailabilityResponse.ReservationTimeSlotResponse(
                        item.getReservedStartTime(),
                        item.getReservedEndTime(),
                        teaRoomType.getPricingMode() == TeaRoomPricingMode.PER_ROOM
                                ? statusLabel(item.getStatus()) + " | 该包厢已整间占用"
                                : statusLabel(item.getStatus()) + " | 已占 " + item.getPartySize() + " 人",
                        teaRoomType.getPricingMode() == TeaRoomPricingMode.PER_ROOM
                                ? 0
                                : Math.max(teaRoom.getCapacity() - item.getPartySize(), 0)
                ))
                .toList();

        List<ReservationAvailabilityResponse.ReservationTimeSlotResponse> suggestedSlots =
                buildSuggestedSlots(teaRoom, teaRoomType, rangeStart, rangeEnd, slotMinutes, requestedPartySize, conflicts);

        return new ReservationAvailabilityResponse(
                teaRoomId,
                teaRoom.getCapacity(),
                requestedPartySize,
                estimateAmount(teaRoomType, requestedPartySize, slotMinutes),
                teaRoomType.getPricingMode().name(),
                unavailableSlots,
                suggestedSlots
        );
    }

    /**
     * 在符合业务流转规则的前提下更新预约状态。
     */
    public Reservation updateStatus(Long id, ReservationStatusRequest request) {
        Reservation reservation = requireReservation(id);
        ReservationStatus current = reservation.getStatus();
        ReservationStatus next = ReservationStatus.valueOf(request.status().toUpperCase());

        if (!isTransitionAllowed(current, next)) {
            throw new BusinessException("预约状态流转不合法");
        }
        if (next == ReservationStatus.COMPLETED && reservation.getReservedEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("预约结束时间未到，暂不能确认完成");
        }

        reservation.setStatus(next);
        Reservation saved = reservationRepository.save(reservation);
        notifyReservationStatusChanged(saved);
        return saved;
    }

    @Transactional
    public Reservation registerNoShow(Long id, ReservationNoShowRequest request) {
        Reservation reservation = requireReservation(id);
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException("仅已确认且未打卡的预约可以登记爽约");
        }
        if (LocalDateTime.now().isBefore(reservation.getReservedStartTime().plusMinutes(30))) {
            throw new BusinessException("预约开始时间 30 分钟后才能登记爽约");
        }

        String orderNo = request.orderNo().trim();
        ProductOrder targetOrder = productOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        if (!reservation.getId().equals(targetOrder.getReservationId())) {
            throw new BusinessException("订单号与当前预约不匹配");
        }
        if (!reservation.getUserId().equals(targetOrder.getUserId())) {
            throw new BusinessException("订单用户与当前预约用户不匹配");
        }

        List<ProductOrder> relatedOrders = productOrderRepository.findByReservationId(reservation.getId());
        for (ProductOrder order : relatedOrders) {
            if (order.getStatus() == OrderStatus.REFUNDED || order.getStatus() == OrderStatus.COMPLETED) {
                throw new BusinessException("存在已退款或已完成订单，不能登记爽约");
            }
        }

        for (ProductOrder order : relatedOrders) {
            restoreTeaStock(order);
            order.setStatus(OrderStatus.NO_SHOW);
        }
        productOrderRepository.saveAll(relatedOrders);

        reservation.setStatus(ReservationStatus.NO_SHOW);
        reservation.setNoShowOrderNo(orderNo);
        reservation.setNoShowReason(request.reason().trim());
        Reservation saved = reservationRepository.save(reservation);
        notifyReservationNoShow(saved, targetOrder);
        return saved;
    }

    /**
     * 把已过期的预约自动改成已完成，避免旧预约长期停留在进行中状态。
     */
    @Transactional
    public int completeExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> reservations = reservationRepository.findByStatusInAndReservedEndTimeBefore(
                List.of(
                        ReservationStatus.PENDING,
                        ReservationStatus.CONFIRMED,
                        ReservationStatus.USER_CHECKED_IN,
                        ReservationStatus.STAFF_CONFIRMED_CHECK_IN,
                        ReservationStatus.CHARGING
                ),
                now
        );
        reservations.forEach(item -> item.setStatus(ReservationStatus.COMPLETED));
        reservationRepository.saveAll(reservations);
        reservations.forEach(this::notifyReservationAutoCompleted);
        return reservations.size();
    }
    private TeaRoom requireTeaRoom(Long teaRoomId) {
        return teaRoomRepository.findById(teaRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("茶室不存在"));
    }

    private TeaRoomType requireTeaRoomType(Long teaRoomTypeId) {
        return teaRoomTypeRepository.findById(teaRoomTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("茶室类型不存在"));
    }

    /**
     * 根据计费方式、预约人数和预约时长估算预约金额。
     */
    public BigDecimal calculateReservationAmount(TeaRoomType teaRoomType,
                                                 int partySize,
                                                 LocalDateTime reservedStartTime,
                                                 LocalDateTime reservedEndTime) {
        long durationMinutes = ChronoUnit.MINUTES.between(reservedStartTime, reservedEndTime);
        return estimateAmount(teaRoomType, partySize, durationMinutes);
    }

    private BigDecimal estimateAmount(TeaRoomType teaRoomType, int partySize, long durationMinutes) {
        BigDecimal durationHours = BigDecimal.valueOf(Math.max(durationMinutes, 0))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal baseAmount = teaRoomType.getBasePrice().multiply(durationHours);
        if (teaRoomType.getPricingMode() == TeaRoomPricingMode.PER_PERSON) {
            return baseAmount.multiply(BigDecimal.valueOf(partySize));
        }
        return baseAmount;
    }

    private void validatePartySize(TeaRoom teaRoom, TeaRoomType teaRoomType, Integer partySize) {
        if (partySize == null || partySize < 1) {
            throw new BusinessException("预约人数必须大于 0");
        }
        if (!Boolean.TRUE.equals(teaRoom.getEnabled())) {
            throw new BusinessException("该茶室当前不可预约");
        }
        if (teaRoomType.getPricingMode() == TeaRoomPricingMode.PER_PERSON && partySize > teaRoom.getCapacity()) {
            throw new BusinessException("预约人数超过茶室容量");
        }
    }

    private void validateBusinessHours(TeaRoom teaRoom, LocalDateTime start, LocalDateTime end) {
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new BusinessException("预约必须在同一天内完成，不能跨天");
        }

        LocalTime businessStart = teaRoom.getBusinessStartTime();
        LocalTime businessEnd = teaRoom.getBusinessEndTime();
        if (businessStart == null || businessEnd == null) {
            throw new BusinessException("茶室营业时间未配置");
        }

        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();
        if (startTime.isBefore(businessStart) || endTime.isAfter(businessEnd)) {
            throw new BusinessException("预约时间必须在茶室营业时间内");
        }
    }

    /**
     * 检查当前选择的时间段是否与同一茶室已有预约发生冲突。
     */
    private void validateTimeConflict(TeaRoom teaRoom, TeaRoomType teaRoomType, LocalDateTime start, LocalDateTime end, Integer partySize) {
        List<Reservation> conflicts = findConflictingReservations(teaRoom.getId(), start, end);
        if (teaRoomType.getPricingMode() == TeaRoomPricingMode.PER_ROOM) {
            if (!conflicts.isEmpty()) {
                throw new BusinessException("该包厢在所选时段已被整间预订，请更换时段");
            }
            return;
        }

        int occupied = occupiedPartySize(conflicts, start, end);
        if (occupied + partySize > teaRoom.getCapacity()) {
            throw new BusinessException("该时段剩余容量不足，请减少人数或更换时段");
        }
    }

    private List<Reservation> findConflictingReservations(Long teaRoomId, LocalDateTime start, LocalDateTime end) {
        return reservationRepository.findByTeaRoomIdAndStatusNotInAndReservedEndTimeAfterAndReservedStartTimeBefore(
                teaRoomId,
                List.of(ReservationStatus.CANCELLED, ReservationStatus.COMPLETED, ReservationStatus.NO_SHOW),
                start,
                end
        );
    }

    private int occupiedPartySize(List<Reservation> reservations, LocalDateTime start, LocalDateTime end) {
        return reservations.stream()
                .filter(item -> item.getReservedStartTime().isBefore(end) && item.getReservedEndTime().isAfter(start))
                .mapToInt(item -> item.getPartySize() == null ? 1 : item.getPartySize())
                .sum();
    }

    private void restoreTeaStock(ProductOrder order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : items) {
            Tea tea = teaRepository.findById(item.getTeaId())
                    .orElseThrow(() -> new ResourceNotFoundException("茶叶不存在"));
            tea.setStock(tea.getStock() + item.getQuantity());
            teaRepository.save(tea);
        }
    }

    /**
     * 生成若干可预约候选时段，方便前端给用户快速推荐。
     */
    private List<ReservationAvailabilityResponse.ReservationTimeSlotResponse> buildSuggestedSlots(
            TeaRoom teaRoom,
            TeaRoomType teaRoomType,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int slotMinutes,
            int requestedPartySize,
            List<Reservation> conflicts
    ) {
        List<ReservationAvailabilityResponse.ReservationTimeSlotResponse> suggestions = new ArrayList<>();
        LocalDateTime cursor = alignToBusinessStart(teaRoom, rangeStart.isAfter(LocalDateTime.now()) ? rangeStart : LocalDateTime.now());
        LocalDateTime limit = rangeEnd.isAfter(cursor) ? rangeEnd : cursor.plusDays(7);

        while (!cursor.plusMinutes(slotMinutes).isAfter(limit) && suggestions.size() < 6) {
            LocalDateTime businessStart = cursor.with(teaRoom.getBusinessStartTime());
            LocalDateTime businessEnd = cursor.with(teaRoom.getBusinessEndTime());

            if (cursor.isBefore(businessStart)) {
                cursor = businessStart;
                continue;
            }
            if (cursor.plusMinutes(slotMinutes).isAfter(businessEnd)) {
                cursor = cursor.plusDays(1).with(teaRoom.getBusinessStartTime());
                continue;
            }

            LocalDateTime slotStart = cursor;
            LocalDateTime slotEnd = slotStart.plusMinutes(slotMinutes);
            if (teaRoomType.getPricingMode() == TeaRoomPricingMode.PER_ROOM) {
                boolean available = conflicts.stream().noneMatch(item ->
                        item.getReservedStartTime().isBefore(slotEnd) && item.getReservedEndTime().isAfter(slotStart));
                if (available) {
                    suggestions.add(new ReservationAvailabilityResponse.ReservationTimeSlotResponse(
                            slotStart,
                            slotEnd,
                            "推荐可约 | 可整间预订",
                            1
                    ));
                }
            } else {
                int occupied = occupiedPartySize(conflicts, slotStart, slotEnd);
                int remainingCapacity = Math.max(teaRoom.getCapacity() - occupied, 0);
                if (remainingCapacity >= requestedPartySize) {
                    suggestions.add(new ReservationAvailabilityResponse.ReservationTimeSlotResponse(
                            slotStart,
                            slotEnd,
                            "推荐可约 | 剩余 " + remainingCapacity + " 人",
                            remainingCapacity
                    ));
                }
            }
            cursor = cursor.plusHours(1);
        }
        return suggestions;
    }

    private LocalDateTime alignToBusinessStart(TeaRoom teaRoom, LocalDateTime time) {
        LocalDateTime rounded = roundUpToHour(time);
        LocalDateTime businessStart = rounded.with(teaRoom.getBusinessStartTime());
        if (rounded.toLocalTime().isBefore(teaRoom.getBusinessStartTime())) {
            return businessStart;
        }
        if (!rounded.toLocalTime().isBefore(teaRoom.getBusinessEndTime())) {
            return rounded.plusDays(1).with(teaRoom.getBusinessStartTime());
        }
        return rounded;
    }

    private LocalDateTime roundUpToHour(LocalDateTime time) {
        LocalDateTime truncated = time.truncatedTo(ChronoUnit.HOURS);
        return truncated.equals(time) ? truncated : truncated.plusHours(1);
    }

    private String statusLabel(ReservationStatus status) {
        return switch (status) {
            case PENDING -> "待确认";
            case CONFIRMED -> "已确认";
            case USER_CHECKED_IN -> "用户已签到";
            case STAFF_CONFIRMED_CHECK_IN -> "茶室员已确认";
            case CHARGING -> "进行中";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
            default -> status.name();
        };
    }

    private boolean isTransitionAllowed(ReservationStatus current, ReservationStatus next) {
        if (current == next) {
            return true;
        }

        return switch (current) {
            case PENDING -> next == ReservationStatus.CONFIRMED || next == ReservationStatus.CANCELLED;
            case CONFIRMED -> next == ReservationStatus.USER_CHECKED_IN || next == ReservationStatus.CANCELLED;
            case USER_CHECKED_IN -> next == ReservationStatus.STAFF_CONFIRMED_CHECK_IN || next == ReservationStatus.CANCELLED;
            case STAFF_CONFIRMED_CHECK_IN -> next == ReservationStatus.CHARGING || next == ReservationStatus.COMPLETED || next == ReservationStatus.CANCELLED;
            case CHARGING -> next == ReservationStatus.COMPLETED || next == ReservationStatus.CANCELLED;
            case NO_SHOW, CANCELLED, COMPLETED -> false;
        };
    }

    private void notifyReservationCreated(Reservation reservation, TeaRoom teaRoom) {
        notificationService.create(
                reservation.getUserId(),
                "RESERVATION_CREATED",
                "预约已提交",
                "你已成功提交“" + teaRoom.getName() + "”预约申请，请等待确认。",
                "RESERVATION",
                reservation.getId(),
                "/reservations"
        );
        if (teaRoom.getStaffUserId() != null) {
            notificationService.create(
                    teaRoom.getStaffUserId(),
                    "RESERVATION_PENDING",
                    "有新的预约待处理",
                    "茶室“" + teaRoom.getName() + "”收到新的预约申请。",
                    "RESERVATION",
                    reservation.getId(),
                    "/reservations"
            );
        }
    }

    private void notifyReservationStatusChanged(Reservation reservation) {
        TeaRoom teaRoom = requireTeaRoom(reservation.getTeaRoomId());
        String statusText = statusLabel(reservation.getStatus());
        notificationService.create(
                reservation.getUserId(),
                "RESERVATION_STATUS_UPDATED",
                "预约状态已更新",
                "你的预约“" + teaRoom.getName() + "”状态已更新为 " + statusText + "。",
                "RESERVATION",
                reservation.getId(),
                "/reservations"
        );
        if (teaRoom.getStaffUserId() != null) {
            notificationService.create(
                    teaRoom.getStaffUserId(),
                    "RESERVATION_STATUS_UPDATED",
                    "预约状态已更新",
                    "茶室“" + teaRoom.getName() + "”的一条预约状态已更新为 " + statusText + "。",
                    "RESERVATION",
                    reservation.getId(),
                    "/reservations"
            );
        }
    }

    private void notifyReservationNoShow(Reservation reservation, ProductOrder order) {
        TeaRoom teaRoom = requireTeaRoom(reservation.getTeaRoomId());
        notificationService.create(
                reservation.getUserId(),
                "RESERVATION_NO_SHOW",
                "预约已登记爽约",
                "你的预约“" + teaRoom.getName() + "”已登记爽约，订单 " + order.getOrderNo() + " 已标记为已爽约，系统仅释放资源不进行退款。",
                "RESERVATION",
                reservation.getId(),
                "/reservations"
        );
        if (teaRoom.getStaffUserId() != null) {
            notificationService.create(
                    teaRoom.getStaffUserId(),
                    "RESERVATION_NO_SHOW",
                    "爽约登记已完成",
                    "预约 #" + reservation.getId() + " 已登记爽约，关联资源已释放。",
                    "RESERVATION",
                    reservation.getId(),
                "/reservations"
            );
        }
    }

    private void notifyReservationAutoCompleted(Reservation reservation) {
        TeaRoom teaRoom = requireTeaRoom(reservation.getTeaRoomId());
        notificationService.create(
                reservation.getUserId(),
                "RESERVATION_AUTO_COMPLETED",
                "预约已自动完成",
                "你的预约“" + teaRoom.getName() + "”已到结束时间，系统已自动完成该预约。",
                "RESERVATION",
                reservation.getId(),
                "/reservations"
        );
    }
}

