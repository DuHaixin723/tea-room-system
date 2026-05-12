package com.tea.management.service;

import com.tea.management.domain.entity.MemberAccount;
import com.tea.management.domain.entity.MemberRechargeRecord;
import com.tea.management.domain.entity.User;
import com.tea.management.domain.entity.UserCoupon;
import com.tea.management.domain.enums.CouponStatus;
import com.tea.management.domain.enums.RoleType;
import com.tea.management.domain.enums.StaffApprovalStatus;
import com.tea.management.dto.request.ChangePasswordRequest;
import com.tea.management.dto.request.RechargeRequest;
import com.tea.management.dto.request.StaffApprovalRequest;
import com.tea.management.dto.request.UserUpdateRequest;
import com.tea.management.dto.response.RechargeBonusTierResponse;
import com.tea.management.exception.BusinessException;
import com.tea.management.exception.ResourceNotFoundException;
import com.tea.management.repository.MemberAccountRepository;
import com.tea.management.repository.MemberRechargeRecordRepository;
import com.tea.management.repository.UserCouponRepository;
import com.tea.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
/**
 * UserService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final MemberRechargeRecordRepository memberRechargeRecordRepository;
    private final UserCouponRepository userCouponRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigService systemConfigService;
    private final MemberLevelService memberLevelService;
    private final NotificationService notificationService;

    public Page<User> listAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Page<User> listByRole(RoleType roleType, Pageable pageable) {
        return userRepository.findByRole(roleType, pageable);
    }

    public User update(Long id, UserUpdateRequest request) {
        User user = getUser(id);
        if (request.nickname() != null) user.setNickname(request.nickname());
        if (request.phone() != null) user.setPhone(request.phone());
        if (request.email() != null) user.setEmail(request.email());
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        if (request.enabled() != null) user.setEnabled(request.enabled());
        return userRepository.save(user);
    }

    public User reviewStaffApproval(Long id, StaffApprovalRequest request) {
        User user = getUser(id);
        if (user.getRole() != RoleType.STAFF) {
            throw new BusinessException("Only staff accounts require approval");
        }

        StaffApprovalStatus status;
        try {
            status = StaffApprovalStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Invalid approval status");
        }

        if (status == StaffApprovalStatus.PENDING) {
            throw new BusinessException("Approval result cannot remain pending");
        }
        if (status == StaffApprovalStatus.REJECTED && !StringUtils.hasText(request.remark())) {
            throw new BusinessException("A rejection remark is required");
        }

        user.setStaffApprovalStatus(status);
        user.setStaffApprovalRemark(StringUtils.hasText(request.remark()) ? request.remark().trim() : null);
        user.setEnabled(status == StaffApprovalStatus.APPROVED);
        User saved = userRepository.save(user);
        notificationService.create(
                saved.getId(),
                status == StaffApprovalStatus.APPROVED ? "STAFF_APPROVAL_APPROVED" : "STAFF_APPROVAL_REJECTED",
                status == StaffApprovalStatus.APPROVED ? "Staff approval granted" : "Staff approval rejected",
                status == StaffApprovalStatus.APPROVED
                        ? "Your staff account has been approved."
                        : "Your staff account was rejected: " + saved.getStaffApprovalRemark(),
                "USER",
                saved.getId(),
                "/profile"
        );
        return saved;
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public MemberAccount recharge(Long userId, RechargeRequest request) {
        if (!systemConfigService.isRechargeEnabled()) {
            throw new BusinessException("Membership recharge is disabled");
        }

        UserCoupon coupon = resolveRechargeCoupon(userId, request);
        BigDecimal couponDiscount = coupon == null ? BigDecimal.ZERO : coupon.getDiscountAmount();
        BigDecimal actualPayment = request.amount().subtract(couponDiscount).max(BigDecimal.ZERO);
        RechargeBonusTierResponse matchedTier = systemConfigService.matchRechargeBonusTier(request.amount());
        BigDecimal bonusAmount = matchedTier == null ? BigDecimal.ZERO : matchedTier.bonusAmount();
        BigDecimal creditedAmount = request.amount().add(bonusAmount);

        MemberAccount account = getAccount(userId);
        account.setBalance(account.getBalance().add(creditedAmount));
        account.setCumulativeRecharge(account.getCumulativeRecharge().add(request.amount()));
        account.setMemberLevel(memberLevelService.resolveLevel(account.getCumulativeRecharge(), account.getCumulativeSpend()));
        MemberAccount saved = memberAccountRepository.save(account);

        MemberRechargeRecord record = new MemberRechargeRecord();
        record.setUserId(userId);
        record.setAmount(actualPayment);
        record.setBalanceAfter(saved.getBalance());
        record.setRemark(buildRechargeRemark(request.amount(), actualPayment, coupon, bonusAmount, creditedAmount));
        memberRechargeRecordRepository.save(record);

        if (coupon != null) {
            coupon.setStatus(CouponStatus.USED);
            userCouponRepository.save(coupon);
        }

        notificationService.create(
                userId,
                "RECHARGE_SUCCESS",
                "充值到账",
                buildRechargeNotificationContent(request.amount(), actualPayment, couponDiscount, bonusAmount, saved.getBalance()),
                "ACCOUNT",
                saved.getId(),
                "/recharge"
        );

        return saved;
    }

    public List<MemberRechargeRecord> getRechargeRecords(Long userId) {
        getAccount(userId);
        return memberRechargeRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public MemberAccount getAccount(Long userId) {
        return memberAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member account not found"));
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User updateProfile(Long id, UserUpdateRequest request) {
        return update(id, request);
    }

    public User updateAvatar(Long id, String avatarUrl) {
        User user = getUser(id);
        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }

    public void changePassword(Long id, ChangePasswordRequest request) {
        User user = getUser(id);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BusinessException("Incorrect current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private UserCoupon resolveRechargeCoupon(Long userId, RechargeRequest request) {
        if (request.couponId() == null) {
            return null;
        }

        UserCoupon coupon = userCouponRepository.findByIdAndUserId(request.couponId(), userId)
                .orElseThrow(() -> new BusinessException("Coupon not found for current user"));

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStatus() == CouponStatus.EXPIRED || coupon.getValidUntil().isBefore(now)) {
            coupon.setStatus(CouponStatus.EXPIRED);
            userCouponRepository.save(coupon);
            throw new BusinessException("Coupon expired");
        }
        if (coupon.getStatus() == CouponStatus.USED) {
            throw new BusinessException("Coupon already used");
        }
        if (coupon.getValidFrom().isAfter(now)) {
            throw new BusinessException("Coupon not active yet");
        }
        if (request.amount().compareTo(coupon.getThresholdAmount()) < 0) {
            throw new BusinessException("Recharge amount does not meet coupon threshold");
        }
        return coupon;
    }

    private String buildRechargeRemark(BigDecimal rechargeAmount,
                                       BigDecimal actualPayment,
                                       UserCoupon coupon,
                                       BigDecimal bonusAmount,
                                       BigDecimal creditedAmount) {
        StringBuilder remark = new StringBuilder("会员充值，充值金额 ")
                .append(rechargeAmount)
                .append(" 元，实付 ")
                .append(actualPayment);
        if (coupon != null) {
            remark.append(" 元，使用优惠券【")
                    .append(coupon.getTitle())
                    .append("】抵扣 ")
                    .append(coupon.getDiscountAmount());
        }
        if (bonusAmount.compareTo(BigDecimal.ZERO) > 0) {
            remark.append(" 元，赠送 ").append(bonusAmount);
        }
        remark.append(" 元，到账 ").append(creditedAmount).append(" 元");
        return remark.toString();
    }

    private String buildRechargeNotificationContent(BigDecimal rechargeAmount,
                                                    BigDecimal actualPayment,
                                                    BigDecimal couponDiscount,
                                                    BigDecimal bonusAmount,
                                                    BigDecimal balance) {
        StringBuilder content = new StringBuilder("你的账户已成功充值 ")
                .append(rechargeAmount)
                .append(" 元，实付 ")
                .append(actualPayment);
        if (couponDiscount.compareTo(BigDecimal.ZERO) > 0) {
            content.append(" 元，优惠券抵扣 ").append(couponDiscount);
        }
        if (bonusAmount.compareTo(BigDecimal.ZERO) > 0) {
            content.append(" 元，赠送 ").append(bonusAmount);
        }
        content.append(" 元，当前余额为 ").append(balance).append(" 元。");
        return content.toString();
    }
}
