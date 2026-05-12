package com.tea.management.domain.entity;

import com.tea.management.domain.enums.RoleType;
import com.tea.management.domain.enums.StaffApprovalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
/**
 * User 实体类，对应系统中的一类持久化业务数据。
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "staff_real_name", length = 50)
    private String staffRealName;

    @Column(name = "staff_id_card_no", length = 30)
    private String staffIdCardNo;

    @Column(name = "staff_certification_url", length = 255)
    private String staffCertificationUrl;

    @Column(name = "staff_certification_images", columnDefinition = "TEXT")
    private String staffCertificationImages;

    @Column(name = "staff_application_note", length = 500)
    private String staffApplicationNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_approval_status", length = 20)
    private StaffApprovalStatus staffApprovalStatus;

    @Column(name = "staff_approval_remark", length = 255)
    private String staffApprovalRemark;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleType role;

    @Column(nullable = false)
    private Boolean enabled;
}
