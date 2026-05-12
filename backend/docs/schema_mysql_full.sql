CREATE DATABASE IF NOT EXISTS tea_room_management
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE tea_room_management;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100),
    role VARCHAR(20) NOT NULL,
    enabled BIT NOT NULL DEFAULT b'1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE member_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    cumulative_recharge DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    cumulative_spend DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    member_level VARCHAR(30) NOT NULL DEFAULT 'NORMAL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_account_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
);

CREATE TABLE tea_room_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    base_price DECIMAL(10, 2) NOT NULL,
    min_capacity INT NOT NULL DEFAULT 1,
    max_capacity INT NOT NULL DEFAULT 99,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE tea_room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_id BIGINT NOT NULL,
    staff_user_id BIGINT,
    name VARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    enabled BIT NOT NULL DEFAULT b'1',
    location VARCHAR(255),
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_tea_room_type FOREIGN KEY (type_id) REFERENCES tea_room_type (id),
    CONSTRAINT fk_tea_room_staff FOREIGN KEY (staff_user_id) REFERENCES sys_user (id)
);

CREATE TABLE tea (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL,
    description TEXT,
    enabled BIT NOT NULL DEFAULT b'1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE reservation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tea_room_id BIGINT NOT NULL,
    reserved_start_time DATETIME NOT NULL,
    reserved_end_time DATETIME NOT NULL,
    party_size INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_reservation_room FOREIGN KEY (tea_room_id) REFERENCES tea_room (id)
);

CREATE TABLE review (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tea_room_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_review_room FOREIGN KEY (tea_room_id) REFERENCES tea_room (id)
);

CREATE TABLE equipment_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tea_room_id BIGINT NOT NULL,
    reported_by BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_equipment_report_room FOREIGN KEY (tea_room_id) REFERENCES tea_room (id),
    CONSTRAINT fk_equipment_report_user FOREIGN KEY (reported_by) REFERENCES sys_user (id)
);

CREATE TABLE activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content TEXT,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    capacity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE activity_registration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    cancelled BIT NOT NULL DEFAULT b'0',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_registration_activity FOREIGN KEY (activity_id) REFERENCES activity (id),
    CONSTRAINT fk_activity_registration_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
);

CREATE TABLE product_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    reservation_id BIGINT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_order_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_product_order_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id)
);

CREATE INDEX idx_product_order_reservation_id ON product_order (reservation_id);

CREATE TABLE order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    tea_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES product_order (id),
    CONSTRAINT fk_order_item_tea FOREIGN KEY (tea_id) REFERENCES tea (id)
);

CREATE TABLE favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
);

CREATE TABLE consultation_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tea_room_id BIGINT NULL,
    admin_user_id BIGINT,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_consultation_session_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_consultation_session_room FOREIGN KEY (tea_room_id) REFERENCES tea_room (id),
    CONSTRAINT fk_consultation_session_admin FOREIGN KEY (admin_user_id) REFERENCES sys_user (id)
);

CREATE TABLE consultation_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    notify_admin BIT NOT NULL DEFAULT b'0',
    notify_staff BIT NOT NULL DEFAULT b'0',
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_consultation_message_session FOREIGN KEY (session_id) REFERENCES consultation_session (id),
    CONSTRAINT fk_consultation_message_sender FOREIGN KEY (sender_id) REFERENCES sys_user (id)
);

CREATE TABLE staff_complaint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tea_room_id BIGINT NOT NULL,
    staff_user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_complaint_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_staff_complaint_room FOREIGN KEY (tea_room_id) REFERENCES tea_room (id),
    CONSTRAINT fk_staff_complaint_staff FOREIGN KEY (staff_user_id) REFERENCES sys_user (id)
);

CREATE TABLE member_recharge_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    balance_after DECIMAL(10, 2) NOT NULL,
    operator_user_id BIGINT,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_recharge_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_member_recharge_operator FOREIGN KEY (operator_user_id) REFERENCES sys_user (id)
);

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

CREATE TABLE system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO system_config (config_key, config_value, description) VALUES
('site_name', '茶室平台', '平台名称'),
('customer_service_phone', '400-000-0000', '客服电话'),
('membership_recharge_enabled', 'true', '是否开启会员充值'),
('membership_recharge_bonus_rules', '[{"minAmount":100,"bonusAmount":10},{"minAmount":300,"bonusAmount":40},{"minAmount":500,"bonusAmount":80}]', '会员充值赠送规则，JSON 数组');
