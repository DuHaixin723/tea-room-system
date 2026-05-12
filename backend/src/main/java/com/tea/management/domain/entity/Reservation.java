package com.tea.management.domain.entity;

import com.tea.management.domain.enums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
/**
 * Reservation 实体类，对应系统中的一类持久化业务数据。
 */
@Getter
@Setter
@Entity
@Table(name = "reservation")
public class Reservation extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tea_room_id", nullable = false)
    private Long teaRoomId;

    @Column(name = "reserved_start_time", nullable = false)
    private LocalDateTime reservedStartTime;

    @Column(name = "reserved_end_time", nullable = false)
    private LocalDateTime reservedEndTime;

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReservationStatus status;

    @Column(length = 255)
    private String remark;

    @Column(name = "no_show_reason", length = 255)
    private String noShowReason;

    @Column(name = "no_show_order_no", length = 64)
    private String noShowOrderNo;
}
