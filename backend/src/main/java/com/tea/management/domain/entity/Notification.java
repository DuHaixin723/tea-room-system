package com.tea.management.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
/**
 * Notification 实体类，对应系统中的一类持久化业务数据。
 */
@Getter
@Setter
@Entity
@Table(name = "notification")
public class Notification extends BaseEntity {

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(name = "target_type", length = 50)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "route_path", length = 255)
    private String routePath;

    @Column(name = "is_read", nullable = false)
    private Boolean read = false;
}
