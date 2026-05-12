package com.tea.management.repository;

import com.tea.management.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
/**
 * NotificationRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId, Pageable pageable);

    long countByRecipientUserIdAndReadFalse(Long recipientUserId);

    boolean existsByRecipientUserIdAndTypeAndTargetId(Long recipientUserId, String type, Long targetId);

    @Modifying
    @Query("update Notification n set n.read = true where n.id = :id and n.recipientUserId = :recipientUserId")
    int markRead(@Param("id") Long id, @Param("recipientUserId") Long recipientUserId);

    @Modifying
    @Query("update Notification n set n.read = true where n.recipientUserId = :recipientUserId and n.read = false")
    int markAllRead(@Param("recipientUserId") Long recipientUserId);
}
