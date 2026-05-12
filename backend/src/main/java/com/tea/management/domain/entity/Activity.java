package com.tea.management.domain.entity;

import com.tea.management.domain.enums.ActivityStatus;
import com.tea.management.domain.enums.RoleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
/**
 * Activity 实体类，对应系统中的一类持久化业务数据。
 */
@Getter
@Setter
@Entity
@Table(name = "activity")
public class Activity extends BaseEntity {

    @Column(name = "creator_user_id", nullable = false)
    private Long creatorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "creator_role", nullable = false, length = 20)
    private RoleType creatorRole;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityStatus status;

    @Column(name = "summary_content", columnDefinition = "text")
    private String summaryContent;

    @Column(name = "summary_submitted_at")
    private LocalDateTime summarySubmittedAt;
}
