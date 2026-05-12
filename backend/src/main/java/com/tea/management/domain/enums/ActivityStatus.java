package com.tea.management.domain.enums;
/**
 * ActivityStatus 枚举类，用来约束系统中的固定状态或固定类型。
 */
public enum ActivityStatus {
    PENDING_REVIEW,
    REJECTED,
    DRAFT,
    PUBLISHED,
    CLOSED,
    CANCELLED
}
