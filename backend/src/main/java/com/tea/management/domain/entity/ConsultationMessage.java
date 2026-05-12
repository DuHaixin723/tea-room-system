package com.tea.management.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
/**
 * ConsultationMessage 实体类，对应系统中的一类持久化业务数据。
 */
@Getter
@Setter
@Entity
@Table(name = "consultation_message")
public class ConsultationMessage extends BaseEntity {

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "notify_admin", nullable = false)
    private Boolean mentionAdmin = false;

    @Column(name = "notify_staff", nullable = false)
    private Boolean mentionStaff = false;

    @Column(columnDefinition = "text", nullable = false)
    private String content;
}
