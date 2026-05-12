package com.tea.management.domain.entity;

import com.tea.management.domain.enums.ReportStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
/**
 * EquipmentReport 实体类，对应系统中的一类持久化业务数据。
 */
@Getter
@Setter
@Entity
@Table(name = "equipment_report")
public class EquipmentReport extends BaseEntity {

    @Column(name = "tea_room_id", nullable = false)
    private Long teaRoomId;

    @Column(name = "reported_by", nullable = false)
    private Long reportedBy;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;
}
