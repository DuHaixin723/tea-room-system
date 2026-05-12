package com.tea.management.repository;

import com.tea.management.domain.entity.ConsultationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * ConsultationMessageRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface ConsultationMessageRepository extends JpaRepository<ConsultationMessage, Long> {

    List<ConsultationMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    @Modifying
    @Transactional
    void deleteBySessionId(Long sessionId);
}
