-- Seed demo business data for local development / presentation.
-- MySQL + Flyway.

-- BCrypt hash for "123456"
SET @DEMO_BCRYPT := '$2a$10$3F0M4TngaCFogHFb5hPn8O5cXUvvf8B56Hv/FGs8xzEPNo5xKV7Zm';

-- Ensure demo accounts always have the expected password (useful if the DB already had these users).
UPDATE sys_user SET password = @DEMO_BCRYPT, enabled = b'1', role = 'ADMIN' WHERE username = 'admin';
UPDATE sys_user SET password = @DEMO_BCRYPT, enabled = b'1', role = 'STAFF' WHERE username = 'staff1';
UPDATE sys_user SET password = @DEMO_BCRYPT, enabled = b'1', role = 'USER'  WHERE username = 'user1';

SET @ADMIN_ID := (SELECT id FROM sys_user WHERE username = 'admin' LIMIT 1);
SET @STAFF_ID := (SELECT id FROM sys_user WHERE username = 'staff1' LIMIT 1);
SET @USER_ID  := (SELECT id FROM sys_user WHERE username = 'user1' LIMIT 1);

-- Tea room types
INSERT INTO tea_room_type (name, description, base_price)
SELECT '雅间', '适合 2-4 人，小型私密空间', 88.00
WHERE NOT EXISTS (SELECT 1 FROM tea_room_type WHERE name = '雅间');

INSERT INTO tea_room_type (name, description, base_price)
SELECT '商务包间', '适合商务接待，支持投屏与茶艺展示', 168.00
WHERE NOT EXISTS (SELECT 1 FROM tea_room_type WHERE name = '商务包间');

INSERT INTO tea_room_type (name, description, base_price)
SELECT '家庭套间', '适合家庭聚会，空间更宽敞', 128.00
WHERE NOT EXISTS (SELECT 1 FROM tea_room_type WHERE name = '家庭套间');

-- Tea rooms (assign to staff1)
INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, description)
SELECT (SELECT id FROM tea_room_type WHERE name = '雅间' LIMIT 1),
       @STAFF_ID,
       '茶室·云栖 01',
       4,
       b'1',
       '上海·静安',
       '窗边座位，适合小聚与阅读。'
WHERE NOT EXISTS (SELECT 1 FROM tea_room WHERE name = '茶室·云栖 01');

INSERT INTO tea_room (type_id, staff_user_id, name, capacity, enabled, location, description)
SELECT (SELECT id FROM tea_room_type WHERE name = '商务包间' LIMIT 1),
       @STAFF_ID,
       '茶室·松风 02',
       8,
       b'1',
       '上海·徐汇',
       '投屏设备齐全，适合商务会谈。'
WHERE NOT EXISTS (SELECT 1 FROM tea_room WHERE name = '茶室·松风 02');

-- Teas
INSERT INTO tea (name, category, price, stock, description, enabled)
SELECT '西湖龙井', '绿茶', 58.00, 120, '清香鲜爽，回甘明显。', b'1'
WHERE NOT EXISTS (SELECT 1 FROM tea WHERE name = '西湖龙井');

INSERT INTO tea (name, category, price, stock, description, enabled)
SELECT '安溪铁观音', '乌龙茶', 66.00, 80, '兰花香，韵味足。', b'1'
WHERE NOT EXISTS (SELECT 1 FROM tea WHERE name = '安溪铁观音');

INSERT INTO tea (name, category, price, stock, description, enabled)
SELECT '云南普洱熟茶', '黑茶', 78.00, 60, '醇厚顺滑，暖胃耐泡。', b'1'
WHERE NOT EXISTS (SELECT 1 FROM tea WHERE name = '云南普洱熟茶');

INSERT INTO tea (name, category, price, stock, description, enabled)
SELECT '茉莉花茶', '花茶', 39.00, 150, '花香浓郁，入口清甜。', b'1'
WHERE NOT EXISTS (SELECT 1 FROM tea WHERE name = '茉莉花茶');

-- Activities
INSERT INTO activity (title, content, start_time, end_time, capacity, status)
SELECT '春日品茗小课堂',
       '茶艺员带你认识绿茶冲泡要点，现场品鉴。',
       DATE_ADD(CURDATE(), INTERVAL 2 DAY) + INTERVAL 19 HOUR,
       DATE_ADD(CURDATE(), INTERVAL 2 DAY) + INTERVAL 21 HOUR,
       30,
       'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '春日品茗小课堂');

INSERT INTO activity (title, content, start_time, end_time, capacity, status)
SELECT '周末茶会·乌龙专场',
       '铁观音与岩茶风味对比与互动分享。',
       DATE_ADD(CURDATE(), INTERVAL 5 DAY) + INTERVAL 15 HOUR,
       DATE_ADD(CURDATE(), INTERVAL 5 DAY) + INTERVAL 17 HOUR,
       20,
       'PUBLISHED'
