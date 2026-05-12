package com.tea.management.repository;

import com.tea.management.domain.entity.UserCoupon;
import com.tea.management.domain.enums.CouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
/**
 * UserCouponRepository 数据访问层，负责和数据库中的业务数据进行读写。
 */
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    Page<UserCoupon> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<UserCoupon> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<UserCoupon> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndIssuedWeekAndSourceLevel(Long userId, String issuedWeek, String sourceLevel);

    List<UserCoupon> findByStatusAndValidUntilBefore(CouponStatus status, LocalDateTime validUntil);

    List<UserCoupon> findByStatusAndValidUntilBetween(CouponStatus status, LocalDateTime start, LocalDateTime end);
}
