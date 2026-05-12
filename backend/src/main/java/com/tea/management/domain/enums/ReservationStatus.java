package com.tea.management.domain.enums;
/**
 * ReservationStatus 枚举类，用来约束系统中的固定状态或固定类型。
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    USER_CHECKED_IN,
    STAFF_CONFIRMED_CHECK_IN,
    CHARGING,
    NO_SHOW,
    CANCELLED,
    COMPLETED
}
