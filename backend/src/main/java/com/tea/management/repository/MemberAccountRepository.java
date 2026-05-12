package com.tea.management.repository;

import com.tea.management.domain.entity.MemberAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
/**
 * MemberAccountRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface MemberAccountRepository extends JpaRepository<MemberAccount, Long> {

    Optional<MemberAccount> findByUserId(Long userId);

    List<MemberAccount> findByMemberLevel(String memberLevel);
}
