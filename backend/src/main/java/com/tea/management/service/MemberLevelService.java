package com.tea.management.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
/**
 * MemberLevelService 服务层，负责封装核心业务规则、状态流转与数据处理。
 */
@Service
public class MemberLevelService {

    private static final List<LevelRule> RULES = List.of(
            new LevelRule("NORMAL", BigDecimal.ZERO, BigDecimal.ZERO),
            new LevelRule("SILVER", new BigDecimal("300"), new BigDecimal("500")),
            new LevelRule("GOLD", new BigDecimal("1000"), new BigDecimal("2000")),
            new LevelRule("PLATINUM", new BigDecimal("3000"), new BigDecimal("5000")),
            new LevelRule("DIAMOND", new BigDecimal("8000"), new BigDecimal("12000"))
    );

    public String resolveLevel(BigDecimal cumulativeRecharge, BigDecimal cumulativeSpend) {
        String currentLevel = "NORMAL";
        for (LevelRule rule : RULES) {
            if (cumulativeRecharge.compareTo(rule.rechargeThreshold()) >= 0
                    || cumulativeSpend.compareTo(rule.spendThreshold()) >= 0) {
                currentLevel = rule.level();
            }
        }
        return currentLevel;
    }

    private record LevelRule(String level, BigDecimal rechargeThreshold, BigDecimal spendThreshold) {
    }
}