WHERE NOT EXISTS (SELECT 1 FROM activity WHERE title = '周末茶会·乌龙专场');

-- Activity registrations (user1 registers)
INSERT INTO activity_registration (activity_id, user_id, cancelled)
SELECT (SELECT id FROM activity WHERE title = '春日品茗小课堂' LIMIT 1), @USER_ID, b'0'
WHERE NOT EXISTS (
  SELECT 1 FROM activity_registration
  WHERE activity_id = (SELECT id FROM activity WHERE title = '春日品茗小课堂' LIMIT 1)
    AND user_id = @USER_ID
);

-- Rooms for references
SET @ROOM1_ID := (SELECT id FROM tea_room WHERE name = '茶室·云栖 01' LIMIT 1);
SET @ROOM2_ID := (SELECT id FROM tea_room WHERE name = '茶室·松风 02' LIMIT 1);

-- Reservations (various statuses)
INSERT INTO reservation (user_id, tea_room_id, reserved_start_time, reserved_end_time, status, remark)
SELECT @USER_ID, @ROOM1_ID,
       DATE_ADD(NOW(), INTERVAL 1 DAY),
       DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 2 HOUR,
       'PENDING',
       '希望靠窗座位'
WHERE NOT EXISTS (
  SELECT 1 FROM reservation
  WHERE user_id = @USER_ID AND tea_room_id = @ROOM1_ID AND status = 'PENDING'
);

INSERT INTO reservation (user_id, tea_room_id, reserved_start_time, reserved_end_time, status, remark)
SELECT @USER_ID, @ROOM2_ID,
       DATE_ADD(NOW(), INTERVAL 3 DAY),
       DATE_ADD(NOW(), INTERVAL 3 DAY) + INTERVAL 2 HOUR,
       'CONFIRMED',
       '商务会谈'
WHERE NOT EXISTS (
  SELECT 1 FROM reservation
  WHERE user_id = @USER_ID AND tea_room_id = @ROOM2_ID AND status = 'CONFIRMED'
);

INSERT INTO reservation (user_id, tea_room_id, reserved_start_time, reserved_end_time, status, remark)
SELECT @USER_ID, @ROOM1_ID,
       DATE_ADD(NOW(), INTERVAL -5 DAY),
       DATE_ADD(NOW(), INTERVAL -5 DAY) + INTERVAL 2 HOUR,
       'COMPLETED',
       '已完成体验'
WHERE NOT EXISTS (
  SELECT 1 FROM reservation
  WHERE user_id = @USER_ID AND tea_room_id = @ROOM1_ID AND status = 'COMPLETED'
);

-- Reviews (user1)
INSERT INTO review (user_id, tea_room_id, rating, content)
SELECT @USER_ID, @ROOM1_ID, 5, '环境安静，茶香很好，服务到位。'
WHERE NOT EXISTS (
  SELECT 1 FROM review
  WHERE user_id = @USER_ID AND tea_room_id = @ROOM1_ID AND content = '环境安静，茶香很好，服务到位。'
);

INSERT INTO review (user_id, tea_room_id, rating, content)
SELECT @USER_ID, @ROOM2_ID, 4, '设备齐全，整体不错，建议增加茶点选择。'
WHERE NOT EXISTS (
  SELECT 1 FROM review
  WHERE user_id = @USER_ID AND tea_room_id = @ROOM2_ID AND content = '设备齐全，整体不错，建议增加茶点选择。'
);

-- Equipment reports (created by staff1)
INSERT INTO equipment_report (tea_room_id, reported_by, title, content, status)
SELECT @ROOM2_ID, @STAFF_ID, '投影仪连接异常', '投影仪偶发无信号，需检修接口或更换线材。', 'PENDING'
WHERE NOT EXISTS (
  SELECT 1 FROM equipment_report
  WHERE tea_room_id = @ROOM2_ID AND title = '投影仪连接异常'
);

INSERT INTO equipment_report (tea_room_id, reported_by, title, content, status)
SELECT @ROOM1_ID, @STAFF_ID, '空调温控异常', '制冷效果不稳定，建议检查滤网与温控面板。', 'PROCESSING'
WHERE NOT EXISTS (
  SELECT 1 FROM equipment_report
  WHERE tea_room_id = @ROOM1_ID AND title = '空调温控异常'
);

-- Orders + items
SET @TEA1_ID := (SELECT id FROM tea WHERE name = '西湖龙井' LIMIT 1);
SET @TEA2_ID := (SELECT id FROM tea WHERE name = '安溪铁观音' LIMIT 1);
SET @TEA3_ID := (SELECT id FROM tea WHERE name = '云南普洱熟茶' LIMIT 1);
SET @TEA4_ID := (SELECT id FROM tea WHERE name = '茉莉花茶' LIMIT 1);

