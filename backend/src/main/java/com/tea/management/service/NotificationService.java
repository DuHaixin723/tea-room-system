package com.tea.management.service;

import com.tea.management.domain.entity.Notification;
import com.tea.management.domain.entity.User;
import com.tea.management.domain.enums.RoleType;
import com.tea.management.dto.response.NotificationResponse;
import com.tea.management.dto.response.ResponseMapper;
import com.tea.management.exception.ResourceNotFoundException;
import com.tea.management.repository.NotificationRepository;
import com.tea.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * NotificationService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public Page<Notification> listByRecipient(Long recipientUserId, Pageable pageable) {
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(recipientUserId, pageable);
    }

    public long unreadCount(Long recipientUserId) {
        return notificationRepository.countByRecipientUserIdAndReadFalse(recipientUserId);
    }

    /**
     * 只把当前接收人的这一条通知标记为已读。
     */
    @Transactional
    public void markRead(Long id, Long recipientUserId) {
        if (notificationRepository.markRead(id, recipientUserId) == 0) {
            throw new ResourceNotFoundException("通知不存在");
        }
    }

    @Transactional
    public int markAllRead(Long recipientUserId) {
        return notificationRepository.markAllRead(recipientUserId);
    }

    /**
     * 创建通知记录后，再把同一条消息推送到实时通道。
     */
    @Transactional
    public Notification create(Long recipientUserId, String type, String title, String content, String targetType, Long targetId, String routePath) {
        Notification notification = new Notification();
        notification.setRecipientUserId(recipientUserId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setRoutePath(routePath);
        notification.setRead(false);
        Notification saved = notificationRepository.save(notification);
        push(saved);
        return saved;
    }

    /**
     * 给多位用户发送同一条通知前，先去掉重复接收人。
     */
    @Transactional
    public void createForUsers(List<Long> recipientUserIds, String type, String title, String content, String targetType, Long targetId, String routePath) {
        recipientUserIds.stream().distinct().forEach(userId -> create(userId, type, title, content, targetType, targetId, routePath));
    }

    /**
     * 把一条通知广播给所有管理员账号。
     */
    @Transactional
    public void notifyAdmins(String type, String title, String content, String targetType, Long targetId, String routePath) {
        List<Long> adminIds = userRepository.findByRole(RoleType.ADMIN).stream().map(User::getId).toList();
        createForUsers(adminIds, type, title, content, targetType, targetId, routePath);
    }

    private void push(Notification notification) {
        NotificationResponse payload = ResponseMapper.toNotificationResponse(notification);
        messagingTemplate.convertAndSend("/topic/notifications/" + notification.getRecipientUserId(), payload);
    }
}
