package com.tea.management.service;

import com.tea.management.domain.entity.MemberAccount;
import com.tea.management.domain.entity.User;
import com.tea.management.domain.enums.RoleType;
import com.tea.management.domain.enums.StaffApprovalStatus;
import com.tea.management.dto.request.LoginRequest;
import com.tea.management.dto.request.RegisterRequest;
import com.tea.management.dto.response.LoginResponse;
import com.tea.management.exception.BusinessException;
import com.tea.management.repository.MemberAccountRepository;
import com.tea.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
/**
 * AuthService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final NotificationService notificationService;

    /**
     * 创建新账号后，按普通用户或茶室员申请人分别执行后续处理。
     */
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("用户名已存在");
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new BusinessException("手机号已存在");
        }

        RoleType role = parseRegisterRole(request.role());

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        user.setRole(role);
        applyRegistrationState(user, request, role);
        User saved = userRepository.save(user);

        if (saved.getRole() == RoleType.USER) {
            MemberAccount account = new MemberAccount();
            account.setUserId(saved.getId());
            account.setBalance(BigDecimal.ZERO);
            account.setCumulativeRecharge(BigDecimal.ZERO);
            account.setCumulativeSpend(BigDecimal.ZERO);
            account.setMemberLevel("NORMAL");
            memberAccountRepository.save(account);
        } else if (saved.getRole() == RoleType.STAFF) {
            notificationService.notifyAdmins(
                    "STAFF_APPROVAL_PENDING",
                    "有新的茶室员待审核",
                    "茶室员账号“" + saved.getNickname() + "”已提交审核资料，请尽快处理。",
                    "USER",
                    saved.getId(),
                    "/users"
            );
        }
        return saved;
    }

    /**
     * 校验账号密码与账户状态，成功后返回登录令牌信息。
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        validateLoginStatus(user);
        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getRole(),
                jwtTokenService.generate(user)
        );
    }

    /**
     * 把传入角色字符串转换为系统角色，并阻止自行注册管理员账号。
     */
    private RoleType parseRegisterRole(String roleValue) {
        try {
            RoleType role = RoleType.valueOf(roleValue.toUpperCase());
            if (role == RoleType.ADMIN) {
                throw new BusinessException("不支持自助注册管理员账号");
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("角色类型不正确");
        }
    }

    /**
     * 当注册人为茶室员时，补齐该角色专用的审核字段。
     */
    private void applyRegistrationState(User user, RegisterRequest request, RoleType role) {
        if (role == RoleType.STAFF) {
            String staffRealName = normalizeNullableText(request.staffRealName());
            String staffIdCardNo = normalizeNullableText(request.staffIdCardNo());
            List<String> certificationImages = sanitizeCertificationImages(request.staffCertificationImages());

            if (staffRealName == null || staffIdCardNo == null || certificationImages.isEmpty()) {
                throw new BusinessException("茶室员注册需提交真实姓名、身份证号和审核资料");
            }
            if (staffRealName.length() < 2 || staffRealName.length() > 50) {
                throw new BusinessException("真实姓名长度必须在2和50之间");
            }
            if (staffIdCardNo.length() < 6 || staffIdCardNo.length() > 30) {
                throw new BusinessException("身份证号长度必须在6和30之间");
            }

            user.setStaffRealName(staffRealName);
            user.setStaffIdCardNo(staffIdCardNo);
            user.setStaffCertificationUrl(certificationImages.get(0));
            user.setStaffCertificationImages(String.join("\n", certificationImages));
            user.setStaffApplicationNote(normalizeNullableText(request.staffApplicationNote()));
            user.setStaffApprovalStatus(StaffApprovalStatus.PENDING);
            user.setStaffApprovalRemark("待管理员审核");
            user.setEnabled(false);
            return;
        }

        user.setEnabled(true);
        user.setStaffCertificationUrl(null);
        user.setStaffCertificationImages(null);
        user.setStaffApprovalStatus(null);
        user.setStaffApprovalRemark(null);
    }

    private List<String> sanitizeCertificationImages(List<String> images) {
        if (images == null) return List.of();
        return images.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String normalizeNullableText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 阻止被禁用账号或仍在审核中的茶室员账号登录。
     */
    private void validateLoginStatus(User user) {
        if (user.getRole() == RoleType.STAFF) {
            if (user.getStaffApprovalStatus() == StaffApprovalStatus.PENDING) {
                throw new BusinessException("茶室员账号正在审核中，请等待管理员处理");
            }
            if (user.getStaffApprovalStatus() == StaffApprovalStatus.REJECTED) {
                String remark = StringUtils.hasText(user.getStaffApprovalRemark()) ? "，原因：" + user.getStaffApprovalRemark() : "";
                throw new BusinessException("茶室员账号审核未通过" + remark);
            }
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException("账号已禁用");
        }
    }
}
