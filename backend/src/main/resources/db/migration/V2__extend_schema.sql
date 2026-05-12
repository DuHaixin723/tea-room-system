-- Extra tables and default system configuration.

CREATE TABLE system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO system_config (config_key, config_value, description)
VALUES
('site_name', '茶室平台', '平台名称'),
('customer_service_phone', '400-000-0000', '客服电话'),
('membership_recharge_enabled', 'true', '是否开启会员充值');

