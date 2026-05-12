package com.tea.management.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
/**
 * MemberRechargeRecord 实体类，对应系统中的一类持久化业务数据。
 */
@Getter
@Setter
@Entity
@Table(name = "member_recharge_record")
public class MemberRechargeRecord extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "operator_user_id")
    private Long operatorUserId;

    @Column(name = "remark", length = 255)
    private String remark;
}
