package com.tea.management.service;

import com.tea.management.domain.entity.MemberAccount;
import com.tea.management.domain.entity.OrderItem;
import com.tea.management.domain.entity.ProductOrder;
import com.tea.management.domain.entity.Reservation;
import com.tea.management.domain.entity.Tea;
import com.tea.management.domain.entity.TeaRoom;
import com.tea.management.domain.entity.TeaRoomType;
import com.tea.management.domain.enums.OrderStatus;
import com.tea.management.domain.enums.PaymentMethod;
import com.tea.management.domain.enums.ReservationStatus;
import com.tea.management.dto.request.OrderCreateRequest;
import com.tea.management.dto.request.OrderItemsUpdateRequest;
import com.tea.management.dto.request.OrderPayRequest;
import com.tea.management.dto.request.OrderStatusRequest;
import com.tea.management.dto.response.OrderDetailResponse;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.exception.BusinessException;
import com.tea.management.exception.ResourceNotFoundException;
import com.tea.management.security.SecurityUtils;
import com.tea.management.repository.MemberAccountRepository;
import com.tea.management.repository.OrderItemRepository;
import com.tea.management.repository.ProductOrderRepository;
import com.tea.management.repository.ReservationRepository;
import com.tea.management.repository.TeaRepository;
import com.tea.management.repository.TeaRoomRepository;
import com.tea.management.repository.TeaRoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
/**
 * OrderService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final ProductOrderRepository productOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final TeaRepository teaRepository;
    private final ReservationRepository reservationRepository;
    private final TeaRoomRepository teaRoomRepository;
    private final TeaRoomTypeRepository teaRoomTypeRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final MemberLevelService memberLevelService;
    private final NotificationService notificationService;
    private final ReservationService reservationService;

    public Page<ProductOrder> listAll(Pageable pageable) {
        return productOrderRepository.findAll(pageable);
    }

    public Page<ProductOrder> listByUser(Long userId, Pageable pageable) {
        return productOrderRepository.findByUserId(userId, pageable);
    }

    public Page<ProductOrder> listByStaff(Long staffUserId, Pageable pageable) {
        List<Long> teaRoomIds = teaRoomRepository.findByStaffUserId(staffUserId).stream()
                .map(teaRoom -> teaRoom.getId())
                .toList();
        if (teaRoomIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> reservationIds = reservationRepository.findByTeaRoomIdIn(teaRoomIds).stream()
                .map(Reservation::getId)
                .toList();
        if (reservationIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return productOrderRepository.findByReservationIdIn(reservationIds, pageable);
    }

    public ProductOrder requireOrder(Long id) {
        return productOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
    }

    public boolean isOrderAssignedToStaff(ProductOrder order, Long staffUserId) {
        if (order.getReservationId() == null) {
            return false;
        }
        Reservation reservation = reservationRepository.findById(order.getReservationId()).orElse(null);
        if (reservation == null) {
            return false;
        }
        return teaRoomRepository.existsByIdAndStaffUserId(reservation.getTeaRoomId(), staffUserId);
    }

    /**
     * 先创建订单主记录，再写入明细并重新计算应付金额。
     */
    @Transactional
    public ProductOrder create(OrderCreateRequest request) {
        Reservation reservation = validateReservation(request.userId(), request.reservationId());
        if (reservation != null && reservation.getStatus() == ReservationStatus.NO_SHOW) {
            throw new BusinessException("该预约已登记爽约，不允许继续操作");
        }

        ProductOrder order = new ProductOrder();
        order.setOrderNo("ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        order.setUserId(request.userId());
        order.setReservationId(reservation != null ? reservation.getId() : null);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setAmount(BigDecimal.ZERO);
        ProductOrder savedOrder = productOrderRepository.save(order);

        BigDecimal totalAmount = replaceItems(savedOrder, toItemMap(request.items().stream()
                .map(item -> new OrderItemsUpdateRequest.OrderItemRequest(item.teaId(), item.quantity()))
                .toList()));
        savedOrder.setAmount(totalAmount);
        ProductOrder result = productOrderRepository.save(savedOrder);
        notifyOrderCreated(result);
        return result;
    }

    /**
     * 修改订单状态时，同步保持关联预约的状态一致。
     */
    @Transactional
    public ProductOrder updateStatus(Long id, OrderStatusRequest request) {
        ProductOrder order = requireOrder(id);
        OrderStatus next = OrderStatus.valueOf(request.status().toUpperCase());
        if (next == order.getStatus()) {
            throw new BusinessException("订单状态未发生变化");
        }
        if (next == OrderStatus.REFUNDED) {
            if (!SecurityUtils.hasRole("ADMIN")) {
                throw new BusinessException("仅管理员可以执行订单退款");
            }
            return refundOrder(order);
        }
        if (next == OrderStatus.CANCELLED && order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("仅待支付订单允许取消");
        }
        if (next != OrderStatus.CANCELLED && next != OrderStatus.COMPLETED) {
            throw new BusinessException("当前仅支持取消、完成或退款操作");
        }
        if (next == OrderStatus.COMPLETED && order.getReservationId() != null) {
            Reservation reservation = reservationRepository.findById(order.getReservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("关联预约不存在"));
            if (reservation.getStatus() != ReservationStatus.STAFF_CONFIRMED_CHECK_IN
                    && reservation.getStatus() != ReservationStatus.CHARGING
                    && reservation.getStatus() != ReservationStatus.COMPLETED) {
                throw new BusinessException("茶室员确认到店后才能完成订单");
            }
            if (reservation.getStatus() != ReservationStatus.COMPLETED) {
                reservation.setStatus(ReservationStatus.COMPLETED);
                reservationRepository.save(reservation);
            }
        }
        order.setStatus(next);
        ProductOrder saved = productOrderRepository.save(order);
        notifyOrderStatusUpdated(saved);
        return saved;
    }

    public boolean canUserCompleteEarly(ProductOrder order) {
        if (order.getStatus() != OrderStatus.PAID || order.getReservationId() == null) {
            return false;
        }
        Reservation reservation = reservationRepository.findById(order.getReservationId()).orElse(null);
        if (reservation == null) {
            return false;
        }
        return reservation.getStatus() == ReservationStatus.USER_CHECKED_IN
                || reservation.getStatus() == ReservationStatus.STAFF_CONFIRMED_CHECK_IN
                || reservation.getStatus() == ReservationStatus.CHARGING;
    }

    /**
     * 重建订单明细列表，并在明细变化后重新调整应付金额。
     */
    @Transactional
    public ProductOrder updateItems(Long id, OrderItemsUpdateRequest request) {
        ProductOrder order = requireOrder(id);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.REFUNDED) {
            throw new BusinessException("已结束订单不允许修改明细");
        }

        BigDecimal oldAmount = order.getAmount();
        BigDecimal newAmount = replaceItems(order, toItemMap(request.items()));
        order.setAmount(newAmount);
        reconcilePaidOrderAmount(order, oldAmount, newAmount);
        ProductOrder saved = productOrderRepository.save(order);
        notifyOrderItemsUpdated(saved, oldAmount, newAmount);
        return saved;
    }

    /**
     * 完成订单支付；若使用余额支付，还要同步扣减会员账户余额。
     */
    @Transactional
    public ProductOrder pay(Long id, Long userId, OrderPayRequest request) {
        ProductOrder order = requireOrder(id);
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException("只能支付自己的订单");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("当前订单状态不支持支付");
        }

        PaymentMethod paymentMethod = PaymentMethod.valueOf(request.paymentMethod().toUpperCase());
        MemberAccount account = memberAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("会员账户不存在"));

        if (paymentMethod == PaymentMethod.BALANCE) {
            if (account.getBalance().compareTo(order.getAmount()) < 0) {
                throw new BusinessException("会员余额不足，请先充值或切换其他支付方式");
            }
            account.setBalance(account.getBalance().subtract(order.getAmount()));
        }

        account.setCumulativeSpend(account.getCumulativeSpend().add(order.getAmount()));
        account.setMemberLevel(memberLevelService.resolveLevel(account.getCumulativeRecharge(), account.getCumulativeSpend()));
        memberAccountRepository.save(account);

        order.setStatus(OrderStatus.PAID);
        order.setPaymentMethod(paymentMethod);
        ProductOrder saved = productOrderRepository.save(order);
        notifyOrderPaid(saved);
        return saved;
    }

    public OrderDetailResponse detail(Long id) {
        ProductOrder order = requireOrder(id);
        return ResponseMapper.toOrderDetailResponse(order, orderItemRepository.findByOrderId(id));
    }

    /**
     * 创建预约确认后产生的预约费用订单。
     */
    @Transactional
    public ProductOrder createReservationOrder(Reservation reservation) {
        ProductOrder existingReservationFeeOrder = findReservationFeeOrder(reservation.getId());
        if (existingReservationFeeOrder != null) {
            return existingReservationFeeOrder;
        }

        TeaRoom teaRoom = teaRoomRepository.findById(reservation.getTeaRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("茶室不存在"));
        TeaRoomType teaRoomType = teaRoomTypeRepository.findById(teaRoom.getTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("茶室类型不存在"));

        ProductOrder order = new ProductOrder();
        order.setOrderNo("ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        order.setUserId(reservation.getUserId());
        order.setReservationId(reservation.getId());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setAmount(reservationChargeAmount(reservation, teaRoomType));
        ProductOrder saved = productOrderRepository.save(order);

        notificationService.create(
                saved.getUserId(),
                "RESERVATION_PAYMENT_REQUIRED",
                "预约已确认，请尽快支付",
                "你的茶室预约已确认，请前往订单中心支付订单 " + saved.getOrderNo() + "。",
                "ORDER",
                saved.getId(),
                "/orders"
        );
        return saved;
    }

    public boolean hasPaidReservationOrder(Long reservationId) {
        ProductOrder reservationFeeOrder = findReservationFeeOrder(reservationId);
        return reservationFeeOrder != null
                && (reservationFeeOrder.getStatus() == OrderStatus.PAID || reservationFeeOrder.getStatus() == OrderStatus.COMPLETED);
    }

    private ProductOrder findReservationFeeOrder(Long reservationId) {
        return productOrderRepository.findByReservationId(reservationId).stream()
                .filter(this::isReservationFeeOrder)
                .findFirst()
                .orElse(null);
    }

    private boolean isReservationFeeOrder(ProductOrder order) {
        return orderItemRepository.findByOrderId(order.getId()).isEmpty();
    }

    /**
     * 自动结束过期的活动订单，避免支付和服务状态长期挂起。
     */
    @Transactional
    public int completeExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<ProductOrder> orders = productOrderRepository.findByStatusInAndReservationIdIsNotNull(List.of(OrderStatus.PAID));
        int count = 0;
        List<ProductOrder> completedOrders = new java.util.ArrayList<>();
        for (ProductOrder order : orders) {
            Reservation reservation = reservationRepository.findById(order.getReservationId()).orElse(null);
            if (reservation == null) {
                continue;
            }
            if (!reservation.getReservedEndTime().isAfter(now)) {
                order.setStatus(OrderStatus.COMPLETED);
                completedOrders.add(order);
                count++;
            }
        }
        productOrderRepository.saveAll(completedOrders);
        return count;
    }

    private Reservation validateReservation(Long userId, Long reservationId) {
        if (reservationId == null) {
            return null;
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("关联预约不存在"));
        if (!userId.equals(reservation.getUserId())) {
            throw new BusinessException("只能关联自己的预约记录");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new BusinessException("当前预约状态不支持加购茶叶");
        }
        return reservation;
    }

    private BigDecimal reservationChargeAmount(Reservation reservation, TeaRoomType teaRoomType) {
        return reservationService.calculateReservationAmount(
                teaRoomType,
                reservation.getPartySize(),
                reservation.getReservedStartTime(),
                reservation.getReservedEndTime()
        );
    }

    private Map<Long, Integer> toItemMap(List<OrderItemsUpdateRequest.OrderItemRequest> items) {
        Map<Long, Integer> result = new HashMap<>();
        for (OrderItemsUpdateRequest.OrderItemRequest item : items) {
            result.merge(item.teaId(), item.quantity(), Integer::sum);
        }
        return result;
    }

    private BigDecimal replaceItems(ProductOrder order, Map<Long, Integer> requestedItems) {
        List<OrderItem> existingItems = orderItemRepository.findByOrderId(order.getId());
        Map<Long, Integer> existingByTea = new HashMap<>();
        for (OrderItem item : existingItems) {
            existingByTea.merge(item.getTeaId(), item.getQuantity(), Integer::sum);
        }

        Map<Long, Integer> teaIds = new HashMap<>();
        teaIds.putAll(existingByTea);
        teaIds.putAll(requestedItems);

        for (Long teaId : teaIds.keySet()) {
            int oldQty = existingByTea.getOrDefault(teaId, 0);
            int newQty = requestedItems.getOrDefault(teaId, 0);
            int diff = newQty - oldQty;
            if (diff == 0) {
                continue;
            }
            Tea tea = teaRepository.findById(teaId)
                    .orElseThrow(() -> new ResourceNotFoundException("茶叶不存在"));
            if (diff > 0 && tea.getStock() < diff) {
                throw new BusinessException("茶叶库存不足: " + tea.getName());
            }
            tea.setStock(tea.getStock() - diff);
            teaRepository.save(tea);
        }

        orderItemRepository.deleteByOrderId(order.getId());

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : requestedItems.entrySet()) {
            Tea tea = teaRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("茶叶不存在"));
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setTeaId(tea.getId());
            orderItem.setQuantity(entry.getValue());
            orderItem.setUnitPrice(tea.getPrice());
            orderItemRepository.save(orderItem);

            total = total.add(tea.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
        }
        return total;
    }

    private void reconcilePaidOrderAmount(ProductOrder order, BigDecimal oldAmount, BigDecimal newAmount) {
        if (order.getStatus() != OrderStatus.PAID || order.getPaymentMethod() != PaymentMethod.BALANCE) {
            return;
        }

        BigDecimal delta = newAmount.subtract(oldAmount);
        if (delta.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        MemberAccount account = memberAccountRepository.findByUserId(order.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("会员账户不存在"));

        if (delta.compareTo(BigDecimal.ZERO) > 0) {
            if (account.getBalance().compareTo(delta) < 0) {
                throw new BusinessException("余额不足，无法追加订单金额");
            }
            account.setBalance(account.getBalance().subtract(delta));
            account.setCumulativeSpend(account.getCumulativeSpend().add(delta));
        } else {
            BigDecimal refund = delta.abs();
            account.setBalance(account.getBalance().add(refund));
            account.setCumulativeSpend(account.getCumulativeSpend().subtract(refund).max(BigDecimal.ZERO));
        }

        account.setMemberLevel(memberLevelService.resolveLevel(account.getCumulativeRecharge(), account.getCumulativeSpend()));
        memberAccountRepository.save(account);
    }

    private ProductOrder refundOrder(ProductOrder order) {
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("仅已支付或已完成订单允许退款");
        }

        refundToMemberBalance(order);
        restoreTeaStock(order);
        releaseReservationResource(order);

        order.setStatus(OrderStatus.REFUNDED);
        ProductOrder saved = productOrderRepository.save(order);
        notifyOrderRefunded(saved);
        return saved;
    }

    private void refundToMemberBalance(ProductOrder order) {
        MemberAccount account = memberAccountRepository.findByUserId(order.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("会员账户不存在"));

        account.setBalance(account.getBalance().add(order.getAmount()));
        account.setCumulativeSpend(account.getCumulativeSpend().subtract(order.getAmount()).max(BigDecimal.ZERO));
        account.setMemberLevel(memberLevelService.resolveLevel(account.getCumulativeRecharge(), account.getCumulativeSpend()));
        memberAccountRepository.save(account);
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

    private void releaseReservationResource(ProductOrder order) {
        if (order.getReservationId() == null || !isReservationFeeOrder(order)) {
            return;
        }

        Reservation reservation = reservationRepository.findById(order.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException("关联预约不存在"));
        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.COMPLETED) {
            return;
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
        notifyReservationReleasedByRefund(reservation, order);
    }
    private void notifyOrderCreated(ProductOrder order) {
        notificationService.create(
                order.getUserId(),
                "ORDER_CREATED",
                "订单已创建",
                "订单 " + order.getOrderNo() + " 已创建，请尽快完成支付。",
                "ORDER",
                order.getId(),
                "/orders"
        );
        notifyAssignedStaff(order, "ORDER_CREATED", "有新的订单待关注", "关联预约产生了新的订单 " + order.getOrderNo() + "。");
    }

    private void notifyOrderPaid(ProductOrder order) {
        notificationService.create(
                order.getUserId(),
                "ORDER_PAID",
                "订单支付成功",
                "订单 " + order.getOrderNo() + " 已支付成功。",
                "ORDER",
                order.getId(),
                "/orders"
        );
        notifyAssignedStaff(order, "ORDER_PAID", "订单已支付", "订单 " + order.getOrderNo() + " 已完成支付。");
    }

    private void notifyOrderItemsUpdated(ProductOrder order, BigDecimal oldAmount, BigDecimal newAmount) {
        notificationService.create(
                order.getUserId(),
                "ORDER_ITEMS_UPDATED",
                "订单商品已更新",
                "订单 " + order.getOrderNo() + " 商品已更新，金额从 " + oldAmount + " 调整为 " + newAmount + "。",
                "ORDER",
                order.getId(),
                "/orders"
        );
        notifyAssignedStaff(order, "ORDER_ITEMS_UPDATED", "订单商品已更新", "订单 " + order.getOrderNo() + " 的商品明细已更新。");
    }

    private void notifyOrderStatusUpdated(ProductOrder order) {
        if (order.getStatus() == OrderStatus.COMPLETED) {
            notifyOrderCompletedWithFollowUp(order);
            return;
        }
        notificationService.create(
                order.getUserId(),
                "ORDER_STATUS_UPDATED",
                "订单状态已更新",
                "订单 " + order.getOrderNo() + " 状态已更新为 " + order.getStatus().name() + "。",
                "ORDER",
                order.getId(),
                "/orders"
        );
        notifyAssignedStaff(order, "ORDER_STATUS_UPDATED", "订单状态已更新", "订单 " + order.getOrderNo() + " 状态已更新为 " + order.getStatus().name() + "。");
    }
    private void notifyOrderRefunded(ProductOrder order) {
        notificationService.create(
                order.getUserId(),
                "ORDER_REFUNDED",
                "订单已退款",
                "订单 " + order.getOrderNo() + " 已退款，退款金额已退回账户余额。",
                "ORDER",
                order.getId(),
                "/orders"
        );
        notifyAssignedStaff(
                order,
                "ORDER_REFUNDED",
                "订单已退款",
                "订单 " + order.getOrderNo() + " 已完成退款，相关占用资源已回滚。"
        );
    }

    private void notifyReservationReleasedByRefund(Reservation reservation, ProductOrder order) {
        notificationService.create(
                reservation.getUserId(),
                "RESERVATION_CANCELLED_BY_REFUND",
                "预约已因退款释放",
                "订单 " + order.getOrderNo() + " 退款成功，关联茶室预约已释放。",
                "RESERVATION",
                reservation.getId(),
                "/reservations"
        );

        TeaRoom teaRoom = teaRoomRepository.findById(reservation.getTeaRoomId()).orElse(null);
        if (teaRoom != null && teaRoom.getStaffUserId() != null) {
            notificationService.create(
                    teaRoom.getStaffUserId(),
                    "RESERVATION_CANCELLED_BY_REFUND",
                    "预约已因退款释放",
                    "订单 " + order.getOrderNo() + " 已退款，关联预约资源已释放。",
                    "RESERVATION",
                    reservation.getId(),
                    "/reservations"
            );
        }
    }

    private void notifyAssignedStaff(ProductOrder order, String type, String title, String content) {
        if (order.getReservationId() == null) {
            return;
        }
        Reservation reservation = reservationRepository.findById(order.getReservationId()).orElse(null);
        if (reservation == null) {
            return;
        }
        var teaRoom = teaRoomRepository.findById(reservation.getTeaRoomId()).orElse(null);
        if (teaRoom == null || teaRoom.getStaffUserId() == null) {
            return;
        }
        notificationService.create(
                teaRoom.getStaffUserId(),
                type,
                title,
                content,
                "ORDER",
                order.getId(),
                "/orders"
        );
    }

    private void notifyOrderCompletedWithFollowUp(ProductOrder order) {
        String userContent = "订单 " + order.getOrderNo() + " 已完成，茶室已释放，可前往订单中心提交服务评价或继续后续咨询。";
        notificationService.create(
                order.getUserId(),
                "ORDER_COMPLETED",
                "订单已完成",
                userContent,
                "ORDER",
                order.getId(),
                "/orders"
        );
        notifyAssignedStaff(
                order,
                "ORDER_COMPLETED",
                "订单已完成",
                "订单 " + order.getOrderNo() + " 已完成，关联茶室预约已释放，可继续接待后续预约。"
        );
    }
}


