package com.tea.management.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
/**
 * TeaRoom 实体类，对应系统中的一类持久化业务数据。
 */
@Getter
@Setter
@Entity
@Table(name = "tea_room")
public class TeaRoom extends BaseEntity {

    @Column(name = "type_id", nullable = false)
    private Long typeId;

    @Column(name = "staff_user_id")
    private Long staffUserId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(length = 255)
    private String location;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "business_start_time", nullable = false)
    private LocalTime businessStartTime;

    @Column(name = "business_end_time", nullable = false)
    private LocalTime businessEndTime;

    @Column(columnDefinition = "text")
    private String description;
}
