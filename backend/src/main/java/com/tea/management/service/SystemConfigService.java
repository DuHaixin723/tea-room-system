package com.tea.management.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tea.management.domain.entity.SystemConfig;
import com.tea.management.dto.request.SystemConfigRequest;
import com.tea.management.dto.response.BasicSystemSettingsResponse;
import com.tea.management.dto.response.RechargeBonusTierResponse;
import com.tea.management.dto.response.RechargePolicyResponse;
import com.tea.management.exception.BusinessException;
import com.tea.management.exception.ResourceNotFoundException;
import com.tea.management.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
/**
 * SystemConfigService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    public static final String SITE_NAME_KEY = "site_name";
    public static final String CUSTOMER_SERVICE_PHONE_KEY = "customer_service_phone";
    public static final String MEMBERSHIP_RECHARGE_ENABLED_KEY = "membership_recharge_enabled";
    public static final String MEMBERSHIP_RECHARGE_BONUS_RULES_KEY = "membership_recharge_bonus_rules";

    private final SystemConfigRepository systemConfigRepository;
    private final ObjectMapper objectMapper;

    public Page<SystemConfig> list(Pageable pageable) {
        return systemConfigRepository.findAll(pageable);
    }

    public SystemConfig save(SystemConfigRequest request) {
        SystemConfig config = systemConfigRepository.findByConfigKey(request.configKey()).orElseGet(SystemConfig::new);
        config.setConfigKey(request.configKey());
        config.setConfigValue(request.configValue());
        config.setDescription(request.description());
        return systemConfigRepository.save(config);
    }

    public void delete(Long id) {
        if (!systemConfigRepository.existsById(id)) {
            throw new ResourceNotFoundException("系统配置不存在");
        }
        systemConfigRepository.deleteById(id);
    }

    public RechargePolicyResponse getRechargePolicy() {
        boolean enabled = Boolean.parseBoolean(getConfigValue(MEMBERSHIP_RECHARGE_ENABLED_KEY).orElse("true"));
        return new RechargePolicyResponse(enabled, getRechargeBonusTiers());
    }

    public BasicSystemSettingsResponse getBasicSettings() {
        return new BasicSystemSettingsResponse(
                getConfigValue(SITE_NAME_KEY).orElse("茶室平台"),
                getConfigValue(CUSTOMER_SERVICE_PHONE_KEY).orElse("400-000-0000")
        );
    }

    public boolean isRechargeEnabled() {
        return getRechargePolicy().enabled();
    }

    public RechargeBonusTierResponse matchRechargeBonusTier(BigDecimal amount) {
        return getRechargeBonusTiers().stream()
                .filter(item -> amount.compareTo(item.minAmount()) >= 0)
                .max(Comparator.comparing(RechargeBonusTierResponse::minAmount))
                .orElse(null);
    }

    public List<RechargeBonusTierResponse> getRechargeBonusTiers() {
        String raw = getConfigValue(MEMBERSHIP_RECHARGE_BONUS_RULES_KEY).orElse("[]");
        try {
            List<RechargeBonusTierResponse> tiers = objectMapper.readValue(raw, new TypeReference<>() {});
            return tiers.stream()
                    .filter(item -> item != null && item.minAmount() != null && item.bonusAmount() != null)
                    .filter(item -> item.minAmount().compareTo(BigDecimal.ZERO) > 0 && item.bonusAmount().compareTo(BigDecimal.ZERO) >= 0)
                    .sorted(Comparator.comparing(RechargeBonusTierResponse::minAmount))
                    .toList();
        } catch (IOException exception) {
            throw new BusinessException("会员充值优惠配置格式错误");
        }
    }

    private Optional<String> getConfigValue(String key) {
        return systemConfigRepository.findByConfigKey(key).map(SystemConfig::getConfigValue);
    }
}
