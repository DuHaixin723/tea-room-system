INSERT INTO system_config (config_key, config_value, description)
SELECT 'membership_recharge_bonus_rules',
       '[{"minAmount":100,"bonusAmount":10},{"minAmount":300,"bonusAmount":40},{"minAmount":500,"bonusAmount":80}]',
       '会员充值优惠规则，JSON 数组'
WHERE NOT EXISTS (
    SELECT 1 FROM system_config WHERE config_key = 'membership_recharge_bonus_rules'
);
