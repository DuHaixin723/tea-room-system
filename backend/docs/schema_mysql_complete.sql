-- Unified MySQL initialization script for the tea room management project.
-- Covers final table structure, default config, and demo seed data.

DROP DATABASE IF EXISTS tea_room_management;
CREATE DATABASE tea_room_management
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE tea_room_management;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100),
    avatar_url VARCHAR(255),
    staff_real_name VARCHAR(50),
    staff_id_card_no VARCHAR(30),
    staff_certification_url VARCHAR(255),
    staff_certification_images TEXT,
    staff_application_note VARCHAR(500),
    staff_approval_status VARCHAR(20),
    staff_approval_remark VARCHAR(255),
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
    pricing_mode VARCHAR(20) NOT NULL DEFAULT 'PER_ROOM',
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
    image_url VARCHAR(255),
    business_start_time TIME NOT NULL DEFAULT '09:00:00',
    business_end_time TIME NOT NULL DEFAULT '22:00:00',
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
    image_url VARCHAR(255),
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
    reservation_id BIGINT UNIQUE,
    rating INT NOT NULL,
    content TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_review_room FOREIGN KEY (tea_room_id) REFERENCES tea_room (id),
    CONSTRAINT fk_review_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id)
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
    creator_user_id BIGINT NOT NULL DEFAULT 1,
    creator_role VARCHAR(20) NOT NULL DEFAULT 'ADMIN',
    title VARCHAR(100) NOT NULL,
    content TEXT,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    capacity INT NOT NULL,
    image_url VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    summary_content TEXT,
    summary_submitted_at DATETIME,
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
    reservation_id BIGINT,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20),
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
    order_id BIGINT,
    tea_room_id BIGINT,
    admin_user_id BIGINT,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_consultation_session_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_consultation_session_order FOREIGN KEY (order_id) REFERENCES product_order (id),
    CONSTRAINT fk_consultation_session_room FOREIGN KEY (tea_room_id) REFERENCES tea_room (id),
    CONSTRAINT fk_consultation_session_admin FOREIGN KEY (admin_user_id) REFERENCES sys_user (id)
);

CREATE INDEX idx_consultation_session_order_id ON consultation_session (order_id);

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

CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    target_type VARCHAR(50),
    target_id BIGINT,
    route_path VARCHAR(255),
    is_read BIT NOT NULL DEFAULT b'0',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_notification_recipient_created (recipient_user_id, created_at),
    INDEX idx_notification_recipient_read (recipient_user_id, is_read)
);

CREATE TABLE system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

SET FOREIGN_KEY_CHECKS = 1;

SET @DEMO_BCRYPT := '$2a$10$3F0M4TngaCFogHFb5hPn8O5cXUvvf8B56Hv/FGs8xzEPNo5xKV7Zm';