INSERT INTO product_order (order_no, user_id, amount, status)
SELECT 'ORD-DEMO-0001', @USER_ID, 124.00, 'PENDING_PAYMENT'
WHERE NOT EXISTS (SELECT 1 FROM product_order WHERE order_no = 'ORD-DEMO-0001');

INSERT INTO order_item (order_id, tea_id, quantity, unit_price)
SELECT (SELECT id FROM product_order WHERE order_no = 'ORD-DEMO-0001' LIMIT 1), @TEA1_ID, 1, 58.00
WHERE NOT EXISTS (
  SELECT 1 FROM order_item
  WHERE order_id = (SELECT id FROM product_order WHERE order_no = 'ORD-DEMO-0001' LIMIT 1)
    AND tea_id = @TEA1_ID
);

INSERT INTO order_item (order_id, tea_id, quantity, unit_price)
SELECT (SELECT id FROM product_order WHERE order_no = 'ORD-DEMO-0001' LIMIT 1), @TEA2_ID, 1, 66.00
WHERE NOT EXISTS (
  SELECT 1 FROM order_item
  WHERE order_id = (SELECT id FROM product_order WHERE order_no = 'ORD-DEMO-0001' LIMIT 1)
    AND tea_id = @TEA2_ID
);

INSERT INTO product_order (order_no, user_id, amount, status)
SELECT 'ORD-DEMO-0002', @USER_ID, 156.00, 'PAID'
WHERE NOT EXISTS (SELECT 1 FROM product_order WHERE order_no = 'ORD-DEMO-0002');

INSERT INTO order_item (order_id, tea_id, quantity, unit_price)
SELECT (SELECT id FROM product_order WHERE order_no = 'ORD-DEMO-0002' LIMIT 1), @TEA3_ID, 2, 78.00
WHERE NOT EXISTS (
  SELECT 1 FROM order_item
  WHERE order_id = (SELECT id FROM product_order WHERE order_no = 'ORD-DEMO-0002' LIMIT 1)
    AND tea_id = @TEA3_ID
);

INSERT INTO product_order (order_no, user_id, amount, status)
SELECT 'ORD-DEMO-0003', @USER_ID, 39.00, 'COMPLETED'
WHERE NOT EXISTS (SELECT 1 FROM product_order WHERE order_no = 'ORD-DEMO-0003');

INSERT INTO order_item (order_id, tea_id, quantity, unit_price)
SELECT (SELECT id FROM product_order WHERE order_no = 'ORD-DEMO-0003' LIMIT 1), @TEA4_ID, 1, 39.00
WHERE NOT EXISTS (
  SELECT 1 FROM order_item
  WHERE order_id = (SELECT id FROM product_order WHERE order_no = 'ORD-DEMO-0003' LIMIT 1)
    AND tea_id = @TEA4_ID
);

-- Favorites (user1)
INSERT INTO favorite (user_id, target_id, target_type)
SELECT @USER_ID, @TEA1_ID, 'TEA'
WHERE NOT EXISTS (
  SELECT 1 FROM favorite WHERE user_id = @USER_ID AND target_id = @TEA1_ID AND target_type = 'TEA'
);

INSERT INTO favorite (user_id, target_id, target_type)
SELECT @USER_ID, @ROOM1_ID, 'TEA_ROOM'
WHERE NOT EXISTS (
  SELECT 1 FROM favorite WHERE user_id = @USER_ID AND target_id = @ROOM1_ID AND target_type = 'TEA_ROOM'
);

-- Consultation session + messages (user1 <-> admin)
INSERT INTO consultation_session (user_id, admin_user_id, status)
SELECT @USER_ID, @ADMIN_ID, 'OPEN'
WHERE NOT EXISTS (
  SELECT 1 FROM consultation_session WHERE user_id = @USER_ID AND admin_user_id = @ADMIN_ID AND status = 'OPEN'
);

SET @SESSION_ID := (
  SELECT id FROM consultation_session
  WHERE user_id = @USER_ID AND admin_user_id = @ADMIN_ID
  ORDER BY id DESC LIMIT 1
);

INSERT INTO consultation_message (session_id, sender_id, content)
SELECT @SESSION_ID, @USER_ID, '你好，我想咨询茶室预约的收费标准。'
WHERE NOT EXISTS (
  SELECT 1 FROM consultation_message
  WHERE session_id = @SESSION_ID AND sender_id = @USER_ID AND content = '你好，我想咨询茶室预约的收费标准。'
);

INSERT INTO consultation_message (session_id, sender_id, content)
SELECT @SESSION_ID, @ADMIN_ID, '您好，收费标准与茶室类型相关，您可以在茶室列表查看基础价格。'
WHERE NOT EXISTS (
  SELECT 1 FROM consultation_message
  WHERE session_id = @SESSION_ID AND sender_id = @ADMIN_ID AND content = '您好，收费标准与茶室类型相关，您可以在茶室列表查看基础价格。'
);
