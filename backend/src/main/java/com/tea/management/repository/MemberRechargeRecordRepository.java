package com.tea.management.repository;

import com.tea.management.domain.entity.MemberRechargeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * MemberRechargeRecordRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface MemberRechargeRecordRepository extends JpaRepository<MemberRechargeRecord, Long> {

    List<MemberRechargeRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