INSERT INTO sys_user (
    id, username, password, nickname, phone, email, avatar_url,
    staff_real_name, staff_id_card_no, staff_certification_url, staff_certification_images,
    staff_application_note, staff_approval_status, staff_approval_remark,
    role, enabled
) VALUES
(1, 'admin',  @DEMO_BCRYPT, 'Admin', '13900000001', 'admin@example.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'ADMIN', b'1'),
(2, 'staff1', @DEMO_BCRYPT, 'Staff One', '13900000002', 'staff1@example.com', NULL, 'Li Ming', '310101199001010011', 'uploads/staff/staff1-cert-1.jpg', 'uploads/staff/staff1-cert-1.jpg', 'Tea service specialist', 'APPROVED', 'Approved by admin', 'STAFF', b'1'),
(3, 'user1',  @DEMO_BCRYPT, 'User One', '13900000003', 'user1@example.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'USER', b'1'),
(4, 'staff2', @DEMO_BCRYPT, 'Staff Two', '13900000012', 'staff2@example.com', NULL, 'Wang Yue', '310101199202020022', 'uploads/staff/staff2-cert-1.jpg', 'uploads/staff/staff2-cert-1.jpg', 'Courtyard tea room attendant', 'APPROVED', 'Approved by admin', 'STAFF', b'1'),
(5, 'staff3', @DEMO_BCRYPT, 'Staff Three', '13900000013', 'staff3@example.com', NULL, 'Zhao Qing', '310101199303030033', 'uploads/staff/staff3-cert-1.jpg', 'uploads/staff/staff3-cert-1.jpg', 'Salon tea room attendant', 'APPROVED', 'Approved by admin', 'STAFF', b'1'),
(6, 'user2',  @DEMO_BCRYPT, 'User Two', '13900000022', 'user2@example.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'USER', b'1'),
(7, 'user3',  @DEMO_BCRYPT, 'User Three', '13900000023', 'user3@example.com', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'USER', b'1');

INSERT INTO member_account (user_id, balance, cumulative_recharge, cumulative_spend, member_level) VALUES
(3, 260.00, 260.00, 58.00, 'NORMAL'),
(6, 680.00, 680.00, 168.00, 'SILVER'),
(7, 1480.00, 1480.00, 520.00, 'GOLD');

INSERT INTO tea_room_type (id, name, description, base_price, pricing_mode, min_capacity, max_capacity) VALUES
(1, 'Private Small Room', 'Suitable for 2 to 4 guests', 88.00, 'PER_ROOM', 2, 4),
(2, 'Business Room', 'Suitable for meeting and tea reception', 168.00, 'PER_ROOM', 6, 10),
(3, 'Family Room', 'Suitable for family gatherings', 128.00, 'PER_ROOM', 4, 8),
(4, 'Courtyard Seats', 'Outdoor style tea experience', 198.00, 'PER_ROOM', 4, 6),
(5, 'Zen Single Room', 'Quiet single room', 108.00, 'PER_ROOM', 1, 2),
(6, 'Salon Suite', 'Suitable for group tea sharing', 238.00, 'PER_ROOM', 6, 12),
(7, 'Public Tea Room', 'Public seating charged by headcount', 58.00, 'PER_PERSON', 1, 20);

INSERT INTO tea_room (
    id, type_id, staff_user_id, name, capacity, enabled, location, image_url,
    business_start_time, business_end_time, description
) VALUES
(1, 1, 2, 'Cloud Pavilion 01', 4, b'1', 'Jingan, Shanghai', 'uploads/tea-rooms/cloud-01.jpg', '09:00:00', '22:00:00', 'Window seats for small private tea sessions'),
(2, 2, 2, 'Pine Breeze 02', 8, b'1', 'Xuhui, Shanghai', 'uploads/tea-rooms/pine-02.jpg', '09:00:00', '22:00:00', 'Business room with screen equipment'),
(3, 4, 4, 'Bamboo Shadow 03', 6, b'1', 'Xuhui, Shanghai', 'uploads/tea-rooms/bamboo-03.jpg', '09:00:00', '22:00:00', 'Courtyard atmosphere for afternoon tea'),
(4, 5, 4, 'Quiet Hill 04', 2, b'1', 'Changning, Shanghai', 'uploads/tea-rooms/hill-04.jpg', '09:00:00', '22:00:00', 'Quiet room for solo tea and reading'),
(5, 6, 5, 'Listen Rain 05', 10, b'1', 'Pudong, Shanghai', 'uploads/tea-rooms/rain-05.jpg', '09:00:00', '22:00:00', 'Salon room for events and sharing'),
(6, 4, 5, 'Moon View 06', 4, b'1', 'Huangpu, Shanghai', 'uploads/tea-rooms/moon-06.jpg', '09:00:00', '22:00:00', 'Terrace room with evening atmosphere'),
(7, 7, 2, 'Public Tea Hall', 20, b'1', 'Jingan, Shanghai', 'uploads/tea-rooms/public-07.jpg', '09:00:00', '22:00:00', 'Open public tea room charged by guest count');

INSERT INTO tea (id, name, category, price, stock, image_url, description, enabled) VALUES
(1, 'West Lake Longjing', 'GREEN', 58.00, 120, 'uploads/tea/longjing.jpg', 'Classic green tea for tasting', b'1'),
(2, 'Tieguanyin', 'OOLONG', 66.00, 80, 'uploads/tea/tgy.jpg', 'Floral oolong tea', b'1'),
(3, 'Ripe Pu-erh', 'DARK', 78.00, 60, 'uploads/tea/puer.jpg', 'Smooth aged dark tea', b'1'),
(4, 'Jasmine Tea', 'FLOWER', 39.00, 150, 'uploads/tea/jasmine.jpg', 'Popular flower tea', b'1'),
(5, 'Phoenix Dancong', 'OOLONG', 88.00, 50, 'uploads/tea/dancong.jpg', 'Layered aroma and fruity note', b'1'),
(6, 'Silver Needle', 'WHITE', 96.00, 40, 'uploads/tea/silver.jpg', 'Light and elegant white tea', b'1'),
(7, 'Lapsang Souchong', 'BLACK', 52.00, 90, 'uploads/tea/lapsang.jpg', 'Warm black tea for general users', b'1'),
(8, 'Shoumei', 'WHITE', 68.00, 70, 'uploads/tea/shoumei.jpg', 'Good for long steeping', b'1'),
(9, 'Rougui', 'OOLONG', 76.00, 65, 'uploads/tea/rougui.jpg', 'Rock tea with strong body', b'1'),
(10, 'Loose Pu-erh', 'DARK', 45.00, 110, 'uploads/tea/loose-puer.jpg', 'Daily dark tea option', b'1');

INSERT INTO activity (
    id, creator_user_id, creator_role, title, content, start_time, end_time,
    capacity, image_url, status, summary_content, summary_submitted_at
) VALUES
(1, 1, 'ADMIN', 'Spring Tasting Class', 'Introductory tea tasting activity', '2026-04-20 19:00:00', '2026-04-20 21:00:00', 30, 'uploads/activities/activity-01.jpg', 'PUBLISHED', NULL, NULL),
(2, 1, 'ADMIN', 'Weekend Oolong Salon', 'Oolong comparison and sharing session', '2026-04-23 15:00:00', '2026-04-23 17:00:00', 20, 'uploads/activities/activity-02.jpg', 'PUBLISHED', NULL, NULL),
(3, 2, 'STAFF', 'Tea Service Training', 'Internal staff service etiquette training', '2026-04-26 10:00:00', '2026-04-26 12:00:00', 12, 'uploads/activities/activity-03.jpg', 'PENDING_REVIEW', NULL, NULL),
(4, 1, 'ADMIN', 'New User Tea 101', 'Entry level tea category introduction', '2026-04-28 19:00:00', '2026-04-28 21:00:00', 24, 'uploads/activities/activity-04.jpg', 'PUBLISHED', NULL, NULL);

INSERT INTO activity_registration (activity_id, user_id, cancelled) VALUES
(1, 3, b'0'),
(2, 6, b'0'),
(4, 7, b'0');

INSERT INTO reservation (
    id, user_id, tea_room_id, reserved_start_time, reserved_end_time, party_size, status, remark
) VALUES
(1, 3, 1, '2026-04-18 14:00:00', '2026-04-18 16:00:00', 2, 'PENDING', 'Need a quiet window seat'),
(2, 3, 2, '2026-04-19 15:00:00', '2026-04-19 17:00:00', 4, 'CONFIRMED', 'Business meeting'),
(3, 3, 1, '2026-04-10 14:00:00', '2026-04-10 16:00:00', 2, 'COMPLETED', 'Completed reservation'),
(4, 6, 3, '2026-04-21 13:00:00', '2026-04-21 15:00:00', 4, 'CONFIRMED', 'Friends gathering'),
(5, 7, 7, '2026-04-22 19:00:00', '2026-04-22 21:00:00', 3, 'PENDING', 'Public tea hall experience');

INSERT INTO review (user_id, tea_room_id, reservation_id, rating, content) VALUES
(3, 1, 3, 5, 'Quiet room and good service');

INSERT INTO equipment_report (tea_room_id, reported_by, title, content, status) VALUES
(2, 2, 'Projector connection issue', 'Screen output is unstable during meetings', 'PENDING'),
(1, 2, 'Air conditioning issue', 'Cooling effect is unstable', 'PROCESSING');

INSERT INTO product_order (id, order_no, user_id, reservation_id, amount, status, payment_method) VALUES
(1, 'ORD-DEMO-0001', 3, NULL, 124.00, 'PENDING_PAYMENT', NULL),
(2, 'ORD-DEMO-0002', 3, 2, 156.00, 'PAID', 'BALANCE'),
(3, 'ORD-DEMO-0003', 3, NULL, 39.00, 'COMPLETED', 'BALANCE'),
(4, 'ORD-DEMO-0004', 6, 4, 176.00, 'PAID', 'WECHAT'),
(5, 'ORD-DEMO-0005', 7, NULL, 164.00, 'COMPLETED', 'ALIPAY');

INSERT INTO order_item (order_id, tea_id, quantity, unit_price) VALUES
(1, 1, 1, 58.00),
(1, 2, 1, 66.00),
(2, 3, 2, 78.00),
(3, 4, 1, 39.00),
(4, 5, 2, 88.00),
(5, 9, 1, 76.00),
(5, 7, 1, 52.00),
(5, 4, 1, 36.00);

INSERT INTO favorite (user_id, target_id, target_type) VALUES
(3, 1, 'TEA'),
(3, 1, 'TEA_ROOM'),
(6, 5, 'TEA'),
(7, 7, 'TEA_ROOM');

INSERT INTO consultation_session (id, user_id, order_id, tea_room_id, admin_user_id, status) VALUES
(1, 3, 2, 2, 1, 'OPEN'),
(2, 6, 4, 3, 1, 'OPEN');

INSERT INTO consultation_message (session_id, sender_id, notify_admin, notify_staff, content) VALUES
(1, 3, b'1', b'0', 'Hello, I want to ask about the reservation fee standard'),
(1, 1, b'0', b'0', 'The fee depends on tea room type and pricing mode'),
(2, 6, b'1', b'1', 'Can the courtyard room prepare tea snacks in advance?'),
(2, 1, b'0', b'0', 'Yes, the staff can prepare in advance after confirmation');

INSERT INTO staff_complaint (user_id, tea_room_id, staff_user_id, content, status) VALUES
(3, 2, 2, 'The service response was slow during the last visit', 'PENDING');

INSERT INTO member_recharge_record (user_id, amount, balance_after, operator_user_id, remark) VALUES
(3, 100.00, 100.00, 1, 'Initial recharge'),
(3, 160.00, 260.00, 1, 'Recharge for pending order'),
(6, 680.00, 680.00, 1, 'Silver member recharge'),
(7, 1480.00, 1480.00, 1, 'Gold member recharge');

INSERT INTO user_coupon (
    user_id, title, coupon_code, threshold_amount, discount_amount, source_level,
    issued_week, status, valid_from, valid_until
) VALUES
(3, 'Normal Member Recharge Coupon', 'CPN-NORMAL-001', 99.00, 10.00, 'NORMAL', '2026-W16', 'UNUSED', '2026-04-14 00:00:00', '2026-04-20 23:59:59'),
(6, 'Silver Member Recharge Coupon', 'CPN-SILVER-001', 120.00, 15.00, 'SILVER', '2026-W16', 'UNUSED', '2026-04-14 00:00:00', '2026-04-24 23:59:59'),
(7, 'Gold Member Recharge Coupon', 'CPN-GOLD-001', 200.00, 30.00, 'GOLD', '2026-W16', 'UNUSED', '2026-04-14 00:00:00', '2026-04-29 23:59:59'),
(7, 'Gold Member Manual Coupon', 'CPN-GOLD-MAN-01', 200.00, 30.00, 'GOLD', 'MANUAL-20260415100000-GOLD', 'UNUSED', '2026-04-15 10:00:00', '2026-04-30 10:00:00'),
(7, 'Expired Coupon Example', 'CPN-GOLD-EXP-01', 150.00, 20.00, 'GOLD', '2026-W15', 'EXPIRED', '2026-04-01 00:00:00', '2026-04-07 23:59:59');

INSERT INTO notification (
    recipient_user_id, type, title, content, target_type, target_id, route_path, is_read
) VALUES
(3, 'ORDER_NEED_RECHARGE', 'Insufficient balance', 'Your balance is not enough for order payment. Please recharge first.', 'ORDER', 1, '/recharge', b'0'),
(7, 'COUPON_EXPIRING_SOON', 'Coupon expiring soon', 'One of your coupons will expire within 24 hours.', 'COUPON', 4, '/recharge', b'0'),
(7, 'COUPON_EXPIRED', 'Coupon expired', 'One of your recharge coupons has expired.', 'COUPON', 5, '/recharge', b'0');

INSERT INTO system_config (config_key, config_value, description) VALUES
('site_name', 'Tea Room Platform', 'Platform name'),
('customer_service_phone', '400-000-0000', 'Customer service phone'),
('membership_recharge_enabled', 'true', 'Whether member recharge is enabled'),
('membership_recharge_bonus_rules', '[{"minAmount":100,"bonusAmount":10},{"minAmount":300,"bonusAmount":40},{"minAmount":500,"bonusAmount":80}]', 'Recharge bonus rule JSON');
