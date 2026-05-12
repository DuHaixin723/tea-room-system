package com.tea.management.repository;

import com.tea.management.domain.entity.ConsultationSession;
import com.tea.management.domain.enums.ConsultationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
/**
 * ConsultationSessionRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface ConsultationSessionRepository extends JpaRepository<ConsultationSession, Long> {

    List<ConsultationSession> findByUserId(Long userId);

    Page<ConsultationSession> findByUserId(Long userId, Pageable pageable);

    Page<ConsultationSession> findByAdminUserId(Long adminUserId, Pageable pageable);

    Page<ConsultationSession> findByUserIdOrAdminUserId(Long userId, Long adminUserId, Pageable pageable);

    Page<ConsultationSession> findByUserIdOrAdminUserIdOrTeaRoomIdIn(Long userId, Long adminUserId, List<Long> teaRoomIds, Pageable pageable);

    Optional<ConsultationSession> findFirstByOrderIdAndStatusOrderByIdDesc(Long orderId, ConsultationStatus status);
}
