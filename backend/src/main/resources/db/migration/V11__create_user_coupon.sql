CREATE TABLE user_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    coupon_code VARCHAR(64) NOT NULL UNIQUE,
    threshold_amount DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL,
    source_level VARCHAR(30) NOT NULL,
    issued_week VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    valid_from DATETIME NOT NULL,
    valid_until DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_coupon_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
);

CREATE INDEX idx_user_coupon_user_id ON user_coupon (user_id);
CREATE INDEX idx_user_coupon_week_level ON user_coupon (issued_week, source_level);
