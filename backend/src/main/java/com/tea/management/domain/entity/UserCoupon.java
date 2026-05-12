package com.tea.management.domain.entity;

import com.tea.management.domain.enums.CouponStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * UserCoupon 实体类，对应系统中的一类持久化业务数据。
 */
@Getter
@Setter
@Entity
@Table(name = "user_coupon")
public class UserCoupon extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "coupon_code", nullable = false, unique = true, length = 64)
    private String couponCode;

    @Column(name = "threshold_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal thresholdAmount;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "source_level", nullable = false, length = 30)
    private String sourceLevel;

    @Column(name = "issued_week", nullable = false, length = 20)
    private String issuedWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponStatus status;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;
}
