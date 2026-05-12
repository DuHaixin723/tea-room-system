package com.tea.management.service;

import com.tea.management.domain.entity.MemberAccount;
import com.tea.management.domain.entity.UserCoupon;
import com.tea.management.domain.enums.CouponStatus;
import com.tea.management.repository.MemberAccountRepository;
import com.tea.management.repository.NotificationRepository;
import com.tea.management.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
/**
 * UserCouponService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponService {

    private static final Set<String> SUPPORTED_LEVELS = Set.of("NORMAL", "SILVER", "GOLD", "PLATINUM", "DIAMOND");
    private static final String TYPE_COUPON_EXPIRED = "COUPON_EXPIRED";
    private static final String TYPE_COUPON_EXPIRING_SOON = "COUPON_EXPIRING_SOON";
    private static final DateTimeFormatter MANUAL_BATCH_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private final UserCouponRepository userCouponRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public Page<UserCoupon> listByUser(Long userId, Pageable pageable) {
        expireOverdueCoupons();
        return userCouponRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<UserCoupon> listAll(Pageable pageable) {
        expireOverdueCoupons();
        return userCouponRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public int dispatchWeeklyCoupons() {
        expireOverdueCoupons();
        List<MemberAccount> accounts = memberAccountRepository.findAll();
        int count = 0;
        for (MemberAccount account : accounts) {
            count += issueWeeklyCouponForAccount(account);
        }
        return count;
    }

    @Transactional
    public int dispatchCouponsByLevel(String rawLevel) {
        String level = normalizeLevel(rawLevel);
        CouponTemplate template = resolveTemplate(level);
        List<MemberAccount> accounts = memberAccountRepository.findByMemberLevel(level);
        if (accounts.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime validUntil = now.plusDays(template.validDays());
        String batchCode = "M" + now.format(MANUAL_BATCH_FORMATTER) + level.charAt(0);
        List<UserCoupon> coupons = new ArrayList<>();

        for (MemberAccount account : accounts) {
            UserCoupon coupon = new UserCoupon();
            coupon.setUserId(account.getUserId());
            coupon.setTitle(template.manualTitle());
            coupon.setCouponCode("CPN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
            coupon.setThresholdAmount(template.thresholdAmount());
            coupon.setDiscountAmount(template.discountAmount());
            coupon.setSourceLevel(level);
            coupon.setIssuedWeek(batchCode);
            coupon.setStatus(CouponStatus.UNUSED);
            coupon.setValidFrom(now);
            coupon.setValidUntil(validUntil);
            coupons.add(coupon);
        }

        userCouponRepository.saveAll(coupons);
        return coupons.size();
    }

    @Scheduled(cron = "0 0 9 ? * MON", zone = "Asia/Shanghai")
    public void weeklyDispatchJob() {
        int count = dispatchWeeklyCoupons();
        log.info("weekly member coupons dispatched: {}", count);
    }

    @Scheduled(cron = "0 0 * * * ?", zone = "Asia/Shanghai")
    public void couponReminderJob() {
        remindCouponsExpiringSoon();
        expireOverdueCoupons();
    }

    @Transactional
    public void expireOverdueCoupons() {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> overdueCoupons = userCouponRepository.findByStatusAndValidUntilBefore(CouponStatus.UNUSED, now);
        for (UserCoupon coupon : overdueCoupons) {
            coupon.setStatus(CouponStatus.EXPIRED);
            notifyExpired(coupon);
        }
        if (!overdueCoupons.isEmpty()) {
            userCouponRepository.saveAll(overdueCoupons);
        }
    }

    private int issueWeeklyCouponForAccount(MemberAccount account) {
        CouponTemplate template = resolveTemplate(account.getMemberLevel());
        String issuedWeek = currentWeekCode();
        if (userCouponRepository.existsByUserIdAndIssuedWeekAndSourceLevel(account.getUserId(), issuedWeek, account.getMemberLevel())) {
            return 0;
        }

        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        UserCoupon coupon = new UserCoupon();
        coupon.setUserId(account.getUserId());
        coupon.setTitle(template.weeklyTitle());
        coupon.setCouponCode("CPN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        coupon.setThresholdAmount(template.thresholdAmount());
        coupon.setDiscountAmount(template.discountAmount());
        coupon.setSourceLevel(account.getMemberLevel());
        coupon.setIssuedWeek(issuedWeek);
        coupon.setStatus(CouponStatus.UNUSED);
        coupon.setValidFrom(LocalDateTime.of(monday, LocalTime.MIN));
        coupon.setValidUntil(LocalDateTime.of(sunday, LocalTime.MAX));
        userCouponRepository.save(coupon);
        return 1;
    }

    private String currentWeekCode() {
        LocalDate now = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.CHINA);
        int week = now.get(weekFields.weekOfWeekBasedYear());
        int year = now.get(weekFields.weekBasedYear());
        return year + "-W" + String.format("%02d", week);
    }

    private void remindCouponsExpiringSoon() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(24);
        List<UserCoupon> coupons = userCouponRepository.findByStatusAndValidUntilBetween(CouponStatus.UNUSED, now, deadline);
        for (UserCoupon coupon : coupons) {
            if (notificationRepository.existsByRecipientUserIdAndTypeAndTargetId(coupon.getUserId(), TYPE_COUPON_EXPIRING_SOON, coupon.getId())) {
                continue;
            }
            notificationService.create(
                    coupon.getUserId(),
                    TYPE_COUPON_EXPIRING_SOON,
                    "优惠券即将到期",
                    "您有一张“" + coupon.getTitle() + "”将在 " + coupon.getValidUntil() + " 到期，请尽快使用。",
                    "COUPON",
                    coupon.getId(),
                    "/recharge"
            );
        }
    }

    private void notifyExpired(UserCoupon coupon) {
        if (notificationRepository.existsByRecipientUserIdAndTypeAndTargetId(coupon.getUserId(), TYPE_COUPON_EXPIRED, coupon.getId())) {
            return;
        }
        notificationService.create(
                coupon.getUserId(),
                TYPE_COUPON_EXPIRED,
                "优惠券已过期",
                "您的“" + coupon.getTitle() + "”已过期，请前往充值页面查看最新可用优惠券。",
                "COUPON",
                coupon.getId(),
                "/recharge"
        );
    }

    private String normalizeLevel(String rawLevel) {
        String level = rawLevel == null ? "" : rawLevel.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_LEVELS.contains(level)) {
            throw new IllegalArgumentException("不支持的会员等级: " + rawLevel);
        }
        return level;
    }

    private CouponTemplate resolveTemplate(String level) {
        return switch (level) {
            case "DIAMOND" -> new CouponTemplate("钻石会员", new BigDecimal("500"), new BigDecimal("88"), 30);
            case "PLATINUM" -> new CouponTemplate("铂金会员", new BigDecimal("300"), new BigDecimal("50"), 20);
            case "GOLD" -> new CouponTemplate("黄金会员", new BigDecimal("200"), new BigDecimal("30"), 15);
            case "SILVER" -> new CouponTemplate("白银会员", new BigDecimal("120"), new BigDecimal("15"), 10);
            default -> new CouponTemplate("普通会员", new BigDecimal("99"), new BigDecimal("10"), 7);
        };
    }

    private record CouponTemplate(String levelName, BigDecimal thresholdAmount, BigDecimal discountAmount, int validDays) {
        private String weeklyTitle() {
            return "本周" + levelName + "专享券";
        }

        private String manualTitle() {
            return levelName + "充值优惠券";
        }
    }
}
