package com.tea.management.service;

import com.tea.management.domain.entity.ConsultationMessage;
import com.tea.management.domain.entity.ConsultationSession;
import com.tea.management.domain.entity.ProductOrder;
import com.tea.management.domain.entity.Reservation;
import com.tea.management.domain.entity.TeaRoom;
import com.tea.management.domain.entity.User;
import com.tea.management.domain.enums.ConsultationStatus;
import com.tea.management.domain.enums.RoleType;
import com.tea.management.dto.request.ConsultationMessageRequest;
import com.tea.management.dto.request.ConsultationSessionRequest;
import com.tea.management.dto.response.ConsultationDetailResponse;
import com.tea.management.dto.response.ConsultationMessageResponse;
import com.tea.management.dto.response.ConsultationSessionResponse;
import com.tea.management.exception.BusinessException;
import com.tea.management.exception.ResourceNotFoundException;
import com.tea.management.repository.ConsultationMessageRepository;
import com.tea.management.repository.ConsultationSessionRepository;
import com.tea.management.repository.ProductOrderRepository;
import com.tea.management.repository.ReservationRepository;
import com.tea.management.repository.TeaRoomRepository;
import com.tea.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 * ConsultationService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationSessionRepository sessionRepository;
    private final ConsultationMessageRepository messageRepository;
    private final TeaRoomRepository teaRoomRepository;
    private final UserRepository userRepository;
    private final ProductOrderRepository productOrderRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;

    public ConsultationSession createSession(ConsultationSessionRequest request, RoleType creatorRole, Long creatorUserId) {
        ProductOrder order = productOrderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        Reservation reservation = loadReservation(order);
        TeaRoom teaRoom = loadTeaRoom(null, reservation);

        if (teaRoom != null && !Boolean.TRUE.equals(teaRoom.getEnabled())) {
            throw new BusinessException("该茶室未启用，无法发起会话");
        }
        if (!request.userId().equals(order.getUserId())) {
            throw new BusinessException("会话顾客必须与订单所属顾客一致");
        }
        if (creatorRole == RoleType.USER && !creatorUserId.equals(order.getUserId())) {
            throw new BusinessException("只能基于自己的订单发起会话");
        }
        if (creatorRole == RoleType.STAFF && (teaRoom == null || !creatorUserId.equals(teaRoom.getStaffUserId()))) {
            throw new BusinessException("只能基于自己负责的订单发起管理员协作会话");
        }

        var existing = sessionRepository.findFirstByOrderIdAndStatusOrderByIdDesc(order.getId(), ConsultationStatus.OPEN);
        if (existing.isPresent()) {
            return existing.get();
        }

        ConsultationSession session = new ConsultationSession();
        session.setUserId(order.getUserId());
        session.setOrderId(order.getId());
        session.setTeaRoomId(teaRoom == null ? null : teaRoom.getId());
        session.setStatus(ConsultationStatus.OPEN);

        Long adminId = userRepository.findByRole(RoleType.ADMIN).stream()
                .map(User::getId)
                .findFirst()
                .orElseThrow(() -> new BusinessException("当前没有可分配的管理员"));
        session.setAdminUserId(adminId);

        ConsultationSession saved = sessionRepository.save(session);
        if (saved.getAdminUserId() != null) {
            notificationService.create(
                    saved.getAdminUserId(),
                    "CONSULTATION_SESSION_CREATED",
                    "有新的协作会话",
                    "订单 #" + saved.getOrderId() + " 创建了新的在线咨询会话。",
                    "CONSULTATION",
                    saved.getId(),
                    "/consultations"
            );
        }
        if (teaRoom != null && teaRoom.getStaffUserId() != null && !teaRoom.getStaffUserId().equals(saved.getAdminUserId())) {
            notificationService.create(
                    teaRoom.getStaffUserId(),
                    "CONSULTATION_SESSION_CREATED",
                    "有新的在线咨询",
                    "订单 #" + saved.getOrderId() + " 创建了新的在线咨询会话。",
                    "CONSULTATION",
                    saved.getId(),
                    "/consultations"
            );
        }
        return saved;
    }

    public Page<ConsultationSession> listAllSessions(Pageable pageable) {
        return sessionRepository.findAll(pageable);
    }

    public Page<ConsultationSession> listUserSessions(Long userId, Pageable pageable) {
        return sessionRepository.findByUserId(userId, pageable);
    }

    public Page<ConsultationSession> listParticipantSessions(RoleType role, Long userId, Pageable pageable) {
        if (role == RoleType.STAFF) {
            List<Long> teaRoomIds = teaRoomRepository.findByStaffUserId(userId).stream()
                    .map(TeaRoom::getId)
                    .toList();
            if (!teaRoomIds.isEmpty()) {
                return sessionRepository.findByUserIdOrAdminUserIdOrTeaRoomIdIn(userId, userId, teaRoomIds, pageable);
            }
        }
        return sessionRepository.findByUserIdOrAdminUserId(userId, userId, pageable);
    }

    public ConsultationSession requireSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("会话不存在"));
    }

    public ConsultationMessage sendMessage(ConsultationMessageRequest request) {
        ConsultationSession session = requireSession(request.sessionId());
        ConsultationMessage message = new ConsultationMessage();
        message.setSessionId(request.sessionId());
        message.setSenderId(request.senderId());
        message.setMentionAdmin(Boolean.TRUE.equals(request.mentionAdmin()));
        message.setMentionStaff(Boolean.TRUE.equals(request.mentionStaff()));
        message.setContent(request.content());
        ConsultationMessage saved = messageRepository.save(message);
        notifyMentionTargets(session, saved);
        return saved;
    }

    public ConsultationMessageResponse sendMessageResponse(ConsultationMessageRequest request) {
        return toMessageResponse(sendMessage(request));
    }

    public ConsultationDetailResponse detail(Long sessionId) {
        ConsultationSession session = requireSession(sessionId);
        List<ConsultationMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        Map<Long, User> users = loadUsersForDetail(session, messages);
        ProductOrder order = loadOrder(session.getOrderId());
        Reservation reservation = loadReservation(order);
        TeaRoom teaRoom = loadTeaRoom(session.getTeaRoomId(), reservation);
        User supportAdmin = findSupportAdmin();
        return new ConsultationDetailResponse(
                toSessionResponse(session, users, order, reservation, teaRoom, supportAdmin),
                messages.stream().map(message -> toMessageResponse(message, users)).toList()
        );
    }

    public ConsultationSession closeSession(Long sessionId) {
        ConsultationSession session = requireSession(sessionId);
        session.setStatus(ConsultationStatus.CLOSED);
        ConsultationSession saved = sessionRepository.save(session);
        notifySessionClosed(saved);
        return saved;
    }

    private void notifyMentionTargets(ConsultationSession session, ConsultationMessage message) {
        if (Boolean.TRUE.equals(message.getMentionAdmin()) && session.getAdminUserId() != null && !session.getAdminUserId().equals(message.getSenderId())) {
            notificationService.create(
                    session.getAdminUserId(),
                    "CONSULTATION_MENTION",
                    "咨询消息提到了你",
                    "会话 #" + session.getId() + " 中有消息 @ 了你。",
                    "CONSULTATION",
                    session.getId(),
                    "/consultations"
            );
        }

        TeaRoom teaRoom = loadTeaRoom(session.getTeaRoomId(), loadReservation(loadOrder(session.getOrderId())));
        if (Boolean.TRUE.equals(message.getMentionStaff())
                && teaRoom != null
                && teaRoom.getStaffUserId() != null
                && !teaRoom.getStaffUserId().equals(message.getSenderId())) {
            notificationService.create(
                    teaRoom.getStaffUserId(),
                    "CONSULTATION_MENTION",
                    "咨询消息提到了你",
                    "会话 #" + session.getId() + " 中有消息 @ 了你。",
                    "CONSULTATION",
                    session.getId(),
                    "/consultations"
            );
        }
    }

    private void notifySessionClosed(ConsultationSession session) {
        if (session.getUserId() != null) {
            notificationService.create(
                    session.getUserId(),
                    "CONSULTATION_CLOSED",
                    "咨询会话已关闭",
                    "会话 #" + session.getId() + " 已关闭。",
                    "CONSULTATION",
                    session.getId(),
                    "/consultations"
            );
        }
        if (session.getAdminUserId() != null) {
            notificationService.create(
                    session.getAdminUserId(),
                    "CONSULTATION_CLOSED",
                    "咨询会话已关闭",
                    "会话 #" + session.getId() + " 已关闭。",
                    "CONSULTATION",
                    session.getId(),
                    "/consultations"
            );
        }
        TeaRoom teaRoom = loadTeaRoom(session.getTeaRoomId(), loadReservation(loadOrder(session.getOrderId())));
        if (teaRoom != null && teaRoom.getStaffUserId() != null) {
            notificationService.create(
                    teaRoom.getStaffUserId(),
                    "CONSULTATION_CLOSED",
                    "咨询会话已关闭",
                    "会话 #" + session.getId() + " 已关闭。",
                    "CONSULTATION",
                    session.getId(),
                    "/consultations"
            );
        }
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        requireSession(sessionId);
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    public ConsultationSessionResponse toSessionResponse(ConsultationSession session) {
        Set<Long> userIds = new HashSet<>();
        userIds.add(session.getUserId());
        userIds.add(session.getAdminUserId());
        ProductOrder order = loadOrder(session.getOrderId());
        Reservation reservation = loadReservation(order);
        TeaRoom teaRoom = loadTeaRoom(session.getTeaRoomId(), reservation);
        User supportAdmin = findSupportAdmin();
        if (teaRoom != null) {
            userIds.add(teaRoom.getStaffUserId());
        }
        if (supportAdmin != null) {
            userIds.add(supportAdmin.getId());
        }
        return toSessionResponse(session, loadUsers(userIds), order, reservation, teaRoom, supportAdmin);
    }

    public ConsultationMessageResponse toMessageResponse(ConsultationMessage message) {
        Set<Long> userIds = new HashSet<>();
        userIds.add(message.getSenderId());
        return toMessageResponse(message, loadUsers(userIds));
    }

    public boolean isParticipant(ConsultationSession session, Long currentUserId, RoleType role) {
        if (currentUserId.equals(session.getUserId()) || currentUserId.equals(session.getAdminUserId())) {
            return true;
        }
        return role == RoleType.STAFF
                && session.getTeaRoomId() != null
                && teaRoomRepository.existsByIdAndStaffUserId(session.getTeaRoomId(), currentUserId);
    }

    private ConsultationSessionResponse toSessionResponse(ConsultationSession session,
                                                          Map<Long, User> users,
                                                          ProductOrder order,
                                                          Reservation reservation,
                                                          TeaRoom teaRoom,
                                                          User supportAdmin) {
        User user = users.get(session.getUserId());
        User admin = users.get(session.getAdminUserId());
        User staff = teaRoom == null ? null : users.get(teaRoom.getStaffUserId());
        Long teaRoomId = teaRoom == null ? session.getTeaRoomId() : teaRoom.getId();

        return new ConsultationSessionResponse(
                session.getId(),
                session.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getNickname(),
                user == null ? null : user.getAvatarUrl(),
                order == null ? session.getOrderId() : order.getId(),
                order == null ? null : order.getOrderNo(),
                reservation == null ? (order == null ? null : order.getReservationId()) : reservation.getId(),
                teaRoomId,
                session.getAdminUserId(),
                admin == null ? null : admin.getUsername(),
                admin == null ? null : admin.getNickname(),
                admin == null ? null : admin.getAvatarUrl(),
                staff == null ? null : staff.getId(),
                staff == null ? null : staff.getUsername(),
                staff == null ? null : staff.getNickname(),
                staff == null ? null : staff.getAvatarUrl(),
                supportAdmin == null ? null : supportAdmin.getId(),
                supportAdmin == null ? null : supportAdmin.getUsername(),
                supportAdmin == null ? null : supportAdmin.getNickname(),
                supportAdmin == null ? null : supportAdmin.getAvatarUrl(),
                session.getStatus()
        );
    }

    private ConsultationMessageResponse toMessageResponse(ConsultationMessage message, Map<Long, User> users) {
        User sender = users.get(message.getSenderId());
        return new ConsultationMessageResponse(
                message.getId(),
                message.getSessionId(),
                message.getSenderId(),
                sender == null ? null : sender.getUsername(),
                sender == null ? null : sender.getNickname(),
                sender == null ? null : sender.getAvatarUrl(),
                Boolean.TRUE.equals(message.getMentionAdmin()),
                Boolean.TRUE.equals(message.getMentionStaff()),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    private Map<Long, User> loadUsersForDetail(ConsultationSession session, List<ConsultationMessage> messages) {
        Set<Long> userIds = new HashSet<>();
        userIds.add(session.getUserId());
        userIds.add(session.getAdminUserId());

        ProductOrder order = loadOrder(session.getOrderId());
        Reservation reservation = loadReservation(order);
        TeaRoom teaRoom = loadTeaRoom(session.getTeaRoomId(), reservation);
        if (teaRoom != null) {
            userIds.add(teaRoom.getStaffUserId());
        }

        User supportAdmin = findSupportAdmin();
        if (supportAdmin != null) {
            userIds.add(supportAdmin.getId());
        }
        for (ConsultationMessage message : messages) {
            userIds.add(message.getSenderId());
        }
        return loadUsers(userIds);
    }

    private ProductOrder loadOrder(Long orderId) {
        if (orderId == null) {
            return null;
        }
        return productOrderRepository.findById(orderId).orElse(null);
    }

    private Reservation loadReservation(ProductOrder order) {
        if (order == null || order.getReservationId() == null) {
            return null;
        }
        return reservationRepository.findById(order.getReservationId()).orElse(null);
    }

    private TeaRoom loadTeaRoom(Long teaRoomId, Reservation reservation) {
        Long resolvedTeaRoomId = teaRoomId;
        if (resolvedTeaRoomId == null && reservation != null) {
            resolvedTeaRoomId = reservation.getTeaRoomId();
        }
        if (resolvedTeaRoomId == null) {
            return null;
        }
        return teaRoomRepository.findById(resolvedTeaRoomId).orElse(null);
    }

    private User findSupportAdmin() {
        return userRepository.findByRole(RoleType.ADMIN).stream()
                .findFirst()
                .orElse(null);
    }

    private Map<Long, User> loadUsers(Set<Long> userIds) {
        Set<Long> ids = new HashSet<>();
        for (Long userId : userIds) {
            if (userId != null) {
                ids.add(userId);
            }
        }

        Map<Long, User> result = new HashMap<>();
        if (ids.isEmpty()) {
            return result;
        }

        for (User user : userRepository.findAllById(ids)) {
            result.put(user.getId(), user);
        }
        return result;
    }
}
