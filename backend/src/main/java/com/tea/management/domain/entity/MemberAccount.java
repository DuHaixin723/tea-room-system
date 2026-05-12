package com.tea.management.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
/**
 * MemberAccount 实体类，对应系统中的一类持久化业务数据。
 */
@Getter
@Setter
@Entity
@Table(name = "member_account")
public class MemberAccount extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance;

    @Column(name = "cumulative_recharge", nullable = false, precision = 10, scale = 2)
    private BigDecimal cumulativeRecharge;

    @Column(name = "cumulative_spend", nullable = false, precision = 10, scale = 2)
    private BigDecimal cumulativeSpend;

    @Column(name = "member_level", nullable = false, length = 30)
    private String memberLevel;
}
