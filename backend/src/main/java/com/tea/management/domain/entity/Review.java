package com.tea.management.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
/**
 * Review 实体类，对应系统中的一类持久化业务数据。
 */
@Getter
@Setter
@Entity
@Table(name = "review")
public class Review extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tea_room_id", nullable = false)
    private Long teaRoomId;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "text")
    private String content;
}
