package com.tea.management.domain.enums;
/**
 * OrderStatus 枚举类，用来约束系统中的固定状态或固定类型。
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    NO_SHOW,
    COMPLETED,
    CANCELLED,
    REFUNDED
}
